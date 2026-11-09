package newhorizon.expand.units.siphoner;

import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.power.PowerGraph;
import mindustry.world.consumers.ConsumePower;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.modules.PowerModule;

/**
 * What the E-Siphoner's power drain is made of: a hidden consumer that is put into the enemy's power graph and asks it for
 * {@link LeechBuild#demand} power a tick. It is a real consumer of the graph, so the graph treats it like any other: it shares what
 * there is with the rest (batteries included), and what it is <b>actually given</b> is {@code power.status * demand}. That given amount is
 * the energy successfully siphoned. It is not a block of the world: it has no tile, cannot be built or seen, and exists only inside a graph.
 */
public class PowerLeech extends Block{
	/** The one block all leeches are made from (created once, with the rest of the content). */
	public static PowerLeech block;

	public PowerLeech(String name){
		super(name);
		update = true;
		hasPower = true;
		consumesPower = true;
		outputsPower = false;
		destructible = false;
		hideDatabase = true;
		buildVisibility = BuildVisibility.hidden;
		inEditor = false;
		buildType = LeechBuild::new;
		consume(new ConsumePower(0f, 0f, false){
			@Override
			public float requestedPower(Building entity){
				return entity instanceof LeechBuild ? ((LeechBuild)entity).requested() : 0f;
			}
		});
	}

	public static void make(){
		block = new PowerLeech("power-leech");
	}

	/** A new leech that belongs to the unit (it stops asking for power as soon as the unit is gone). */
	public static LeechBuild create(Unit owner){
		LeechBuild b = (LeechBuild)block.newBuilding();
		b.block = block;
		b.team = Team.derelict;
		//Anything that walks a graph's consumers (its debug text, the power node overlay) expects a tile; this one is off the map.
		b.tile = new Tile(-1, -1);
		b.power = new PowerModule();
		b.owner = owner;
		return b;
	}

	/** Puts the leech into the graph (taking it out of the one it was in). */
	public static void attach(LeechBuild leech, PowerGraph graph){
		if(leech.power.graph != null && leech.power.graph != graph) leech.power.graph.consumers.remove(leech);
		leech.power.graph = graph;
		if(graph != null && !graph.consumers.contains(leech, true)) graph.consumers.add(leech);
	}

	/** Takes the leech out of its graph, and stops it asking. */
	public static void detach(LeechBuild leech){
		leech.demand = 0f;
		if(leech.power.graph != null) leech.power.graph.consumers.remove(leech);
		leech.power.graph = null;
		leech.power.status = 0f;
	}

	public class LeechBuild extends Building{
		/** Power asked for a tick. */
		public float demand;
		public Unit owner;

		/** What it asks for: nothing once the unit that owns it is dead or gone. */
		public float requested(){
			return owner != null && owner.isValid() ? demand : 0f;
		}

		/** The power it is given a tick. */
		public float given(){
			return power.status * requested();
		}
	}
}
