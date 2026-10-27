package newhorizon.expand.block.hiemara;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.gen.Call;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import newhorizon.expand.bullets.VortexBulletType;

/**
 * Opens a whirlpool under every enemy naval unit that comes into range. The whirlpool holds the unit and hurts it, see
 * {@link VortexBulletType}. Only a few can be open at once, and a unit that is in one already is left alone.
 */
public class VortexGenerator extends Block{
	public VortexBulletType vortex = new VortexBulletType(40f, 0.22f, 45f, 6f * 60f);
	/** Range in world units. */
	public float range = 30f * 8f;
	/** Ticks between two whirlpools. */
	public float interval = 150f;
	public int maxVortices = 3;
	/** How far ahead of a moving unit the whirlpool is opened, in ticks of its movement. */
	public float lead = 25f;
	
	public VortexGenerator(String name){
		super(name);
		
		update = true;
		solid = true;
		hasPower = true;
		placeableLiquid = true;
		ambientSound = mindustry.gen.Sounds.none;
		
		consumePower(4f);
	}
	
	@Override
	public void setStats(){
		super.setStats();
		
		stats.add(Stat.range, range / Vars.tilesize, StatUnit.blocks);
		stats.add(Stat.reload, 60f / interval, StatUnit.perSecond);
		stats.add(Stat.maxUnits, maxVortices);
		stats.add(Stat.damage, vortex.damagePerSecond, StatUnit.perSecond);
	}
	
	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		super.drawPlace(x, y, rotation, valid);
		
		Drawf.dashCircle(x * Vars.tilesize + offset, y * Vars.tilesize + offset, range, Pal.accent);
	}
	
	public class VortexGeneratorBuild extends mindustry.gen.Building{
		/** x, y and the time (in ticks) at which each open whirlpool ends. Server only. */
		protected final Seq<float[]> open = new Seq<>();
		protected float charge, spin;
		
		@Override
		public void updateTile(){
			spin += efficiency * Time.delta * 3f;
			if(Vars.net.client() || !enabled || efficiency <= 0.01f)return;
			
			open.removeAll(v -> v[2] <= Time.time);
			
			charge += edelta();
			if(charge < interval || open.size >= maxVortices)return;
			
			Unit target = Units.bestEnemy(team, x, y, range, u -> VortexBulletType.hits(u) && !caught(u), mindustry.entities.UnitSorts.closest);
			if(target == null)return;
			charge = 0f;
			
			float tx = target.x + target.vel.x * lead, ty = target.y + target.vel.y * lead;
			open.add(new float[]{tx, ty, Time.time + vortex.lifetime});
			Call.createBullet(vortex, team, tx, ty, 0f, 1f, 1f, 1f);
		}
		
		protected boolean caught(Unit unit){
			for(float[] v : open){
				if(Mathf.within(unit.x, unit.y, v[0], v[1], vortex.radius * 0.8f))return true;
			}
			return false;
		}
		
		@Override
		public void drawSelect(){
			Drawf.dashCircle(x, y, range, Pal.accent);
		}
		
		@Override
		public void draw(){
			super.draw();
			
			Draw.z(mindustry.graphics.Layer.blockOver);
			Draw.color(vortex.color, 0.35f + 0.4f * efficiency);
			Lines.stroke(1.5f);
			Lines.arc(x, y, size * Vars.tilesize * 0.32f, 0.6f, spin);
			Lines.arc(x, y, size * Vars.tilesize * 0.2f, 0.6f, -spin * 1.4f);
			Draw.reset();
		}
		
		@Override
		public void write(Writes write){
			super.write(write);
			write.f(charge);
		}
		
		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			charge = read.f();
		}
	}
}
