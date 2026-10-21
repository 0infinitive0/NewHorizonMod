package newhorizon.expand.units.serpent;

import arc.struct.IntMap;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.UnitType;

/**
 * Who is in a team's Serpent, and in what order. A team has one head, and its body sections follow it in the order they were made (by
 * unit id, which the game saves): nothing has to be stored to know which section is behind which, and when a section is lost the one
 * behind it simply has a new predecessor and closes up. Worked out at most once a tick, and only when a Serpent unit asks.
 */
public class SerpentChain{
	/** Set when the units are made. */
	public static UnitType head, body;

	private static final IntMap<Cache> caches = new IntMap<>();

	private static class Cache{
		float stamp = -1f;
		Unit head;
		final Seq<Unit> sections = new Seq<>();
	}

	private static Cache cache(Team team){
		Cache cache = caches.get(team.id);
		if(cache == null) caches.put(team.id, cache = new Cache());
		if(cache.stamp != Time.time){
			cache.stamp = Time.time;
			cache.head = null;
			cache.sections.clear();
			for(int i = 0; i < Groups.unit.size(); i++){
				Unit unit = Groups.unit.index(i);
				if(unit.team != team || unit.dead || !unit.isAdded()) continue;
				//Only one head counts: the oldest.
				if(unit.type == head){
					if(cache.head == null || unit.id < cache.head.id) cache.head = unit;
				}else if(unit.type == body){
					cache.sections.add(unit);
				}
			}
			cache.sections.sort(unit -> unit.id);
		}
		return cache;
	}

	/** Forgets what was worked out this tick, after a change. */
	public static void refresh(){
		for(Cache cache : caches.values()) cache.stamp = -1f;
	}

	/** The head of the team's Serpent, or null. */
	public static Unit head(Team team){
		return cache(team).head;
	}

	/** How many body sections the team has. */
	public static int sections(Team team){
		return cache(team).sections.size;
	}

	/** The last part of the Serpent: its last section, or the head if it has none. Null with no head. */
	public static Unit tail(Team team){
		Cache cache = cache(team);
		return cache.sections.isEmpty() ? cache.head : cache.sections.peek();
	}

	/** The part in front of this section: the section made before it, or the head. */
	public static Unit predecessor(Unit section){
		Cache cache = cache(section.team);
		int index = cache.sections.indexOf(section, true);
		if(index < 0) return null;
		return index == 0 ? cache.head : cache.sections.get(index - 1);
	}

	/** Whether this is a head that nothing can hurt now, because the Serpent still has sections. */
	public static boolean guarded(Unit unit){
		return unit.type == head && head(unit.team) == unit && sections(unit.team) > 0;
	}

	/** The size of the head and of a section, as drawn: the width across, and the length along the way they face, in world units (a sprite is drawn at 4 pixels to a unit). */
	public static final float headWidth = 40f, headLength = 28f, bodyWidth = 32f, bodyLength = 20f;

	public static float lengthOf(Unit unit){
		return unit.type == head ? headLength : bodyLength;
	}

	/** How far behind its predecessor a section keeps: the ovals just touch, a little overlapping. */
	public static float spacing(Unit section, Unit predecessor){
		return 0.9f * (lengthOf(section) + lengthOf(predecessor)) / 2f;
	}
}
