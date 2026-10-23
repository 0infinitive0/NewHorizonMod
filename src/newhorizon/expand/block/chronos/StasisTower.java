package newhorizon.expand.block.chronos;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.world.Block;
import mindustry.world.meta.Env;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import newhorizon.content.chronos.ChronosColor;
import newhorizon.content.chronos.ChronosFx;
import newhorizon.content.chronos.ChronosStatus;

import static mindustry.Vars.tilesize;

/**
 * Stops time for every enemy unit inside its range: they cannot move or shoot for as long as they stay there and the tower has
 * power. The effect is refreshed a few times a second and runs out soon after a unit leaves.
 */
public class StasisTower extends Block{
	public float range = 150f;
	/** Ticks between two refreshes of the status. */
	public float interval = 12f;
	/** How long one refresh lasts. */
	public float holdTime = 36f;
	public TextureRegion topRegion;

	public StasisTower(String name){
		super(name);
		update = true;
		solid = true;
		hasPower = true;
		envEnabled = Env.any;
	}

	@Override
	public void load(){
		super.load();
		topRegion = Core.atlas.find(name + "-top", region);
	}

	@Override
	public void setStats(){
		super.setStats();
		stats.add(Stat.range, range / tilesize, StatUnit.blocks);
	}

	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		super.drawPlace(x, y, rotation, valid);
		Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, ChronosColor.violet);
	}

	public class StasisTowerBuild extends Building{
		public float warmup, timer, spin;

		@Override
		public void updateTile(){
			boolean on = efficiency > 0.01f;
			warmup = Mathf.lerpDelta(warmup, on ? 1f : 0f, 0.04f);
			spin += warmup * 1.6f * Time.delta;
			if(!on) return;

			timer += edelta();
			if(timer >= interval){
				timer = 0f;
				hold();
			}
			if(Mathf.chanceDelta(0.02 * warmup)) ChronosFx.timeRing.at(x, y, range * 0.9f);
		}

		/** Freezes every enemy unit in range. Returns how many. */
		public int hold(){
			int[] count = {0};
			Units.nearbyEnemies(team, x - range - 16f, y - range - 16f, (range + 16f) * 2f, (range + 16f) * 2f, unit -> {
				if(unit.dead() || !unit.within(this, range + unit.hitSize / 2f)) return;
				unit.apply(ChronosStatus.stasis, holdTime);
				count[0]++;
			});
			return count[0];
		}

		@Override
		public void drawSelect(){
			Drawf.dashCircle(x, y, range, ChronosColor.violet);
		}

		@Override
		public void draw(){
			super.draw();
			Draw.z(Layer.block + 0.1f);
			Draw.rect(topRegion, x, y, spin);
			if(warmup > 0.01f){
				Draw.z(Layer.shields);
				Draw.color(ChronosColor.violet, 0.12f * warmup);
				Lines.stroke(1.4f);
				Lines.circle(x, y, range);
				Draw.color(ChronosColor.chrono, 0.5f * warmup);
				Lines.stroke(1.2f);
				Lines.arc(x, y, range, 0.12f, spin * 3f);
				Lines.arc(x, y, range, 0.12f, spin * 3f + 180f);
				Draw.reset();
			}
		}
	}
}
