package newhorizon.expand.units.spore;

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
import newhorizon.content.spore.SporeContent;
import newhorizon.content.spore.SporeFx;

/**
 * Bookkeeping for Blight, the Spore Mote's infection (the Jade Accord's has its own, {@code JadeInfection}, and the two do not touch).
 *
 * <p>A status effect does not remember who applied it, so every infection is recorded here together with the infecting team. An
 * infection ends in one of two ways: the host dies (it bursts into one Spore Mote per {@link #healthPerSpawn} max health for the infecting
 * team), or the host was hurt and has been healed back to full health (cured). While it lasts the host seeks out the closest
 * uninfected unit of its own team ({@link BlightSeekAI}).
 */
public class BlightInfection{
	/** One Spore Mote is spawned per this much max health of the dead host. */
	public static final float healthPerSpawn = 100f;
	/** Safety cap, so killing a huge boss cannot flood the world with units. */
	public static final int maxSpawnPerDeath = 40;

	private static final IntMap<Infection> infected = new IntMap<>();
	private static final IntSeq finished = new IntSeq();
	private static boolean hooked;

	private static class Infection{
		final Team team;
		/** Only a host that has actually been hurt can be "healed to full". */
		boolean hurt;

		Infection(Team team){
			this.team = team;
		}
	}

	public static void load(){
		if(hooked) return;
		hooked = true;
		Events.on(EventType.UnitDestroyEvent.class, e -> onDeath(e.unit));
		Events.run(EventType.Trigger.update, BlightInfection::update);
		Events.on(EventType.ResetEvent.class, e -> infected.clear());
	}

	public static boolean isInfected(Unit unit){
		return infected.containsKey(unit.id);
	}

	/** Registers every enemy of {@code source} near the explosion that now carries Blight. */
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
		if(!SporeContent.carriesBlight(host)) return;
		if(!infected.containsKey(host.id)) infected.put(host.id, new Infection(source));
	}

	/** One tick of the bookkeeping (public so a test can drive it). */
	public static void update(){
		if(infected.isEmpty() || Vars.net.client()) return;

		finished.clear();
		for(IntMap.Entry<Infection> entry : infected){
			Unit unit = Groups.unit.getByID(entry.key);

			//Dead hosts stay registered until their destroy event: flying units keep falling for a while after dying.
			if(unit == null || !unit.isAdded() || (!unit.dead() && !SporeContent.carriesBlight(unit))){
				finished.add(entry.key);
			}else if(!unit.dead()){
				BlightSeekAI.wrap(unit);
				if(unit.health < unit.maxHealth){
					entry.value.hurt = true;
				}else if(entry.value.hurt){
					unit.unapply(SporeContent.blight);
					finished.add(entry.key);
				}
			}
		}

		for(int i = 0; i < finished.size; i++){
			int id = finished.get(i);
			BlightSeekAI.release(Groups.unit.getByID(id));
			infected.remove(id);
		}
	}

	private static void onDeath(Unit host){
		Infection infection = infected.remove(host.id);
		BlightSeekAI.release(host);
		if(infection == null || Vars.net.client()) return;

		int count = Mathf.clamp((int)(host.maxHealth / healthPerSpawn), 1, maxSpawnPerDeath);
		for(int i = 0; i < count; i++){
			Tmp.v1.trns(Mathf.random(360f), Mathf.random(host.hitSize / 2f));
			SporeContent.sporeMote.spawn(infection.team, host.x + Tmp.v1.x, host.y + Tmp.v1.y);
		}

		SporeFx.moteBurst.at(host.x, host.y);
		SporeFx.moteRing.at(host.x, host.y);
	}
}
