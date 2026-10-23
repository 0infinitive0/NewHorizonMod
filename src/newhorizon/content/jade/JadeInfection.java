package newhorizon.content.jade;

import arc.Events;
import arc.math.Mathf;
import arc.struct.IntMap;
import arc.struct.IntSeq;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Unit;

/**
 * Bookkeeping for {@link JadeStatus#infested}.
 *
 * <p>A status effect does not remember who applied it, so every infection is recorded here together with the
 * infecting team. An infection ends in one of two ways:
 * <ul>
 *     <li>the host dies: {@code maxHealth / }{@link #healthPerSpawn} {@link JadeUnits#sporeling}s burst out for the infecting team;</li>
 *     <li>the host was hurt and has been healed back to full health: the infection is cured.</li>
 * </ul>
 */
public class JadeInfection{
	/** One Sporeling is spawned per this much max health of the dead host. */
	public static final float healthPerSpawn = 100f;
	/** Safety cap, so killing a huge boss cannot flood the world with units. */
	public static final int maxSpawnPerDeath = 40;

	private static final IntMap<Infection> infected = new IntMap<>();
	private static final IntSeq finished = new IntSeq();

	private static class Infection{
		final Team team;
		/** Only a host that has actually been hurt can be "healed to full". */
		boolean hurt;

		Infection(Team team){
			this.team = team;
		}
	}

	public static void load(){
		Events.on(EventType.UnitDestroyEvent.class, e -> onDeath(e.unit));
		Events.run(EventType.Trigger.update, JadeInfection::update);
		Events.on(EventType.ResetEvent.class, e -> infected.clear());
	}

	public static boolean isInfected(Unit unit){
		return infected.containsKey(unit.id);
	}

	/**
	 * Whether the unit carries the status. {@code hasEffect} alone is not enough: it reads a bitset that is only refreshed
	 * on the unit's next update, so it is still false in the frame the status was applied (e.g. by the explosion).
	 */
	private static boolean carriesStatus(Unit unit){
		return unit.hasEffect(JadeStatus.infested) || unit.getDuration(JadeStatus.infested) > 0f;
	}

	/** Registers every enemy of {@code source} near the explosion that now carries the infestation. */
	public static void infectArea(Team source, float x, float y, float radius){
		if(Vars.net.client()) return;

		Units.nearbyEnemies(source, x - radius, y - radius, radius * 2f, radius * 2f, unit -> {
			if(!unit.within(x, y, radius + unit.hitSize / 2f)) return;
			infect(unit, source);
		});
	}

	/** Records {@code host} as infected by {@code source}, if it actually carries the status. The first infector wins. */
	public static void infect(Unit host, Team source){
		//Immune units never received the status, so they are not hosts.
		if(!carriesStatus(host)) return;
		if(!infected.containsKey(host.id)) infected.put(host.id, new Infection(source));
	}

	private static void update(){
		if(infected.isEmpty()) return;

		finished.clear();
		for(IntMap.Entry<Infection> entry : infected){
			Unit unit = Groups.unit.getByID(entry.key);

			//Dead hosts stay registered until their destroy event: flying units keep falling for a while after dying.
			if(unit == null || !unit.isAdded() || (!unit.dead() && !carriesStatus(unit))){
				finished.add(entry.key);
			}else if(!unit.dead()){
				if(unit.health < unit.maxHealth){
					entry.value.hurt = true;
				}else if(entry.value.hurt){
					unit.unapply(JadeStatus.infested);
					finished.add(entry.key);
				}
			}
		}

		for(int i = 0; i < finished.size; i++){
			infected.remove(finished.get(i));
		}
	}

	private static void onDeath(Unit host){
		Infection infection = infected.remove(host.id);
		if(infection == null || Vars.net.client()) return;

		int count = Mathf.clamp((int)(host.maxHealth / healthPerSpawn), 1, maxSpawnPerDeath);
		for(int i = 0; i < count; i++){
			Tmp.v1.trns(Mathf.random(360f), Mathf.random(host.hitSize / 2f));
			JadeUnits.sporeling.spawn(infection.team, host.x + Tmp.v1.x, host.y + Tmp.v1.y);
		}

		JadeFx.leafBurstBig.at(host.x, host.y);
		JadeFx.bloomRing.at(host.x, host.y);
	}
}
