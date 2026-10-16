package newhorizon.expand.units.scavenger;

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
		return unit.type == JadeUnits.scavenger && !unit.dead() ? unit : null;
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
		payOut(p, victim.team, victim.maxHealth, victim.armor, ScavengeRecipes.weaponRecipes(victim.type), ScavengeRecipes.abilityRecipes(victim.type));
	}

	public static void buildingDied(Building victim){
		Participants p = victims.remove(victim);
		if(p == null) return;
		payOut(p, victim.team, victim.maxHealth(), victim.block.armor, ScavengeRecipes.weaponRecipes(victim.block), new Seq<>());
	}

	private static void payOut(Participants p, Team victimTeam, float maxHealth, float armor, Seq<String> weapons, Seq<String> abilities){
		float window = participationSeconds * 60f;
		IntFloatMap.Entries entries = p.hits.entries();
		while(entries.hasNext){
			IntFloatMap.Entry hit = entries.next();
			if(Time.time - hit.value > window) continue;

			Unit scavenger = Groups.unit.getByID(hit.key);
			if(scavenger == null || scavenger.dead() || scavenger.type != JadeUnits.scavenger || scavenger.team == victimTeam) continue;

			ScavengerAbility ability = abilityOf(scavenger);
			if(ability != null) ability.reward(scavenger, maxHealth, armor, weapons, abilities, rand);
		}
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
