package newhorizon.expand.units.serpent;

import arc.Events;
import arc.math.Mathf;
import arc.struct.IntFloatMap;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.abilities.Ability;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Unit;

/**
 * Shares out the damage a Serpent's body takes while its head has Spread damage on. A unit gives nothing to hook into when it is hit, so
 * this watches health: once a tick, for each body section, the damage it took since the last look is what its health fell by (plus what
 * it healed meanwhile). The total taken by the whole body is then shared out equally, so every section ends with its old health less its
 * share. A section that a single hit killed outright stays dead: that is not spread. The head takes no part.
 */
public class SerpentSpreadSystem{
	private static boolean hooked;
	/** What each section's health was when last looked at, and when. */
	private static final IntFloatMap last = new IntFloatMap(), stamp = new IntFloatMap();

	public static void hook(){
		if(hooked) return;
		hooked = true;
		Events.run(EventType.Trigger.update, SerpentSpreadSystem::update);
		Events.on(EventType.ResetEvent.class, e -> clear());
		Events.on(EventType.WorldLoadEvent.class, e -> clear());
	}

	private static void clear(){
		last.clear();
		stamp.clear();
	}

	private static void update(){
		if(Vars.net.client()) return;
		for(int i = 0; i < Groups.unit.size(); i++){
			Unit head = Groups.unit.index(i);
			if(head.type != SerpentChain.head || head.dead || SerpentChain.head(head.team) != head) continue;
			if(!SerpentSpreadAbility.spreading(head)) continue;
			share(SerpentChain.sectionList(head.team));
		}
	}

	private static float healed(Unit section){
		for(Ability ability : section.abilities) if(ability instanceof PassiveHealAbility) return ((PassiveHealAbility)ability).lastHeal;
		return 0f;
	}

	/** One look at a team's sections. */
	private static void share(Seq<Unit> sections){
		float total = 0f;
		int alive = 0;
		for(Unit section : sections){
			if(section.dead) continue;
			alive++;
			float seen = stamp.get(section.id, -1000f);
			//Only counts if it was looked at on the last tick: otherwise there is nothing to compare with.
			if(Time.time - seen > 3f || !last.containsKey(section.id)) continue;
			total += Math.max(0f, last.get(section.id, section.health) + healed(section) - section.health);
		}

		if(total > 0f && alive > 0){
			float each = total / alive;
			for(Unit section : sections){
				if(section.dead) continue;
				float seen = stamp.get(section.id, -1000f);
				float before = Time.time - seen > 3f || !last.containsKey(section.id) ? section.health : last.get(section.id, section.health) + healed(section);
				float now = Math.min(section.maxHealth, before - each);
				if(now <= 0f){
					section.health = 0f;
					section.kill();
				}else{
					section.health = now;
				}
			}
		}

		for(Unit section : sections){
			if(section.dead) continue;
			last.put(section.id, section.health);
			stamp.put(section.id, Time.time);
		}
	}
}
