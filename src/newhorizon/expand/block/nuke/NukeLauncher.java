package newhorizon.expand.block.nuke;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.ai.types.CommandAI;
import mindustry.entities.Units;
import mindustry.entities.units.AIController;
import mindustry.entities.units.UnitController;
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.type.UnitType;
import mindustry.ui.Styles;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.PayloadBlock;
import mindustry.world.blocks.payloads.UnitPayload;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.Env;
import newhorizon.content.nuke.NukeFx;
import newhorizon.content.nuke.NukeUnits;

import static mindustry.Vars.tilesize;

/**
 * Takes an inactive nuke in as a payload (straight from a Nuke Builder, a payload conveyor, or a unit flown onto it) and prepares it:
 * the launch heading is set with a slider (the nuke is turned to it), the target is picked on the map, and arming turns the nuke into
 * its active form. An armed nuke that has turned to the heading can be launched, and flies at the target.
 *
 * <p>Arming can be undone until the launch. The launcher takes {@link #armTime} ticks to arm or disarm.
 */
public class NukeLauncher extends PayloadBlock{
	public float armTime = 150f;
	public float launchCooldown = 60f * 4f;
	/** Degrees per tick the nuke is turned at inside the launcher. */
	public float turnSpeed = 1.6f;
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
		saveConfig = false;
		group = BlockGroup.units;
		envEnabled = Env.any;

		config(Float.class, (NukeLauncherBuild b, Float angle) -> b.angle = Mathf.mod(angle, 360f));
		config(Vec2.class, (NukeLauncherBuild b, Vec2 target) -> b.setTarget(target));
		config(Boolean.class, (NukeLauncherBuild b, Boolean armed) -> b.wantArmed = armed);
		config(Integer.class, (NukeLauncherBuild b, Integer command) -> {
			if(command == 0) b.launch();
			else if(command == 1) b.hasTarget = false;
		});
	}

	public class NukeLauncherBuild extends PayloadBlockBuild<UnitPayload>{
		/** Launch heading in degrees. */
		public float angle = 90f;
		public Vec2 target = new Vec2();
		public boolean hasTarget;
		/** What the player wants, and how far the nuke is towards it: 0 is inactive, 1 is active. */
		public boolean wantArmed;
		public float armProgress;
		public float cooldown;

		public void setTarget(Vec2 pos){
			target.set(pos);
			hasTarget = true;
			//Turning towards the target is almost always what is wanted; the slider still has the last word.
			angle = Mathf.mod(angleTo(pos), 360f);
		}

		public Unit unit(){
			return payload == null ? null : payload.unit;
		}

		public boolean loaded(){
			return unit() != null;
		}

		public boolean isArmed(){
			return loaded() && NukeUnits.isActive(unit().type);
		}

		@Override
		public boolean acceptUnitPayload(Unit unit){
			return payload == null && NukeUnits.isInactive(unit.type) && super.acceptUnitPayload(unit);
		}

		@Override
		public boolean acceptPayload(Building source, Payload p){
			return p instanceof UnitPayload && payload == null && NukeUnits.isInactive(((UnitPayload)p).unit.type) && super.acceptPayload(source, p);
		}

		@Override
		public void updateTile(){
			cooldown = Math.max(0f, cooldown - Time.delta);
			if(payload == null){
				armProgress = 0f;
				return;
			}

			boolean arrived = moveInPayload(false);
			payload.set(x + payVector.x, y + payVector.y, payRotation);
			if(!arrived || efficiency <= 0.5f) return;

			//Turn the nuke to the heading.
			payRotation = Angles.moveToward(payRotation, angle, turnSpeed * edelta());

			//Arm or disarm: the nuke changes form at either end.
			float before = armProgress;
			armProgress = Mathf.approach(armProgress, wantArmed ? 1f : 0f, edelta() / armTime);
			if(armProgress >= 1f && !isArmed()) convert();
			else if(armProgress <= 0f && isArmed()) convert();
			if(armProgress > before && Mathf.chanceDelta(0.2)) NukeFx.armed.at(x, y);
		}

		/** Swaps the loaded nuke for its other form. */
		protected void convert(){
			Unit old = unit();
			UnitType other = old == null ? null : NukeUnits.counterpart(old.type);
			if(other == null) return;

			Unit now = other.create(team);
			now.rotation = old.rotation;
			payload.unit = now;
			payload.set(x + payVector.x, y + payVector.y, payRotation);
		}

		public boolean canLaunch(){
			return loaded() && isArmed() && arrivedAtCenter() && efficiency > 0.5f && cooldown <= 0f
				&& Math.abs(Angles.angleDist(payRotation, angle)) <= aimTolerance && Units.canCreate(team, unit().type);
		}

		protected boolean arrivedAtCenter(){
			return payVector.isZero(0.5f);
		}

		/** Sends the loaded nuke off along the heading. Only the server makes the unit; every side empties the launcher. */
		public boolean launch(){
			if(!canLaunch()) return false;

			Unit nuke = payload.unit;
			payload = null;
			payVector.setZero();
			cooldown = launchCooldown;
			armProgress = 0f;
			wantArmed = false;

			NukeFx.launch.at(x, y, angle);
			Sounds.shootMissileLarge.at(x, y, 1f, 0.8f);
			if(Vars.net.client()) return true;

			nuke.set(x + Angles.trnsx(angle, size * tilesize / 2f), y + Angles.trnsy(angle, size * tilesize / 2f));
			nuke.rotation = angle;
			nuke.vel.trns(angle, nuke.type.speed * 0.6f);
			nuke.add();

			//A team the player commands gets the unit as a commandable one; everything else gets the nuke's own flight AI.
			Vec2 aim = hasTarget ? new Vec2(target) : null;
			if(!team.isAI() && NukeUnits.strikeCommand != null && nuke.type.commands.contains(NukeUnits.strikeCommand)){
				CommandAI command = new CommandAI();
				nuke.controller(command);
				command.command(NukeUnits.strikeCommand);
				if(aim != null) command.commandPosition(aim);
			}else{
				UnitController controller = nuke.controller();
				if(!(controller instanceof AIController)){
					controller = nuke.type.createController(nuke);
					nuke.controller(controller);
				}
				if(aim != null && controller instanceof AIController) ((AIController)controller).commandPosition(aim);
			}
			return true;
		}

		@Override
		public void buildConfiguration(Table table){
			table.table(Styles.black6, t -> {
				t.margin(8f);
				t.defaults().left().padBottom(4f);

				t.label(() -> statusText()).width(260f).wrap().row();

				t.label(() -> Core.bundle.format("nh.nuke.heading", (int)angle)).row();
				Slider slider = new Slider(0f, 359f, 1f, false);
				slider.setValue(angle);
				slider.moved(v -> configure(v));
				slider.update(() -> {
					if(!slider.isDragging()) slider.setValue(angle);
				});
				t.add(slider).width(260f).row();

				t.table(row -> {
					row.defaults().size(124f, 40f).pad(2f);
					row.button(Core.bundle.get("nh.nuke.target"), Styles.defaultt, () -> NukeTargeting.begin(this));
					row.button(Core.bundle.get("nh.nuke.cleartarget"), Styles.defaultt, () -> configure(1)).disabled(b -> !hasTarget);
				}).row();

				t.table(row -> {
					row.defaults().size(124f, 40f).pad(2f);
					row.button(Core.bundle.get("nh.nuke.arm"), Styles.togglet, () -> configure(!wantArmed)).update(b -> b.setChecked(wantArmed)).disabled(b -> !loaded());
					row.button(Core.bundle.get("nh.nuke.launch"), Styles.defaultt, () -> {
						configure(0);
						Vars.control.input.config.hideConfig();
					}).disabled(b -> !canLaunch());
				}).row();
			});
		}

		public String statusText(){
			if(!loaded()) return Core.bundle.get("nh.nuke.empty");
			String name = unit().type.localizedName;
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
			Draw.rect(topRegion, x, y);

			if(loaded()){
				Draw.z(Layer.blockOver + 0.2f);
				float f = isArmed() ? 1f : armProgress;
				Draw.color(Color.scarlet, 0.35f + 0.65f * Mathf.absin(Time.time, 18f / (0.4f + f), 1f) * f);
				Draw.rect(topRegion, x, y);
				Draw.color();
			}
		}

		@Override
		public void drawConfigure(){
			float r = size * tilesize / 2f + 6f;
			Drawf.dashCircle(x, y, r, Color.scarlet);
			Draw.color(Color.scarlet, 0.9f);
			Lines.stroke(2f);
			Lines.lineAngle(x + Angles.trnsx(angle, r), y + Angles.trnsy(angle, r), angle, 30f);
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
		public Object config(){
			return null;
		}

		@Override
		public void write(Writes write){
			super.write(write);
			write.f(angle);
			write.bool(hasTarget);
			write.f(target.x);
			write.f(target.y);
			write.bool(wantArmed);
			write.f(armProgress);
		}

		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			angle = read.f();
			hasTarget = read.bool();
			target.set(read.f(), read.f());
			wantArmed = read.bool();
			armProgress = read.f();
		}
	}
}
