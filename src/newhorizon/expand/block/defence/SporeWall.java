package newhorizon.expand.block.defence;

import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.Effect;
import mindustry.entities.Fires;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.gen.Fire;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import newhorizon.content.jade.JadeFx;
import newhorizon.content.jade.JadeWater;

/**
 * A wall of living spores: a huge pool of health that grows back by itself, and a weakness to fire.
 *
 * <ul>
 *     <li><b>Regrowth:</b> after a short while without taking damage it heals a share of its max health every second, twice as fast
 *     on a water floor.</li>
 *     <li><b>Weak to fire:</b> fiery bullets (incendiary, flames, anything that sets things alight) deal extra damage and ignite it.
 *     So does a fire burning on its tile.</li>
 *     <li><b>Burning:</b> once alight it keeps burning, taking a share of its max health every second that ignores armor, unable to
 *     heal, and it spreads to neighbouring spore walls. A burning wall always has a real fire on its tile, which never burns out by
 *     itself, and the wall burns exactly as long as that fire exists. It is therefore put out the way any other block's fire is:
 *     liquid turrets (Wave, Tsunami) target it and water or cryofluid bullets extinguish it, and rain puts it out as it does any
 *     fire. A wall that stands on water cannot burn.</li>
 * </ul>
 */
public class SporeWall extends Wall{
	/** Ticks without damage before regrowth starts. */
	public float regrowDelay = 120f;
	/** Health regained per second, as a fraction of max health. */
	public float regrowPercent = 0.025f;
	/** Regrowth is multiplied by this on a water floor. */
	public float waterRegrowMultiplier = 2f;
	/** Damage fiery bullets deal compared to normal. */
	public float fireDamageMultiplier = 2.5f;
	/** Health lost per second while burning, as a fraction of max health. Ignores armor. */
	public float burnPercent = 0.04f;
	/** Chance per second that a burning wall sets each neighbouring spore wall alight. */
	public float spreadChance = 0.25f;
	public Effect regrowEffect = JadeFx.leafBurst;
	/**
	 * How much a fire ages per tick on a dry map in this version of the game: one from the entity's own timer and one from the
	 * fire's burn rate. Wet weather adds ten times the environment's water attribute on top.
	 */
	public static final float baseBurnRate = 2f;

	private static final Seq<Tile> linked = new Seq<>();

	public SporeWall(String name){
		super(name);
		update = true;
	}

	@Override
	public void setStats(){
		super.setStats();
		stats.add(Stat.repairSpeed, regrowPercent * 100f, StatUnit.percent);
	}

	/** Anything that sets things alight. */
	public static boolean isFiery(BulletType type){
		return type.incendAmount > 0 || type.makeFire || type.status == StatusEffects.burning || type.status == StatusEffects.melting;
	}

	public class SporeWallBuild extends WallBuild{
		public float lastDamaged = -99999f;
		public boolean burning;
		protected float spreadTimer, fireCheck;

		/** A wall that stands on water cannot burn. */
		public boolean onWater(){
			return JadeWater.isWater(tile.floor());
		}

		/** Sets the wall alight: it burns as long as the fire on its tile does. Returns false if it cannot burn here. */
		public boolean ignite(){
			if(onWater()) return false;
			burning = true;
			Fires.create(tile);
			return true;
		}

		public void douse(){
			burning = false;
		}

		@Override
		public void damage(float amount){
			lastDamaged = Time.time;
			super.damage(amount);
		}

		@Override
		public boolean collision(Bullet other){
			BulletType type = other.type;
			boolean fiery = isFiery(type);
			if(fiery) ignite();

			boolean result = super.collision(other);

			if(fiery && fireDamageMultiplier > 1f){
				//Weak to fire: the hit counts for more.
				damage(other.team, other.damage() * type.buildingDamageMultiplier * (fireDamageMultiplier - 1f));
			}
			return result;
		}

		@Override
		public void updateTile(){
			if(mindustry.Vars.net.client()) return;

			if(!burning){
				fireCheck += Time.delta;
				if(fireCheck >= 10f){
					fireCheck = 0f;
					if(burningTile()) ignite();
				}
			}

			if(burning){
				Fire fire = Fires.get(tile.x, tile.y);
				if(onWater()){
					Fires.remove(tile);
					douse();
				}else if(fire == null || fire.time() >= fire.lifetime()){
					//Put out, by a liquid turret, a water bullet or anything else that extinguishes fires.
					douse();
				}else{
					//A fire ages by itself: by baseBurnRate per tick on a dry map, and faster in wet weather. Only the first part is given
					//back, so the fire never burns out on its own, while whatever extinguishing adds, and what rain adds, still counts.
					fire.lifetime(fire.lifetime() + Time.delta * baseBurnRate);
					burn();
					return;
				}
			}

			if(health < maxHealth && Time.time - lastDamaged >= regrowDelay){
				heal(maxHealth * regrowPercent * (onWater() ? waterRegrowMultiplier : 1f) * Time.delta / 60f);
				if(Mathf.chanceDelta(0.03f)) regrowEffect.at(x + Mathf.range(size * 3f), y + Mathf.range(size * 3f));
			}
		}

		/** A fire on any tile of the wall sets it alight. */
		protected boolean burningTile(){
			for(Tile t : tile.getLinkedTilesAs(block, linked.clear())){
				if(Fires.has(t.x, t.y)) return true;
			}
			return false;
		}

		protected void burn(){
			damagePierce(maxHealth * burnPercent * Time.delta / 60f);
			if(dead) return;

			if(Mathf.chanceDelta(0.25f)) Fx.burning.at(x + Mathf.range(size * 3f), y + Mathf.range(size * 3f));

			spreadTimer += Time.delta;
			if(spreadTimer >= 60f){
				spreadTimer = 0f;
				for(Building other : proximity){
					if(other instanceof SporeWallBuild && Mathf.chance(spreadChance)) ((SporeWallBuild)other).ignite();
				}
			}
		}

		@Override
		public void write(Writes write){
			super.write(write);
			write.bool(burning);
		}

		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			burning = read.bool();
		}
	}
}
