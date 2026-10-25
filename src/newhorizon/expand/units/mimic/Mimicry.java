package newhorizon.expand.units.mimic;

import arc.math.geom.Vec2;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.gen.WaterMovec;
import mindustry.type.UnitType;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import newhorizon.content.jade.JadeFx;
import newhorizon.content.jade.JadeStatus;
import newhorizon.content.jade.JadeUnits;
import newhorizon.content.jade.JadeWater;

/**
 * The Mimic's transformation. It has no weapons of its own. Next to another unit, friendly or enemy, it becomes that kind of
 * unit for good: a new unit of the target's type replaces it, in the same place, on the same team, with the same share of health
 * left, so it has every weapon and ability the target type has. What it keeps from the Mimic are its toughness, health and armor:
 * the new unit wears the {@link JadeStatus#mimicked} status, which holds those two numbers on it for as long as it lives (also
 * through saving and loading, as statuses are saved with the unit). It is no longer a Mimic and cannot transform again.
 */
public class Mimicry{
	/** Why a transformation is not possible: the reasons {@link #blocker} reports. */
	public static final String
		notMimic = "not a mimic", invalidTarget = "invalid target", selfTarget = "itself", noMimics = "mimics cannot be copied",
		hidden = "hidden unit", banned = "banned unit", tooFar = "out of range", capReached = "unit cap reached",
		blockedTerrain = "cannot stand here", needsWater = "naval units need water", wouldDrown = "would drown here";

	/** The mimic's own health and armor, which every copy keeps. */
	public static float health(){
		return JadeUnits.mimic.health;
	}

	public static float armor(){
		return JadeUnits.mimic.armor;
	}

	public static boolean isMimic(Unit unit){
		return unit != null && unit.type == JadeUnits.mimic;
	}

	/** Range at which a mimic copies, from its ability. */
	public static float range(Unit mimic){
		for(mindustry.entities.abilities.Ability a : mimic.abilities){
			if(a instanceof MimicAbility) return ((MimicAbility)a).range;
		}
		return 0f;
	}

	/** A unit worth turning into on its own: one that has weapons or abilities to take. */
	public static boolean worthCopying(UnitType type){
		return !type.weapons.isEmpty() || !type.abilities.isEmpty();
	}

	/** Whether {@code floor} is a place a unit of the type could be dropped without it drowning or being stuck. */
	private static String terrain(Unit mimic, UnitType type, Unit probe){
		Tile tile = mimic.tileOn();
		Floor floor = mimic.floorOn();

		if(type.naval || probe instanceof WaterMovec){
			//A boat only works afloat: the mimic has to be over water.
			if(tile == null || tile.solid() || floor == null || !JadeWater.isWater(floor)) return needsWater;
		}else if(!type.flying){
			if(tile == null || !probe.canPass(tile.x, tile.y)) return blockedTerrain;
			if(type.canDrown && floor != null && floor.isDeep() && !type.hovering) return wouldDrown;
		}
		return null;
	}

	/**
	 * Why {@code mimic} cannot become a copy of {@code target} right now, or null when it can. {@code ignoreRange} is for the AI,
	 * which walks up to its target first.
	 */
	public static String blocker(Unit mimic, Unit target, boolean ignoreRange){
		if(!isMimic(mimic)) return notMimic;
		if(target == null || !target.isValid() || target.dead()) return invalidTarget;
		if(target == mimic) return selfTarget;
		if(isMimic(target)) return noMimics;
		UnitType type = target.type;
		if(type.internal || type.isHidden()) return hidden;
		if(type.isBanned()) return banned;
		if(!ignoreRange && !mimic.within(target, range(mimic) + target.hitSize / 2f)) return tooFar;
		if(!Units.canCreate(mimic.team, type)) return capReached;
		return null;
	}

	/** Like {@link #blocker} but also checks the ground under the mimic, which a unit of the target's type has to be able to stand on. */
	public static String blockerHere(Unit mimic, Unit target, boolean ignoreRange){
		String why = blocker(mimic, target, ignoreRange);
		if(why != null) return why;
		Unit probe = target.type.create(mimic.team);
		return terrain(mimic, target.type, probe);
	}

	/** Every unit the mimic could copy from where it is, nearest first. */
	public static Seq<Unit> candidates(Unit mimic, float radius, boolean weaponsOrAbilitiesOnly){
		Seq<Unit> out = new Seq<>();
		Units.nearby(mimic.x - radius, mimic.y - radius, radius * 2f, radius * 2f, u -> {
			if(u == mimic || !u.within(mimic, radius + u.hitSize / 2f)) return;
			if(weaponsOrAbilitiesOnly && !worthCopying(u.type)) return;
			if(blocker(mimic, u, true) != null) return;
			out.add(u);
		});
		out.sort(u -> u.dst2(mimic));
		return out;
	}

	/** The candidate nearest to a point, in range: what a pilot points at. */
	public static Unit nearestTo(Unit mimic, float x, float y){
		Unit best = null;
		float bestDst = Float.MAX_VALUE;
		float range = range(mimic);
		for(Unit u : candidates(mimic, range + 32f, false)){
			if(blockerHere(mimic, u, false) != null) continue;
			float d = u.dst2(x, y);
			if(d < bestDst){
				best = u;
				bestDst = d;
			}
		}
		return best;
	}

	/**
	 * Replaces {@code mimic} with a unit of {@code target}'s type that keeps the mimic's health share, team, place, heading and
	 * momentum. Returns the new unit, or null if it was not possible. Server side only.
	 */
	public static Unit transform(Unit mimic, Unit target){
		if(Vars.net.client()) return null;
		if(blockerHere(mimic, target, false) != null) return null;

		UnitType type = target.type;
		Unit copy = type.create(mimic.team);
		copy.set(mimic.x, mimic.y);
		copy.rotation = mimic.rotation;
		copy.vel.set(mimic.vel);

		//The toughness of the mimic stays with it.
		float share = mimic.maxHealth <= 0f ? 1f : Math.min(1f, mimic.health / mimic.maxHealth);
		copy.apply(JadeStatus.mimicked, 1f);
		copy.maxHealth = health();
		copy.armor = armor();
		copy.health = copy.maxHealth * share;
		copy.shield = mimic.shield;

		Player pilot = mimic.isPlayer() ? mimic.getPlayer() : null;
		float x = mimic.x, y = mimic.y, size = mimic.hitSize;

		mimic.remove();
		copy.add();
		if(pilot != null) pilot.unit(copy);

		JadeFx.leafBurstBig.at(x, y);
		JadeFx.bloomRing.at(x, y, size);
		return copy;
	}

	/** Used by the AI and the pilot to look for the unit under a point. Convenience for tests. */
	public static Vec2 aim(Unit unit){
		return new Vec2(unit.aimX, unit.aimY);
	}
}
