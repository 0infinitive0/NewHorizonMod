package newhorizon.expand.status;

import arc.Events;
import arc.math.Angles;
import arc.struct.IntFloatMap;
import mindustry.game.EventType;
import mindustry.gen.Unit;

/**
 * Makes a unit turn more slowly while a status effect is on it. The game has no status field for turning speed (a unit turns by setting
 * its rotation toward where it wants to face, at its type's rotate speed), so the effect looks at how far the unit turned since the last time
 * it was asked and puts back a part of it: with a factor of 0.3 the unit ends up turning at about a third of its speed.
 */
public class TurnLimit{
	/** The rotation each unit had the last time it was limited. */
	private static final IntFloatMap last = new IntFloatMap();
	private static boolean hooked;

	public static void hook(){
		if(hooked) return;
		hooked = true;
		Events.on(EventType.WorldLoadEvent.class, e -> last.clear());
		Events.on(EventType.ResetEvent.class, e -> last.clear());
	}

	/** Call once a tick for a unit that has the effect; {@code factor} is the part of its turning that is kept (1 changes nothing, 0 stops it turning). */
	public static void limit(Unit unit, float factor){
		float rotation = unit.rotation;
		if(last.containsKey(unit.id)){
			float before = last.get(unit.id, rotation);
			float turned = Angles.angleDist(before, rotation);
			if(turned > 0.01f){
				unit.rotation = Angles.moveToward(before, rotation, turned * factor);
			}
		}
		last.put(unit.id, unit.rotation);
	}

	/** Forget a unit (the effect wore off or it died). */
	public static void forget(Unit unit){
		last.remove(unit.id, 0f);
	}
}
