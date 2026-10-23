package newhorizon.expand.block.omni;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.Teams;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.Env;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

import static mindustry.Vars.tilesize;

/**
 * Holds omni-matter deposits as they are. A deposit slowly wears away on its own (see OmniDeposit); while a stabilizer has power, the
 * deposits within {@link #range} of it do not. Deposits belong to nobody, so a stabilizer of any team protects them. It does nothing else:
 * the deposits can be mined as usual.
 */
public class OmniStabilizer extends Block{
	/** How far it reaches, in world units. */
	public float range = 12f * tilesize;
	public Color color = Color.valueOf("c885ff");

	public OmniStabilizer(String name){
		super(name);
		update = true;
		solid = true;
		hasPower = true;
		envEnabled = Env.any;
		buildType = OmniStabilizerBuild::new;
	}

	@Override
	public void setStats(){
		super.setStats();
		stats.add(Stat.range, range / tilesize, StatUnit.blocks);
	}

	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		super.drawPlace(x, y, rotation, valid);
		Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, color);
	}

	/** Every stabilizer that is built, of any team. */
	public static Seq<Building> all(Block block){
		Seq<Building> found = new Seq<>();
		for(Teams.TeamData data : Vars.state.teams.present){
			Seq<Building> list = data.getBuildings(block);
			if(list != null) found.addAll(list);
		}
		return found;
	}

	/** Whether a stabilizer from this list, that has power, reaches the tile. */
	public static boolean covers(Seq<Building> stabilizers, Tile tile){
		for(Building b : stabilizers){
			if(!b.isValid() || b.efficiency <= 0f || !(b.block instanceof OmniStabilizer)) continue;
			if(b.within(tile.worldx(), tile.worldy(), ((OmniStabilizer)b.block).range)) return true;
		}
		return false;
	}

	public class OmniStabilizerBuild extends Building{
		public float warmup;

		@Override
		public void updateTile(){
			warmup = Mathf.lerpDelta(warmup, efficiency > 0f ? 1f : 0f, 0.04f);
		}

		@Override
		public boolean shouldConsume(){
			return true;
		}

		@Override
		public void drawSelect(){
			Drawf.dashCircle(x, y, range, color);
		}

		@Override
		public void draw(){
			super.draw();
			if(warmup < 0.01f) return;
			//A slow pulse over the block while it is working, and a ring that swells out from it now and then.
			float pulse = 0.5f + 0.5f * Mathf.sin(Time.time, 40f, 1f);
			Draw.z(Layer.blockOver);
			Drawf.light(x, y, size * tilesize * 1.6f, color, 0.35f * warmup);
			Draw.color(color, (0.12f + 0.2f * pulse) * warmup);
			arc.graphics.g2d.Fill.circle(x, y, size * tilesize * 0.38f);
			float phase = (Time.time / 180f) % 1f;
			Draw.color(color, 0.35f * warmup * (1f - phase));
			Lines.stroke(1.4f);
			Lines.circle(x, y, size * tilesize * 0.5f + (range - size * tilesize * 0.5f) * phase * phase);
			Draw.reset();
		}
	}
}
