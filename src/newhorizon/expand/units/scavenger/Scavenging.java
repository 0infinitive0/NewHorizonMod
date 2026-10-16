package newhorizon.expand.units.scavenger;

import arc.Core;
import arc.Events;
import arc.math.Rand;
import arc.struct.IntFloatMap;
import arc.struct.IntSeq;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.entities.abilities.Ability;
import mindustry.content.StatusEffects;
import mindustry.type.StatusEffect;
import mindustry.gen.Player;
import mindustry.type.UnitType;
import newhorizon.content.jade.JadeFx;
import newhorizon.util.ui.UnitInfoTooltip;
import newhorizon.content.jade.JadeUnits;

/**
 * Decides who gets credit for a kill. Taking part is enough: every Scavenger that damaged an enemy, within
 * {@link #participationSeconds} before it died, is rewarded, not only the one that landed the last hit.
 */
public class Scavenging{
	/** Share of the victim's max health / armor a Scavenger takes is rolled between these two. */
	public static float minSteal = 0.05f, maxSteal = 0.20f;
	/** Chance, per kill, to also steal one of the victim's weapons / abilities. */
	public static float weaponChance = 0.08f, abilityChance = 0.04f;
	/** Stealing a weapon it already owns improves that weapon's damage or fire rate by this much (one stat per steal). */
	public static float upgradeStep = 0.10f;
	/** Chance, per kill of a flying / naval enemy, to also take its flight / swimming. */
	public static float flightChance = 0.05f, swimChance = 0.05f;
	/** A hit counts towards a kill for this long. */
	public static float participationSeconds = 120f;
	public static Rand rand = new Rand();

	/** Attacker unit id -> time of its latest hit. */
	private static class Participants{
		final IntFloatMap hits = new IntFloatMap();
	}

	private static final ObjectMap<Object, Participants> victims = new ObjectMap<>();
	private static boolean hooked;
	private static float pruneTimer;

	public static void init(){
		if(hooked) return;
		hooked = true;

		Events.on(EventType.UnitDamageEvent.class, e -> {
			if(!Vars.net.client()) record(e.unit, e.unit.team, e.bullet);
		});
		Events.on(EventType.BuildDamageEvent.class, e -> {
			if(!Vars.net.client()) record(e.build, e.build.team, e.source);
		});
		Events.on(EventType.UnitDestroyEvent.class, e -> {
			if(!Vars.net.client()) unitDied(e.unit);
		});
		Events.on(EventType.BlockDestroyEvent.class, e -> {
			if(!Vars.net.client() && e.tile != null && e.tile.build != null) buildingDied(e.tile.build);
		});
		Events.on(EventType.WorldLoadEvent.class, e -> victims.clear());
		Events.on(EventType.ResetEvent.class, e -> victims.clear());
		Events.run(EventType.Trigger.update, () -> {
			pruneTimer += Time.delta;
			if(pruneTimer >= 300f){
				pruneTimer = 0f;
				prune();
			}
		});
	}

	/** The Scavenger that fired this bullet, if any. */
	public static Unit scavengerOf(Bullet bullet){
		if(bullet == null || !(bullet.owner instanceof Unit)) return null;
		Unit unit = (Unit)bullet.owner;
		return JadeUnits.isScavenger(unit.type) && !unit.dead() ? unit : null;
	}

	public static ScavengerAbility abilityOf(Unit unit){
		for(Ability a : unit.abilities){
			if(a instanceof ScavengerAbility) return (ScavengerAbility)a;
		}
		return null;
	}

	/** Notes that the bullet's owner, if it is a Scavenger of another team, hurt the victim. This runs on every hit in the game, so it exits early. */
	public static void record(Object victim, Team victimTeam, Bullet bullet){
		Unit scavenger = scavengerOf(bullet);
		if(scavenger == null || scavenger.team == victimTeam) return;
		record(victim, scavenger);
	}

	public static void record(Object victim, Unit scavenger){
		Participants p = victims.get(victim);
		if(p == null) victims.put(victim, p = new Participants());
		p.hits.put(scavenger.id, Time.time);
	}

	public static void unitDied(Unit victim){
		Participants p = victims.remove(victim);
		if(p == null) return;
		//A victim from another mod may have unusual weapons or abilities. If they cannot be listed, only health and armor are taken.
		Seq<String> weapons = new Seq<>(), abilities = new Seq<>();
		try{
			weapons = ScavengeRecipes.weaponRecipes(victim.type);
			abilities = ScavengeRecipes.abilityRecipes(victim.type);
		}catch(RuntimeException ignored){
		}
		payOut(p, victim.team, victim.maxHealth, victim.armor, weapons, abilities, victim.type);
	}

	public static void buildingDied(Building victim){
		Participants p = victims.remove(victim);
		if(p == null) return;
		Seq<String> weapons = new Seq<>();
		try{
			weapons = ScavengeRecipes.weaponRecipes(victim.block);
		}catch(RuntimeException ignored){
		}
		payOut(p, victim.team, victim.maxHealth(), victim.block.armor, weapons, new Seq<>(), null);
	}

	private static void payOut(Participants p, Team victimTeam, float maxHealth, float armor, Seq<String> weapons, Seq<String> abilities, UnitType victimType){
		float window = participationSeconds * 60f;
		IntFloatMap.Entries entries = p.hits.entries();
		while(entries.hasNext){
			IntFloatMap.Entry hit = entries.next();
			if(Time.time - hit.value > window) continue;

			Unit scavenger = Groups.unit.getByID(hit.key);
			if(scavenger == null || scavenger.dead() || !JadeUnits.isScavenger(scavenger.type) || scavenger.team == victimTeam) continue;

			ScavengerAbility ability = abilityOf(scavenger);
			if(ability == null) continue;
			ability.reward(scavenger, maxHealth, armor, weapons, abilities, rand);

			//Shooting down a flyer or sinking a boat may also teach it to fly or swim. It becomes another kind of unit, which cannot be
			//done in the middle of the damage that killed the victim: that waits for the next frame.
			if(victimType != null){
				boolean fly = victimType.flying && rand.chance(flightChance), swim = victimType.naval && rand.chance(swimChance);
				UnitType next = JadeUnits.scavengerWith(scavenger.type, fly, swim);
				if(next != null){
					Core.app.post(() -> {
						if(scavenger.isAdded() && !scavenger.dead()) evolve(scavenger, next);
					});
				}
			}
		}
	}

	/**
	 * Replaces a Scavenger with the same Scavenger of another kind (see {@link JadeUnits#scavengerWith}): same place, heading, team,
	 * share of health, statuses and loot, but a different way of moving. Server side only.
	 *
	 * @return the new unit, or null if nothing was done
	 */
	public static Unit evolve(Unit old, UnitType to){
		if(Vars.net.client() || to == null || old.type == to || !old.isValid() || old.dead()) return null;
		ScavengerAbility oldAbility = abilityOf(old);
		if(oldAbility == null) return null;

		String loot = oldAbility.serialize();
		Unit copy = to.create(old.team);
		ScavengerAbility ability = abilityOf(copy);
		if(ability == null) return null;

		copy.set(old.x, old.y);
		copy.rotation = old.rotation;
		copy.vel.set(old.vel);

		//The loot is read back exactly as it is after loading a save, which also puts back its health and armor.
		float share = old.maxHealth <= 0f ? 1f : Math.min(1f, old.health / old.maxHealth);
		ability.restored = true;
		ability.apply(copy, loot);
		copy.health = copy.maxHealth * share;
		copy.shield = old.shield;
		for(StatusEffect effect : Vars.content.statusEffects()){
			if(effect == StatusEffects.none) continue;
			float left = old.getDuration(effect);
			if(left > 0f || old.hasEffect(effect)){
				try{
					copy.apply(effect, effect.permanent ? 1f : Math.max(1f, left));
				}catch(RuntimeException ignored){
				}
			}
		}

		Player pilot = old.isPlayer() ? old.getPlayer() : null;
		float x = old.x, y = old.y, size = old.hitSize;
		if(Vars.state != null) Vars.state.rules.tags.remove(ScavengerAbility.TAG + old.id);

		old.remove();
		copy.add();
		if(pilot != null) pilot.unit(copy);
		ability.save(copy);
		UnitInfoTooltip.replaced(old, copy);

		JadeFx.leafBurstBig.at(x, y);
		JadeFx.bloomRing.at(x, y, size);
		return copy;
	}

	/** Whether the Scavenger could eat this unit: a friendly one that is not piloted and has a weapon or ability to give. */
	public static boolean canSacrifice(Unit scavenger, Unit victim){
		if(victim == scavenger || victim.team != scavenger.team || !victim.isValid() || victim.dead() || victim.isPlayer()) return false;
		if(JadeUnits.isScavenger(victim.type) || victim.type.isHidden()) return false;
		try{
			return !ScavengeRecipes.weaponRecipes(victim.type).isEmpty() || !ScavengeRecipes.abilityRecipes(victim.type).isEmpty();
		}catch(RuntimeException e){
			return false;
		}
	}

	/**
	 * The Scavenger consumes a friendly unit and takes one random weapon and one random ability of its kind. The unit is only used up
	 * if something was really gained. Server side only.
	 */
	public static boolean sacrifice(Unit scavenger, Unit victim){
		if(Vars.net.client() || !canSacrifice(scavenger, victim)) return false;
		ScavengerAbility ability = abilityOf(scavenger);
		if(ability == null) return false;

		Seq<String> weapons = new Seq<>(), abilities = new Seq<>();
		try{
			weapons = ScavengeRecipes.weaponRecipes(victim.type);
			abilities = ScavengeRecipes.abilityRecipes(victim.type);
		}catch(RuntimeException ignored){
		}
		if(!ScavengerAbility.gained(ability.sacrifice(scavenger, weapons, abilities, rand))) return false;

		float x = victim.x, y = victim.y;
		victim.remove();
		JadeFx.leafBurstBig.at(x, y);
		JadeFx.bloomRing.at(x, y, victim.hitSize);
		return true;
	}

	/** Forgets old hits so the table does not grow forever. */
	public static void prune(){
		float window = participationSeconds * 60f;
		Seq<Object> empty = new Seq<>();
		for(ObjectMap.Entry<Object, Participants> e : victims){
			IntSeq old = new IntSeq();
			IntFloatMap.Entries entries = e.value.hits.entries();
			while(entries.hasNext){
				IntFloatMap.Entry hit = entries.next();
				if(Time.time - hit.value > window) old.add(hit.key);
			}
			for(int i = 0; i < old.size; i++) e.value.hits.remove(old.get(i), 0f);
			if(e.value.hits.isEmpty()) empty.add(e.key);
		}
		for(Object key : empty) victims.remove(key);
	}

	public static int tracked(){
		return victims.size;
	}

	public static void clear(){
		victims.clear();
	}
}
