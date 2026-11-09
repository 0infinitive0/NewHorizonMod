package newhorizon.expand.units.siphoner;

import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.power.PowerGraph;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.modules.PowerModule;

/**
 * What the E-Siphoner's power drain is made of: a hidden <b>producer</b> with a negative output, put into the enemy's power graph. The graph
 * adds up what its producers make before it shares the power among its consumers, so what the leech takes off comes off the supply first: the
 * enemy's buildings get only what is left, however busy the grid is. It is not a block of the world: it has no real tile, cannot be built or
 * seen, and exists only inside a graph.
 */
public class PowerLeech extends Block{
	/** The one block all leeches are made from (created once, with the rest of the content). */
	public static PowerLeech block;

	public PowerLeech(String name){
		super(name);
		update = true;
		hasPower = true;
		consumesPower = false;
		outputsPower = true;
		destructible = false;
		hideDatabase = true;
		buildVisibility = BuildVisibility.hidden;
		inEditor = false;
		buildType = LeechBuild::new;
	}

	public static void make(){
		block = new PowerLeech("power-leech");
	}

	/** A new leech that belongs to the unit (it stops taking power as soon as the unit is gone). */
	public static LeechBuild create(Unit owner){
		LeechBuild b = (LeechBuild)block.newBuilding();
		b.block = block;
		b.team = Team.derelict;
		//Anything that walks a graph's lists (its debug text, the power node overlay) expects a tile; this one is off the map.
		b.tile = new Tile(-1, -1);
		b.power = new PowerModule();
		b.owner = owner;
		return b;
	}

	/** Puts the leech into the graph's producers (taking it out of the graph it was in). */
	public static void attach(LeechBuild leech, PowerGraph graph){
		if(leech.power.graph != null && leech.power.graph != graph) leech.power.graph.producers.remove(leech);
		leech.power.graph = graph;
		if(graph != null && !graph.producers.contains(leech, true)) graph.producers.add(leech);
	}

	/** Takes the leech out of its graph, and stops it taking. */
	public static void detach(LeechBuild leech){
		leech.steal = 0f;
		if(leech.power.graph != null) leech.power.graph.producers.remove(leech);
		leech.power.graph = null;
	}

	public class LeechBuild extends Building{
		/** Power taken off the supply a tick. */
		public float steal;
		public Unit owner;

		/** What it wants to take a tick: nothing once the unit that owns it is dead or gone. */
		public float wanted(){
			return owner != null && owner.isValid() ? Math.max(0f, steal) : 0f;
		}

		/** What the graph's real producers make a tick (every leech left out). */
		public float othersProduction(){
			if(power == null || power.graph == null) return 0f;
			float sum = 0f;
			for(Building p : power.graph.producers){
				if(!(p instanceof LeechBuild)) sum += p.getPowerProduction();
			}
			return sum;
		}

		/**
		 * Negative: what it takes. Never more than the real producers make (the supply never goes below zero), and when several leeches are
		 * in one graph they share what there is in proportion to what each wants.
		 */
		@Override
		public float getPowerProduction(){
			float mine = wanted();
			if(mine <= 0f || power == null || power.graph == null) return 0f;
			float real = 0f, all = 0f;
			for(Building p : power.graph.producers){
				if(p instanceof LeechBuild) all += ((LeechBuild)p).wanted();
				else real += p.getPowerProduction();
			}
			real = Math.max(0f, real);
			return all <= 0f ? 0f : -mine * Math.min(1f, real / all);
		}
	}
}
