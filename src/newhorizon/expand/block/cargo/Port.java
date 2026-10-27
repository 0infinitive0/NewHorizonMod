package newhorizon.expand.block.cargo;

import mindustry.gen.Building;
import mindustry.gen.Teamc;
import mindustry.type.Item;
import mindustry.type.UnitType;
import newhorizon.expand.hiemara.CargoCap;
import newhorizon.expand.units.hiemara.CargoUnitType;

/**
 * Where cargo units pick up what they carry. It holds any mix of items. A unit is bound to one port at a time and
 * only ever takes items from it.
 */
public class Port extends HubBlock{
	public Port(String name){
		super(name);
		
		hasItems = true;
		itemCapacity = 300;
	}
	
	@Override
	public boolean accepts(UnitType type){
		return CargoCap.isCargo(type) && type instanceof CargoUnitType;
	}
	
	public class PortBuild extends HubBuild{
		@Override
		public boolean acceptItem(Building source, Item item){
			return items.total() < itemCapacity && !(source instanceof Depot.DepotBuild) && !(source instanceof HubBuild);
		}
		
		@Override
		public int acceptStack(Item item, int amount, Teamc source){
			return 0;
		}
		
		@Override
		public int getMaximumAccepted(Item item){
			return itemCapacity;
		}
	}
}
