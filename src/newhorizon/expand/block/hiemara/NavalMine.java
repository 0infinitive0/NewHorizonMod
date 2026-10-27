package newhorizon.expand.block.hiemara;

import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Damage;
import mindustry.entities.Effect;
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.world.Block;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

/**
 * A mine for the water. Enemies can not target it, so they only find it by sailing over it, and it blows up once on the
 * first enemy naval unit that does. It arms itself a moment after it is built. Flying units never set it off.
 */
public class NavalMine extends Block{
	public float damage = 220f;
	/** World units. */
	public float radius = 5.5f * 8f;
	/** Ticks after being built until the mine can go off. */
	public float armTime = 120f;
	public Effect explosion = Fx.dynamicExplosion;
	
	public NavalMine(String name){
		super(name);
		
		update = true;
		solid = false;
		targetable = false;
		underBullets = true;
		hasShadow = false;
		placeableLiquid = true;
		rebuildable = false;
		health = 70;
	}
	
	@Override
	public void setStats(){
		super.setStats();
		
		stats.add(Stat.damage, damage, StatUnit.none);
		stats.add(Stat.range, radius / Vars.tilesize, StatUnit.blocks);
	}
	
	public class NavalMineBuild extends Building{
		protected float armed;
		
		@Override
		public void updateTile(){
			armed += Time.delta;
		}
		
		@Override
		public void unitOn(Unit unit){
			if(armed < armTime || unit.team == team || !unit.type.naval || unit.isFlying() || unit.dead)return;
			
			if(!Vars.net.client()){
				Damage.damage(team, x, y, radius, damage, false, false, true);
				explosion.at(x, y, radius / 8f);
				Sounds.explosion.at(x, y);
				kill();
			}
		}
	}
}
