package newhorizon.expand.units.nuke;

import arc.Core;
import arc.Events;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Reflect;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.game.Teams;
import mindustry.gen.Building;

/**
 * Structures a nuke put down (the walls of a wall nuke, the mines of a cluster nuke) are left alone by the things that look after a
 * team's structures:
 * <ul>
 * <li>builder units do not rebuild them when they are destroyed (the ghost plan the game makes for a destroyed block is struck out);</li>
 * <li>repair units do not go looking for them when they are damaged (they are kept out of the list of damaged structures the repair AI picks
 * from). Mend projectors and the like still heal them: most walls cannot be told to refuse healing.</li>
 * </ul>
 * Which structures these are is remembered for the session only: after a save is loaded they are ordinary structures again.
 */
public class NukeSpawned{
	private static final ObjectSet<Building> claimed = new ObjectSet<>();
	private static boolean hooked;
	private static float purgeTimer;

	/** Marks a structure a nuke has just put down. Server side. */
	public static void claim(Building build){
		if(build == null) return;
		hook();
		claimed.add(build);
		hideFromRepair();
	}

	public static boolean claimed(Building build){
		return build != null && claimed.contains(build);
	}

	private static void hook(){
		if(hooked) return;
		hooked = true;

		//The game queues a destroyed block to be rebuilt; the nuke's own are taken off the queue again. Done a moment later so that it
		//does not matter which of the two handlers runs first.
		Events.on(EventType.BlockDestroyEvent.class, e -> {
			Building build = e.tile == null ? null : e.tile.build;
			if(build == null || !claimed.contains(build)) return;
			Team team = build.team;
			short x = build.tile.x, y = build.tile.y;
			claimed.remove(build);
			Core.app.post(() -> forget(team, x, y));
		});
		Events.on(EventType.WorldLoadEvent.class, e -> claimed.clear());
		Events.run(EventType.Trigger.update, () -> {
			purgeTimer += Time.delta;
			if(purgeTimer < 60f * 5f) return;
			purgeTimer = 0f;
			Seq<Building> gone = new Seq<>();
			for(Building b : claimed) if(!b.isValid()) gone.add(b);
			for(Building b : gone) claimed.remove(b);
		});
	}

	/** Strikes the rebuild plans at a position out. */
	public static void forget(Team team, int x, int y){
		for(Teams.BlockPlan plan : team.data().plans){
			if(plan.x == x && plan.y == y) plan.removed = true;
		}
	}

	/** The list of damaged structures a team's repair units choose from, in which nuke-made ones do not get a place. */
	private static class Hidden extends Seq<Building>{
		Hidden(){
			super(false);
		}

		@Override
		public Seq<Building> add(Building value){
			return claimed.contains(value) ? this : super.add(value);
		}
	}

	/** Makes sure every team's list of damaged structures keeps the claimed ones out. Safe to call as often as needed. */
	public static void hideFromRepair(){
		if(Vars.indexer == null) return;
		try{
			Seq<Building>[] lists = Reflect.get(Vars.indexer, "damagedTiles");
			if(lists == null) return;
			for(int i = 0; i < lists.length; i++){
				if(lists[i] instanceof Hidden) continue;
				Hidden hidden = new Hidden();
				if(lists[i] != null) for(Building b : lists[i]) hidden.add(b);
				lists[i] = hidden;
			}
		}catch(Throwable t){
			//The indexer is not what this expects (a different game version): the structures are simply repaired as usual.
		}
	}
}
