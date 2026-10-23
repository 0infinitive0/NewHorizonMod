package newhorizon.expand.block.chronos;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.world.Block;
import mindustry.world.meta.Env;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import newhorizon.content.chronos.ChronosColor;

import static mindustry.Vars.tilesize;

/**
 * A dome in which time runs slow for enemy bullets: they cover only {@link #slow} of their usual distance each tick, and last
 * that much longer, so they take their time and still do not reach any further. Nothing is destroyed. Friendly bullets pass untouched.
 */
public class TimeDilationField extends Block{
	public float range = 150f;
	/** Speed of an enemy bullet inside the dome, as a share of its normal speed. */
	public float slow = 0.25f;

	public TimeDilationField(String name){
		super(name);
		update = true;
		solid = true;
		hasPower = true;
		envEnabled = Env.any;
	}

	@Override
	public void setStats(){
		super.setStats();
		stats.add(Stat.range, range / tilesize, StatUnit.blocks);
		stats.add(Stat.speedIncrease, "-" + (int)((1f - slow) * 100f) + "% bullet speed");
	}

	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		super.drawPlace(x, y, rotation, valid);
		Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, ChronosColor.chrono);
	}

	public class TimeDilationFieldBuild extends Building{
		public float warmup, spin;

		@Override
		public void updateTile(){
			warmup = Mathf.lerpDelta(warmup, efficiency > 0.01f ? 1f : 0f, 0.04f);
			spin += warmup * Time.delta;
			if(efficiency <= 0.01f) return;
			dilate();
		}

		/** Slows every enemy bullet inside the dome. Returns how many. */
		public int dilate(){
			int[] count = {0};
			Groups.bullet.intersect(x - range, y - range, range * 2f, range * 2f, b -> {
				if(!affects(b) || !b.within(this, range)) return;
				count[0]++;

				//Taking back part of this tick's step, and part of this tick's age, is what makes time run slow in here.
				float back = (1f - slow) * Time.delta;
				b.x -= b.vel.x * back;
				b.y -= b.vel.y * back;
				b.time = Math.max(0f, b.time - back);
			});
			return count[0];
		}

		/** Only enemy bullets that actually travel: beams and the like are not bullets in flight. */
		protected boolean affects(Bullet b){
			return b.team != team && b.type.speed > 0.2f && b.type.absorbable && b.vel.len() > 0.05f;
		}

		@Override
		public void drawSelect(){
			Drawf.dashCircle(x, y, range, ChronosColor.chrono);
		}

		@Override
		public void draw(){
			super.draw();
			if(warmup < 0.01f) return;

			Draw.z(Layer.shields);
			Draw.color(ChronosColor.chrono, 0.07f * warmup);
			Fill.circle(x, y, range);
			Draw.color(ChronosColor.chronoLight, 0.55f * warmup);
			Lines.stroke(1.8f);
			Lines.circle(x, y, range);

			//Tick marks and two hands at different speeds, like a clock face.
			Draw.color(ChronosColor.chrono, 0.4f * warmup);
			Lines.stroke(1f);
			for(int i = 0; i < 12; i++){
				float a = i * 30f;
				float len = i % 3 == 0 ? 10f : 5f;
				Lines.lineAngle(x + Mathf.cosDeg(a) * (range - len), y + Mathf.sinDeg(a) * (range - len), a, len);
			}
			Draw.color(ChronosColor.gold, 0.7f * warmup);
			Lines.stroke(1.6f);
			Lines.lineAngle(x, y, -spin * 1.5f, range * 0.85f);
			Lines.lineAngle(x, y, -spin * 0.125f, range * 0.55f);
			Draw.reset();
		}
	}
}
