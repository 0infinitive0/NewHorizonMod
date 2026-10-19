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
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
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
		config(Boolean.class, (NukeLauncherBuild b, Boolean armed) -> b.armSwitch = armed);
		config(Integer.class, (NukeLauncherBuild b, Integer command) -> {
			if(command == 0) b.launch();
			else if(command == 1) b.hasTarget = false;
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
		/** The target picked on the map. */
		public Vec2 target = new Vec2();
		public boolean hasTarget;
		/** What the menu asked for: whether to arm. A pilot's fire button replaces it while they are in control. */
		public boolean armSwitch;
		/** Whether the nuke is being armed now, and how far arming has got: 0 is inactive, 1 is active. */
		public boolean wantArmed;
		public float armProgress;
		public float cooldown;

		public void setTarget(Vec2 pos){
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
			return payload == null && NukeUnits.isInactive(unit.type) && super.acceptUnitPayload(unit);
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

			if(payload == null){
				armProgress = 0f;
				return;
			}

			boolean arrived = moveInPayload(false);
			payload.set(x + payVector.x, y + payVector.y, payRotation);
			if(!arrived || efficiency <= 0.5f) return;

			//Turn like a turret: to the pilot's cursor while someone controls it, otherwise to the map target, otherwise stay.
			float wanted = angle;
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

			//A pilot launches by holding fire; otherwise the Launch button does it.
			if(piloted() && wantArmed && canLaunch()) launch();
		}

		/** Swaps the loaded nuke for its other form. */
		protected void convert(){
			Unit old = loadedUnit();
			UnitType other = old == null ? null : NukeUnits.counterpart(old.type);
			if(other == null) return;

			Unit now = other.create(team);
			now.rotation = old.rotation;
			payload.unit = now;
			payload.set(x + payVector.x, y + payVector.y, payRotation);
		}

		public boolean canLaunch(){
			return loaded() && isArmed() && payVector.isZero(0.5f) && efficiency > 0.5f && cooldown <= 0f
				&& Math.abs(Angles.angleDist(payRotation, angle)) <= aimTolerance && Units.canCreate(team, loadedUnit().type);
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

			NukeFx.launch.at(x, y, angle);
			Sounds.shootMissileLarge.at(x, y, 1f, 0.8f);
			if(Vars.net.client()) return true;

			nuke.set(x + Angles.trnsx(angle, size * tilesize / 2f), y + Angles.trnsy(angle, size * tilesize / 2f));
			nuke.rotation = angle;
			nuke.vel.trns(angle, nuke.type.speed * 0.6f);
			nuke.add();

			//A team the player commands gets the unit as a commandable one; everything else gets the nuke's own flight AI.
			AIController controller;
			if(!team.isAI() && nuke.type.commands.contains(NukeUnits.strikeCommand)){
				CommandAI command = new CommandAI();
				nuke.controller(command);
				command.command(NukeUnits.strikeCommand);
				controller = command;
			}else{
				UnitController current = nuke.controller();
				if(!(current instanceof AIController)){
					current = nuke.type.createController(nuke);
					nuke.controller(current);
				}
				controller = (AIController)current;
			}
			if(aim != null) controller.commandPosition(aim);
			return true;
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
			hasTarget = read.bool();
			target.set(read.f(), read.f());
			armSwitch = read.bool();
			armProgress = read.f();
		}
	}
}
