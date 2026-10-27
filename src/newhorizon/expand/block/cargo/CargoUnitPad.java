package newhorizon.expand.block.cargo;

import arc.Core;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.blocks.units.UnitFactory;
import newhorizon.expand.hiemara.CargoCap;

/** Builds cargo units, as long as the cargo cap has room. */
public class CargoUnitPad extends UnitFactory{
	public CargoUnitPad(String name){
		super(name);
		
		placeableLiquid = true;
		commandable = false;
	}
	
	@Override
	public void setBars(){
		super.setBars();
		
		removeBar("units");
		addBar("cargocap", (CargoUnitPadBuild build) -> new Bar(
			() -> Core.bundle.format("bar.cargocap", CargoCap.count(build.team), CargoCap.capString(build.team)),
			() -> Pal.power,
			() -> {
				int cap = CargoCap.cap(build.team);
				return cap == Integer.MAX_VALUE || cap <= 0 ? 0f : Math.min(1f, (float)CargoCap.count(build.team) / cap);
			}
		));
	}
	
	public class CargoUnitPadBuild extends UnitFactoryBuild{
		@Override
		public boolean shouldConsume(){
			return super.shouldConsume() && CargoCap.canCreate(team);
		}
	}
}
