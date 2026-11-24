package newhorizon.content.supply;

import arc.struct.Seq;
import mindustry.Vars;
import mindustry.ai.ItemUnitStance;
import mindustry.ai.UnitCommand;
import mindustry.ai.UnitStance;
import mindustry.ai.types.CommandAI;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.type.UnitType;
import newhorizon.expand.units.ai.FlyingMinerAI;
import newhorizon.expand.units.ai.GroundMinerAI;

/**
 * The Mine command of the mod's miners: the highest tier first, nothing the core is full of ({@link newhorizon.expand.units.ai.MinerPriority}).
 * It flies a flying unit ({@link FlyingMinerAI}) and walks a walking one ({@link GroundMinerAI}). (The id keeps its first name, so that saved
 * units still find it.)
 */
public class MinerCommands{
	public static final UnitCommand mine = new UnitCommand("nh-ground-mine", "production", u -> u.type.flying ? new FlyingMinerAI() : new GroundMinerAI());

	/**
	 * The base game offers the "mine anything" and item buttons only for its own Mine command (by identity), so a unit with this command gets
	 * them from its type: call this from {@code getUnitStances}. Same list as the base game's: the items it can mine that have ore on the map.
	 */
	public static void addStances(UnitType type, Unit unit, Seq<UnitStance> out){
		if(!(unit.controller() instanceof CommandAI ai) || ai.currentCommand() != mine) return;
		out.add(UnitStance.mineAuto);
		for(Item item : Vars.indexer.getAllPresentOres()){
			if(!unit.canMine(item)) continue;
			if(!((type.mineFloor && Vars.indexer.hasOre(item)) || (type.mineWalls && Vars.indexer.hasWallOre(item)))) continue;
			ItemUnitStance s = ItemUnitStance.getByItem(item);
			if(s != null) out.add(s);
		}
	}
}
