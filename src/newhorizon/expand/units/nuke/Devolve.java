package newhorizon.expand.units.nuke;

import arc.math.Mathf;
import arc.struct.ObjectMap;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.abilities.Ability;
import mindustry.entities.units.UnitController;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.blocks.ConstructBlock;
import mindustry.world.blocks.defense.MendProjector;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.distribution.Conveyor;
import mindustry.world.blocks.distribution.Duct;
import mindustry.world.blocks.distribution.StackConveyor;
import mindustry.world.blocks.power.Battery;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.units.Reconstructor;
import newhorizon.content.nuke.NukeUnits;

/**
 * What a devolve nuke does: units drop one tier (back to the unit a reconstructor makes them from) and structures fall back to the
 * basic version of their kind: walls (of any size) to one-tile copper walls, conveyors to basic ones, turrets to duos, menders and batteries to their one-tile
 * versions, and every power generator to a combustion generator.
 */
public class Devolve{
	private static ObjectMap<UnitType, UnitType> previous;

	/** Which unit each unit is made from: what a reconstructor turns into it. Worked out once. */
	public static UnitType previous(UnitType type){
		if(previous == null){
			previous = new ObjectMap<>();
			for(Block block : Vars.content.blocks()){
				if(!(block instanceof Reconstructor)) continue;
				for(UnitType[] pair : ((Reconstructor)block).upgrades){
					if(pair.length >= 2 && !previous.containsKey(pair[1])) previous.put(pair[1], pair[0]);
				}
			}
		}
		return previous.get(type);
	}

	/** Forgets the table, for when the content changes. */
	public static void reset(){
		previous = null;
	}

	/** Whether this unit can drop a tier: it has a previous one, and it is not a nuke. */
	public static boolean canDevolve(Unit unit){
		return unit != null && !unit.dead() && unit.isAdded() && !NukeUnits.isNuke(unit.type) && previous(unit.type) != null;
	}

	/** Drops the unit one tier. Server side. Returns false if it had none to drop to. */
	public static boolean unit(Unit unit){
		if(!canDevolve(unit)) return false;
		UnitType to = previous(unit.type);
		float fraction = Mathf.clamp(unit.health / Math.max(unit.maxHealth, 1f));

		boolean sameKind = to.constructor.get().getClass() == unit.getClass();
		if(sameKind){
			//The same kind of entity: it simply changes what it is, and keeps whoever controls it.
			UnitController keep = unit.controller();
			unit.setType(to);
			if(unit.controller() != keep) unit.controller(keep);
			Ability[] abilities = new Ability[to.abilities.size];
			for(int i = 0; i < abilities.length; i++) abilities[i] = to.abilities.get(i).copy();
			unit.abilities = abilities;
			unit.setupWeapons(to);
			unit.health = unit.maxHealth * fraction;
			return true;
		}

		//A different kind (a walker that came from a crawler, say): the unit is replaced by a new one of the lower tier in the same place.
		Unit now = to.create(unit.team);
		now.set(unit.x, unit.y);
		now.rotation = unit.rotation;
		now.vel.set(unit.vel);
		now.health = now.maxHealth * fraction;
		Player player = unit.getPlayer();
		now.controller(to.createController(now));
		unit.remove();
		now.add();
		if(player != null) player.unit(now);
		return true;
	}

	/** What a structure falls back to, or null if it is already as basic as it gets (or is not one of the kinds that fall). */
	public static Block target(Block block){
		if(block instanceof CoreBlock || block instanceof ConstructBlock) return null;
		Block to = null;
		if(block instanceof Wall) to = Blocks.copperWall;
		else if(block instanceof Turret) to = Blocks.duo;
		else if(block instanceof Conveyor || block instanceof StackConveyor) to = Blocks.conveyor;
		else if(block instanceof Duct) to = Blocks.duct;
		else if(block instanceof MendProjector) to = Blocks.mender;
		else if(block instanceof Battery) to = Blocks.battery;
		else if(block instanceof PowerGenerator) to = Blocks.combustionGenerator;
		return to == block ? null : to;
	}

	/** Drops a structure to its basic version, on every tile it covered. Server side. Returns false if it was not one that falls. */
	public static boolean block(mindustry.gen.Building build){
		Block to = target(build.block);
		if(to == null || !BlockMover.movable(build)) return false;
		return BlockMover.replace(build, to) != null;
	}
}
