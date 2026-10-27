package newhorizon.expand.block.cargo;

import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.type.UnitType;
import newhorizon.expand.hiemara.CargoCap;
import newhorizon.expand.units.hiemara.TankerUnitType;

/** The port of the tankers. It holds one liquid at a time and fills from the blocks next to it. */
public class LiquidPort extends HubBlock{
	public LiquidPort(String name){
		super(name);
		
		hasLiquids = true;
		liquidCapacity = 1000f;
	}
	
	@Override
	public boolean accepts(UnitType type){
		return CargoCap.isCargo(type) && type instanceof TankerUnitType;
	}
	
	public class LiquidPortBuild extends HubBuild{
		@Override
		public boolean acceptLiquid(Building source, Liquid liquid){
			if(source instanceof HubBuild || source instanceof LiquidDepot.LiquidDepotBuild)return false;
			return (liquids.currentAmount() < 0.01f || liquids.current() == liquid) && liquids.get(liquid) < liquidCapacity - 0.01f;
		}
	}
}
