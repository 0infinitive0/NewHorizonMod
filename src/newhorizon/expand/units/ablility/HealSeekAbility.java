package newhorizon.expand.units.ablility;

import arc.Core;
import arc.math.geom.Position;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.world.Block;
import mindustry.world.blocks.units.RepairTurret;
import newhorizon.content.jade.JadeUnits;

/**
 * Lets a unit look after itself: once it has been idle for a while and is not at full health, it walks to the nearest
 * powered repair block (a {@link RepairTurret}, so vanilla Repair Points count too) or to a Herbalist, waits there until it
 * is healed, then walks back to where it was.
 *
 * <p>It is an ability rather than part of an AI so that it works for every kind of controller. It only ever acts on a unit
 * that is standing still and neither shooting nor building, so it never fights with whatever the AI or the player is doing,
 * and it gives up the moment the unit gets busy. Player-controlled units are never touched.
 *
 * <p>Movement is a straight line, so ground units can get stuck behind walls on the way.
 */
public class HealSeekAbility extends Ability{
	/** Ticks of standing around, damaged, before the unit goes to heal. */
	public float idleTicks = 90f;
	/** How far away a healer may be. */
	public float maxRange = 2400f;
	/** A healer that has not healed anything for this long is ignored for {@link #ignoreTicks}. */
	public float stallTicks = 300f;
	public float ignoreTicks = 1800f;
	/** Fraction of the healer's range the unit moves into before it stops and waits. */
	public float arriveFraction = 0.5f;

	public static final int NONE = 0, GOING = 1, WAITING = 2, RETURNING = 3;

	public int phase = NONE;
	protected float idle, stall, lastHealth, ignoredUntil;
	protected Position source, ignored;
	protected Vec2 home = new Vec2(), move = new Vec2();

	private static Seq<RepairTurret> repairBlocks;

	/** Per-unit copy: a plain clone would share the position vectors between every unit with this ability. */
	@Override
	public Ability copy(){
		HealSeekAbility copy = (HealSeekAbility)super.copy();
		copy.home = new Vec2();
		copy.move = new Vec2();
		return copy;
	}

	@Override
	public void update(Unit unit){
		if(Vars.net.client() || unit.dead() || unit.isPlayer()){
			phase = NONE;
			return;
		}

		switch(phase){
			case NONE:
				if(!unit.damaged() || !stationary(unit) || busy(unit)){
					idle = 0f;
					return;
				}
				idle += Time.delta;
				if(idle < idleTicks) return;

				idle = 0f;
				source = findSource(unit);
				if(source == null) return;

				home.set(unit.x, unit.y);
				stall = 0f;
				lastHealth = unit.health;
				phase = GOING;
				break;

			case GOING: case WAITING:
				if(busy(unit)){
					phase = NONE;
					return;
				}
				if(!unit.damaged()){
					phase = RETURNING;
					return;
				}
				if(!valid(unit, source)){
					source = findSource(unit);
					if(source == null){
						phase = NONE;
						return;
					}
				}

				if(unit.within(source, arriveRadius(source))){
					phase = WAITING;
					if(unit.health > lastHealth + 0.01f){
						lastHealth = unit.health;
						stall = 0f;
					}else if((stall += Time.delta) >= stallTicks){
						ignored = source;
						ignoredUntil = Time.time + ignoreTicks;
						source = null;
						phase = NONE;
					}
				}else{
					phase = GOING;
					moveToward(unit, source);
				}
				break;

			case RETURNING:
				if(busy(unit) || unit.within(home, 16f)){
					phase = NONE;
					idle = 0f;
					return;
				}
				if(unit.damaged()){
					phase = NONE;
					return;
				}
				moveToward(unit, home);
				break;

			default:
				phase = NONE;
		}
	}

	/** Not shooting, not building. */
	protected boolean busy(Unit unit){
		return unit.isShooting || unit.activelyBuilding() || !unit.plans.isEmpty();
	}

	protected boolean stationary(Unit unit){
		float limit = unit.type.speed * 0.08f;
		return unit.vel.len2() <= limit * limit;
	}

	protected void moveToward(Unit unit, Position target){
		move.set(target.getX(), target.getY()).sub(unit.x, unit.y).limit(unit.speed());
		unit.movePref(move);
		unit.lookAt(target);
	}

	protected float arriveRadius(Position source){
		float range = source instanceof Building ? ((RepairTurret)((Building)source).block).repairRadius : JadeUnits.herbalistHealRange;
		return Math.max(8f, range * arriveFraction);
	}

	protected boolean valid(Unit unit, Position p){
		if(p == null) return false;
		if(p instanceof Building) return ((Building)p).isValid() && ((Building)p).efficiency > 0.01f;
		return p instanceof Unit && !((Unit)p).dead() && ((Unit)p).isAdded();
	}

	/** The nearest working repair block or Herbalist of the unit's team. */
	protected Position findSource(Unit unit){
		Position[] best = {null};
		float[] bestDst = {maxRange * maxRange};
		boolean skipIgnored = Time.time < ignoredUntil;

		for(RepairTurret block : repairBlocks()){
			for(Building b : unit.team.data().getBuildings(block)){
				if(!valid(unit, b) || (skipIgnored && b == ignored)) continue;
				float d = unit.dst2(b);
				if(d < bestDst[0]){
					bestDst[0] = d;
					best[0] = b;
				}
			}
		}

		Units.nearby(unit.team, unit.x - maxRange, unit.y - maxRange, maxRange * 2f, maxRange * 2f, other -> {
			if(other == unit || other.type != JadeUnits.herbalist || !valid(unit, other) || (skipIgnored && other == ignored)) return;
			float d = unit.dst2(other);
			if(d < bestDst[0]){
				bestDst[0] = d;
				best[0] = other;
			}
		});
		return best[0];
	}

	private static Seq<RepairTurret> repairBlocks(){
		if(repairBlocks == null){
			repairBlocks = new Seq<>();
			for(Block b : Vars.content.blocks()){
				if(b instanceof RepairTurret) repairBlocks.add((RepairTurret)b);
			}
		}
		return repairBlocks;
	}

	/** Forget the cached list, e.g. when content is reloaded. */
	public static void clearCache(){
		repairBlocks = null;
	}

	@Override
	public String localized(){
		return Core.bundle.get("ability.nh-healseek", "Self Care");
	}
}
