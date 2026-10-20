package newhorizon.expand.block.nuke;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.world.Block;
import mindustry.world.meta.Env;
import newhorizon.content.nuke.NukeStatus;
import newhorizon.content.nuke.NukeUnits;
import newhorizon.expand.units.nuke.NukeAI;
import newhorizon.expand.units.nuke.Warhead;

/**
 * The area blocks that work against nukes. They all need power and cover a circle:
 * <ul>
 * <li><b>radar</b> takes the high altitude off enemy nukes in range, so that they can be targeted and shot at again;</li>
 * <li><b>cloak</b> hides everything in range from enemy launchers: they cannot pick a target there, and nukes already heading for one lose it;</li>
 * <li><b>beacon</b> pulls every enemy nuke in range into flying at it, whatever it was aimed at;</li>
 * <li><b>jammer</b> makes enemy nukes in range lose their target and wander at random, so that they fly past.</li>
 * </ul>
 * The effects on nukes are looked up from {@link newhorizon.expand.units.nuke.NukeDefense}.
 */
public class DefenseField extends Block{
	public enum Kind{
		radar, cloak, beacon, jammer
	}

	public final Kind kind;
	/** Radius of the covered circle, in world units. */
	public float range = 300f;
	public Color color = Color.white;

	public DefenseField(String name, Kind kind){
		super(name);
		this.kind = kind;
		update = true;
		solid = true;
		hasPower = true;
		envEnabled = Env.any;
		buildType = DefenseFieldBuild::new;
	}

	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		super.drawPlace(x, y, rotation, valid);
		Drawf.dashCircle(x * Vars.tilesize + offset, y * Vars.tilesize + offset, range, color);
	}

	public class DefenseFieldBuild extends Building{
		public Kind kind(){
			return kind;
		}

		public float range(){
			return range;
		}

		/** Whether it is working: powered and switched on. */
		public boolean active(){
			return enabled && efficiency > 0.5f;
		}

		@Override
		public void updateTile(){
			if(kind != Kind.radar || !active() || !timer(0, 10f)) return;
			//Takes the high altitude off every enemy nuke in range.
			Units.nearby(x - range, y - range, range * 2f, range * 2f, u -> {
				if(u.team != team && u.team != mindustry.game.Team.derelict && NukeUnits.isNuke(u.type) && u.within(this, range + u.hitSize / 2f)){
					if(u.hasEffect(NukeStatus.highAltitude)) u.unapply(NukeStatus.highAltitude);
				}
			});
		}

		@Override
		public void drawSelect(){
			Drawf.dashCircle(x, y, range, color);
		}

		@Override
		public void draw(){
			super.draw();
			if(!active()) return;

			switch(kind){
				case radar -> {
					Draw.z(Layer.blockOver);
					Draw.rect(topRegion(), x, y, Time.time * 2.5f);
					//Every enemy nuke in range is marked, for the team that owns the radar.
					if(Vars.player != null && Vars.player.team() == team){
						Draw.z(Layer.overlayUI);
						Draw.color(Color.scarlet, 0.9f);
						Lines.stroke(1.6f);
						Vec2 goal = new Vec2();
						Units.nearby(x - range, y - range, range * 2f, range * 2f, u -> {
							if(u.team == team || !NukeUnits.isActive(u.type) || !u.within(this, range + u.hitSize / 2f)) return;
							Lines.circle(u.x, u.y, u.hitSize + 8f + Mathf.absin(Time.time, 8f, 3f));
							Lines.lineAngle(u.x, u.y, u.rotation, 28f);
							//Where it is going, and what the blast would cover.
							if(NukeAI.targetOf(u, goal)){
								Warhead warhead = NukeUnits.warheadOf(u.type);
								Lines.dashLine(u.x, u.y, goal.x, goal.y, Math.max(2, (int)(u.dst(goal) / 14f)));
								Lines.circle(goal.x, goal.y, warhead == null ? 60f : warhead.radius);
							}
						});
						Draw.reset();
					}
				}
				case cloak -> {
					Draw.z(Layer.blockUnder + 0.5f);
					Draw.color(color, 0.05f + 0.03f * Mathf.absin(Time.time, 40f, 1f));
					Fill.circle(x, y, range);
					Draw.color(color, 0.35f);
					Lines.stroke(1.5f);
					Lines.circle(x, y, range);
					Draw.reset();
				}
				case beacon -> {
					Draw.z(Layer.blockOver);
					float f = (Time.time % 90f) / 90f;
					Draw.color(color, 0.8f * (1f - f));
					Lines.stroke(2f * (1f - f) + 0.4f);
					Lines.circle(x, y, 10f + size * 6f * f);
					Draw.reset();
				}
				case jammer -> {
					Draw.z(Layer.blockOver);
					Draw.color(color, 0.7f);
					Lines.stroke(1.2f);
					for(int i = 0; i < 4; i++){
						float a = Mathf.randomSeed((long)(id * 7 + i + (long)(Time.time / 4f)), 360f);
						Lines.lineAngle(x + Mathf.cosDeg(a) * size * 5f, y + Mathf.sinDeg(a) * size * 5f, a + 90f, 5f + Mathf.random(4f));
					}
					Draw.reset();
				}
			}
		}

		private arc.graphics.g2d.TextureRegion topRegion(){
			return arc.Core.atlas.find(name + "-top");
		}
	}
}
