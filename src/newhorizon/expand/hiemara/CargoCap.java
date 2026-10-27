package newhorizon.expand.hiemara;

import arc.Events;
import arc.struct.ObjectIntMap;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;
import newhorizon.expand.block.cargo.CargoCapBlock;

/**
 * The unit cap of cargo units. It is separate from the normal unit cap: cargo unit types do not use the normal cap,
 * and nothing but cargo units counts towards this one. Cores and {@link CargoCapBlock}s raise it.
 */
public class CargoCap{
	/** Every cargo unit type. They must have useUnitCap off. */
	public static final Seq<UnitType> cargoTypes = new Seq<>();
	/** Every block that raises the cap, filled by their constructors. */
	public static final Seq<CargoCapBlock> capBlocks = new Seq<>();
	/** Cap given by a core, by block. Cores that are not in here give half of their normal unit cap bonus, at least 2. */
	public static final ObjectIntMap<Block> coreCaps = new ObjectIntMap<>();
	
	protected static float timer;
	
	public static void load(){
		Events.run(EventType.Trigger.update, () -> {
			if(Vars.net.client() || !Vars.state.isGame())return;
			
			timer += Time.delta;
			if(timer < 60f)return;
			timer = 0f;
			
			for(var data : Vars.state.teams.getActive()){
				enforce(data.team);
			}
		});
	}
	
	public static boolean isCargo(UnitType type){
		return cargoTypes.contains(type, true);
	}
	
	public static int coreCap(CoreBlock core){
		return coreCaps.get(core, Math.max(2, core.unitCapModifier / 2));
	}
	
	public static int cap(Team team){
		if(Vars.state.rules.disableUnitCap || team.ignoreUnitCap)return Integer.MAX_VALUE;
		
		int cap = 0;
		for(var core : team.data().cores){
			cap += coreCap((CoreBlock)core.block);
		}
		for(CargoCapBlock block : capBlocks){
			cap += team.data().getBuildings(block).size * block.cargoCap;
		}
		return cap;
	}
	
	public static int count(Team team){
		int count = 0;
		for(UnitType type : cargoTypes){
			count += team.data().countType(type);
		}
		return count;
	}
	
	public static boolean canCreate(Team team){
		return count(team) < cap(team);
	}
	
	public static String capString(Team team){
		int cap = cap(team);
		return cap == Integer.MAX_VALUE ? "∞" : String.valueOf(cap);
	}
	
	/** Kills the newest cargo units above the cap. */
	public static void enforce(Team team){
		int excess = count(team) - cap(team);
		if(excess <= 0)return;
		
		Seq<Unit> units = new Seq<>();
		for(Unit unit : team.data().units){
			if(isCargo(unit.type))units.add(unit);
		}
		units.sort(u -> -u.id);
		
		for(int i = 0; i < excess && i < units.size; i++){
			Call.unitCapDeath(units.get(i));
		}
	}
}
