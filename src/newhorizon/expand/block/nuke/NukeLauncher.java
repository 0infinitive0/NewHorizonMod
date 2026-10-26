package newhorizon.expand.block.nuke;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.ai.types.CommandAI;
import mindustry.content.UnitTypes;
import mindustry.entities.Units;
import mindustry.entities.units.AIController;
import mindustry.entities.units.UnitController;
import mindustry.gen.BlockUnitc;
import mindustry.core.World;
import mindustry.gen.Building;
import mindustry.gen.Posc;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.logic.LAccess;
import mindustry.type.UnitType;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.blocks.ControlBlock;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.PayloadBlock;
import mindustry.world.blocks.payloads.UnitPayload;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.Env;
import newhorizon.content.nuke.NukeFx;
import newhorizon.content.nuke.NukeUnits;
import newhorizon.expand.units.nuke.NukeBrain;
import newhorizon.expand.units.nuke.NukeDefense;

import static mindustry.Vars.tilesize;

/**
 * A turret for nukes. It takes an inactive nuke in as a payload (straight from a Nuke Builder, a payload conveyor, or a unit flown
 * onto it) and turns it like a turret turns its gun.
 *
 * <p>The target is picked on the map from the launcher's menu, and the launcher turns to face it by itself. Arming (a button in the
 * menu) turns the nuke into its active form, and an armed nuke that faces the target can be launched with the Launch button; it then
 * flies at the target.
 *
 * <p>A player can also control the launcher, like any turret (hold the control key and click it). The pilot's cursor overrides the map
 * target for as long as they are in control: the launcher follows the cursor, holding the fire button arms the nuke and launches it
 * straight along that heading as soon as it is armed and facing the right way, and letting go before that disarms it again. When the
 * pilot leaves, the launcher returns to the map target and to the arming the menu asked for.
 *
 * <p>A logic processor can run it too, with the {@code control shoot} instruction ({@code control shoot launcher x y mode}, x and y in
 * tiles) or {@code control shootp launcher unit mode}: it sets the map target and the mode says what to do with the nuke: 0 disarms it,
 * 1 arms it and launches it at the target as soon as it is armed and facing it, 2 only arms it and holds it. The orders stay in force
 * (there is no need to repeat them), and the menu or a pilot can still override them. {@code sensor} reads the state back; see
 * {@link NukeLauncherBuild#sense(LAccess)}.
 */
public class NukeLauncher extends PayloadBlock{
	/** Ticks to arm (or to disarm from fully armed). */
	public float armTime = 150f;
	public float launchCooldown = 60f * 4f;
	/** Degrees per tick the nuke is turned at inside the launcher. */
	public float turnSpeed = 1.8f;
	/** The heading has to be this close before the nuke can be launched. */
	public float aimTolerance = 3f;

	public NukeLauncher(String name){
		super(name);
		size = 4;
		update = true;
		solid = true;
		hasPower = true;
		rotate = false;
		acceptsPayload = true;
		outputsPayload = false;
		configurable = true;
		group = BlockGroup.units;
		envEnabled = Env.any;

		config(Vec2.class, (NukeLauncherBuild b, Vec2 target) -> b.setTarget(target));
		config(Boolean.class, (NukeLauncherBuild b, Boolean armed) -> {
			b.armSwitch = armed;
			b.launchOrder = false;
		});
		config(Integer.class, (NukeLauncherBuild b, Integer command) -> {
			if(command == 0) b.launch();
			else if(command == 1){
				b.hasTarget = false;
				b.launchOrder = false;
			}
		});
	}

	@Override
	public void setBars(){
		super.setBars();
		addBar("nh-arming", (NukeLauncherBuild build) -> new Bar(
			() -> build.loaded() ? Core.bundle.get(build.isArmed() ? "nh.nuke.bar.armed" : "nh.nuke.bar.arming") : Core.bundle.get("nh.nuke.bar.empty"),
			() -> Pal.remove,
			() -> build.loaded() ? build.armProgress : 0f
		));
	}

	public class NukeLauncherBuild extends PayloadBlockBuild<UnitPayload> implements ControlBlock{
		/** The unit a player becomes to control the launcher, like a turret's. */
		public BlockUnitc unit;
		/** Heading in degrees. */
		public float angle = 90f;
		/** Where it is trying to point: the pilot's cursor, the map target, or just where it already points. */
		public float wanted = 90f;
		/** The target picked on the map. */
		public Vec2 target = new Vec2();
		public boolean hasTarget;
		/** What the menu asked for: whether to arm. A pilot's fire button replaces it while they are in control. */
		public boolean armSwitch;
		/** Set by a logic processor: launch at the target as soon as the nuke is armed and facing it. */
		public boolean launchOrder;
		/** Whether the nuke is being armed now, and how far arming has got: 0 is inactive, 1 is active. */
		public boolean wantArmed;
		public float armProgress;
		public float cooldown;
		/** What an AI team's launcher keeps between its checks (see NukeBrain.driveLauncher): the next check, the last count of nukes and when it changed. */
		public float aiNext, aiSince;
		public int aiCount;

		public void setTarget(Vec2 pos){
			//An enemy cloak hides what is under it: no target can be picked there.
			if(NukeDefense.cloaked(team, pos.x, pos.y)) return;
			target.set(pos);
			hasTarget = true;
		}

		@Override
		public void created(){
			super.created();
			unit = (BlockUnitc)UnitTypes.block.create(team);
			unit.tile(this);
		}

		@Override
		public Unit unit(){
			//Keeps the control unit where the block is and on its team, as a turret does.
			if(unit == null){
				unit = (BlockUnitc)UnitTypes.block.create(team);
			}
			unit.tile(this);
			unit.team(team);
			return (Unit)unit;
		}

		public Unit loadedUnit(){
			return payload == null ? null : payload.unit;
		}

		public boolean loaded(){
			return loadedUnit() != null;
		}

		public boolean isArmed(){
			return loaded() && NukeUnits.isActive(loadedUnit().type);
		}

		@Override
		public boolean acceptUnitPayload(Unit unit){
			//A nuke can also fly in by itself (the Enter Payload command): the base block never takes units in, so this is checked here.
			return payload == null && NukeUnits.isInactive(unit.type) && unit.type.allowedInPayloads && !unit.spawnedByCore;
		}

		@Override
		public boolean acceptPayload(Building source, Payload p){
			return p instanceof UnitPayload && payload == null && NukeUnits.isInactive(((UnitPayload)p).unit.type) && super.acceptPayload(source, p);
		}

		/** The pilot overrides the map target while they are in control. */
		public boolean piloted(){
			return isControlled();
		}

		@Override
		public void updateTile(){
			unit();
			cooldown = Math.max(0f, cooldown - Time.delta);
			NukeBrain.driveLauncher(this);
			//A cloak put up over the target after it was picked takes it away.
			if(hasTarget && NukeDefense.cloaked(team, target.x, target.y)){
				hasTarget = false;
				launchOrder = false;
			}

			if(payload == null){
				armProgress = 0f;
				return;
			}

			boolean arrived = moveInPayload(false);
			payload.set(x + payVector.x, y + payVector.y, payRotation);
			if(!arrived || efficiency <= 0.5f) return;

			//Turn like a turret: to the pilot's cursor while someone controls it, otherwise to the map target, otherwise stay.
			wanted = angle;
			if(piloted()) wanted = angleTo(unit().aimX(), unit().aimY());
			else if(hasTarget) wanted = angleTo(target.x, target.y);
			angle = Angles.moveToward(angle, wanted, turnSpeed * edelta());
			payRotation = angle;

			//A pilot's fire button arms the nuke and letting go disarms it; without a pilot the menu's switch decides.
			wantArmed = piloted() ? unit().isShooting() : armSwitch;
			float before = armProgress;
			armProgress = Mathf.approach(armProgress, wantArmed ? 1f : 0f, edelta() / armTime);
			if(armProgress >= 1f && !isArmed()) convert();
			else if(armProgress <= 0f && isArmed()) convert();
			if(armProgress > before && Mathf.chanceDelta(0.2)) NukeFx.armed.at(x, y);

			//A pilot launches by holding fire; otherwise the Launch button does it, or a processor's launch order once the nuke is armed.
			if(wantArmed && canLaunch() && autoLaunch()) launch();
		}

		/** Whether the launcher is meant to fire by itself now: a pilot holding fire, or a processor's order with a target to fire at. */
		protected boolean autoLaunch(){
			return piloted() || launchOrder && hasTarget;
		}

		/** How many nukes are in the launcher, and how many it can hold. */
		public int count(){
			return loaded() ? 1 : 0;
		}

		public int capacityNow(){
			return 1;
		}

		/** Whether a salvo is under way. */
		public boolean salvoing(){
			return false;
		}

		/** A processor's aim: the target goes to the tile (or unit) it names. Nothing under an enemy cloak can be aimed at. */
		protected boolean logicAim(double x, double y){
			if(Double.isNaN(x) || Double.isNaN(y) || Double.isInfinite(x) || Double.isInfinite(y)) return false;
			float wx = Mathf.clamp((float)x, 0f, Vars.world.unitWidth()), wy = Mathf.clamp((float)y, 0f, Vars.world.unitHeight());
			if(NukeDefense.cloaked(team, wx, wy)){
				hasTarget = false;
				launchOrder = false;
				return false;
			}
			target.set(wx, wy);
			hasTarget = true;
			return true;
		}

		/** A processor's mode: 0 disarms, 1 arms and launches at the target, 2 arms and waits. */
		protected void logicMode(double mode, boolean aimed){
			if(Double.isNaN(mode) || mode < 0.5){
				armSwitch = false;
				launchOrder = false;
			}else if(aimed){
				armSwitch = true;
				launchOrder = mode < 1.5;
			}
		}

		@Override
		public void control(LAccess type, double p1, double p2, double p3, double p4){
			if(type == LAccess.shoot) logicMode(p3, logicAim(World.unconv((float)p1), World.unconv((float)p2)));
			else super.control(type, p1, p2, p3, p4);
		}

		@Override
		public void control(LAccess type, Object p1, double p2, double p3, double p4){
			if(type == LAccess.shootp){
				if(p1 instanceof Posc) logicMode(p2, logicAim(((Posc)p1).getX(), ((Posc)p1).getY()));
			}else super.control(type, p1, p2, p3, p4);
		}

		/**
		 * What a processor can read: {@code payloadCount} the nukes inside and {@code payloadCapacity} how many fit, {@code ammo} how many
		 * of them are armed, {@code progress} how far arming has got (1 once armed), {@code heat} the share of the cooldown after a launch
		 * that is left, {@code shooting} 1 when it is ready to launch or a salvo is under way, {@code rotation} the heading, and {@code shootX}
		 * and {@code shootY} the target's tile (null when there is none).
		 */
		@Override
		public double sense(LAccess sensor){
			switch(sensor){
				case payloadCount: return count();
				case payloadCapacity: case ammoCapacity: return capacityNow();
				case ammo: return isArmed() ? count() : 0;
				case progress: return loaded() ? armProgress : 0f;
				case heat: return Mathf.clamp(cooldown / launchCooldown);
				case shooting: return canLaunch() || salvoing() ? 1 : 0;
				case rotation: return angle;
				case shootX: return hasTarget ? World.conv(target.x) : Double.NaN;
				case shootY: return hasTarget ? World.conv(target.y) : Double.NaN;
				default: return super.sense(sensor);
			}
		}

		@Override
		public Object senseObject(LAccess sensor){
			if(sensor == LAccess.payloadType) return loaded() ? loadedUnit().type : null;
			return super.senseObject(sensor);
		}

		/** Swaps the loaded nuke for its other form. */
		protected void convert(){
			Unit old = loadedUnit();
			UnitType other = old == null ? null : NukeUnits.counterpart(old.type);
			if(other == null) return;

			Unit now = other.create(team);
			now.rotation = old.rotation;
			//Whatever the nuke was set up with (a wall type, passengers) goes with it.
			NukeUnits.transfer(old, now);
			payload.unit = now;
			payload.set(x + payVector.x, y + payVector.y, payRotation);
		}

		public boolean canLaunch(){
			return loaded() && isArmed() && payVector.isZero(0.5f) && efficiency > 0.5f && cooldown <= 0f
				&& aimed() && Units.canCreate(team, loadedUnit().type);
		}

		/** Whether the heading has got to where it is meant to point. */
		public boolean aimed(){
			return Math.abs(Angles.angleDist(angle, wanted)) <= aimTolerance;
		}

		/** Sends the loaded nuke off along the heading, at the target if there is one. Only the server makes the unit; every side empties the launcher. */
		public boolean launch(){
			if(!canLaunch()) return false;

			Unit nuke = payload.unit;
			//The pilot's heading overrides the map target: a nuke launched by hand just flies the way it was pointed.
			Vec2 aim = !piloted() && hasTarget ? new Vec2(target) : null;
			payload = null;
			payVector.setZero();
			cooldown = launchCooldown;
			armProgress = 0f;
			wantArmed = false;
			armSwitch = false;
			launchOrder = false;

			NukeFx.launch.at(x, y, angle);
			Sounds.shootMissileLarge.at(x, y, 1f, 0.8f);
			if(Vars.net.client()) return true;

			send(nuke, 0f, aim);
			return true;
		}

		/**
		 * Puts a nuke into the world at the muzzle, {@code lateral} units to the side of the centre line, flying along the heading or at the
		 * target. Server side: the other peers only see the unit arrive.
		 */
		protected void send(Unit nuke, float lateral, Vec2 aim){
			float reach = size * tilesize / 2f;
			nuke.set(x + Angles.trnsx(angle, reach) + Angles.trnsx(angle + 90f, lateral), y + Angles.trnsy(angle, reach) + Angles.trnsy(angle + 90f, lateral));
			nuke.rotation = angle;
			nuke.vel.trns(angle, nuke.type.speed * 0.6f);
			nuke.add();

			NukeUnits.fly(nuke, aim);
		}

		@Override
		public void buildConfiguration(Table table){
			table.table(Styles.black6, t -> {
				t.margin(8f);
				t.defaults().left().padBottom(4f);

				t.label(() -> statusText()).width(260f).wrap().row();

				t.table(row -> {
					row.defaults().size(124f, 40f).pad(2f);
					row.button(Core.bundle.get("nh.nuke.target"), Styles.defaultt, () -> NukeTargeting.begin(this));
					row.button(Core.bundle.get("nh.nuke.cleartarget"), Styles.defaultt, () -> configure(1)).disabled(b -> !hasTarget);
				}).row();

				t.table(row -> {
					row.defaults().size(124f, 40f).pad(2f);
					row.button(Core.bundle.get("nh.nuke.arm"), Styles.togglet, () -> configure(!armSwitch)).update(b -> b.setChecked(armSwitch)).disabled(b -> !loaded() || piloted());
					row.button(Core.bundle.get("nh.nuke.launch"), Styles.defaultt, () -> {
						configure(0);
						Vars.control.input.config.hideConfig();
					}).disabled(b -> !canLaunch() || piloted());
				}).row();
			});
		}

		public String statusText(){
			if(piloted()) return Core.bundle.get("nh.nuke.piloted");
			if(!loaded()) return Core.bundle.get("nh.nuke.empty");
			String name = loadedUnit().type.localizedName;
			if(efficiency <= 0.5f) return Core.bundle.format("nh.nuke.nopower", name);
			if(isArmed()) return Core.bundle.format(canLaunch() ? "nh.nuke.ready" : "nh.nuke.armed", name);
			if(wantArmed) return Core.bundle.format("nh.nuke.arming", name, (int)(armProgress * 100f));
			return Core.bundle.format("nh.nuke.loaded", name);
		}

		@Override
		public void draw(){
			Draw.rect(region, x, y);
			drawPayload();

			Draw.z(Layer.blockOver + 0.1f);
			Draw.rect(topRegion, x, y, payload == null ? angle - 90f : payRotation - 90f);

			if(loaded()){
				Draw.z(Layer.blockOver + 0.2f);
				float f = isArmed() ? 1f : armProgress;
				Draw.color(Color.scarlet, 0.35f + 0.65f * Mathf.absin(Time.time, 18f / (0.4f + f), 1f) * f);
				Draw.rect(topRegion, x, y, payRotation - 90f);
				Draw.color();
			}
		}

		@Override
		public void drawConfigure(){
			float r = size * tilesize / 2f + 6f;
			Draw.color(Color.scarlet, 0.9f);
			Lines.stroke(2f);
			Lines.lineAngle(x + Angles.trnsx(angle, r), y + Angles.trnsy(angle, r), angle, 40f);
			if(hasTarget){
				Lines.stroke(1.5f);
				Lines.dashLine(x, y, target.x, target.y, (int)(dst(target) / 14f));
				Lines.circle(target.x, target.y, 14f);
				Lines.line(target.x - 20f, target.y, target.x + 20f, target.y);
				Lines.line(target.x, target.y - 20f, target.x, target.y + 20f);
			}
			Draw.reset();
		}

		@Override
		public void write(Writes write){
			super.write(write);
			write.f(angle);
			write.bool(hasTarget);
			write.f(target.x);
			write.f(target.y);
			write.bool(armSwitch);
			write.f(armProgress);
		}

		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			angle = read.f();
			wanted = angle;
			hasTarget = read.bool();
			target.set(read.f(), read.f());
			armSwitch = read.bool();
			armProgress = read.f();
		}
	}
}
