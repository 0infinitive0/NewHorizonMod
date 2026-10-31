package newhorizon.expand.block.cargo;

import arc.Core;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.meta.Stat;
import newhorizon.expand.hiemara.CargoCap;

/** A block that raises the cap of cargo units. */
public class CargoCapBlock extends Block{
	public int cargoCap = 6;
	
	public CargoCapBlock(String name){
		super(name);
		
		//A block that does not update is not in the list of the buildings of its type of the team, and the cap is counted from that list.
		update = true;
		solid = true;
		placeableLiquid = true;
		
		CargoCap.capBlocks.add(this);
	}
	
	@Override
	public void setBars(){
		super.setBars();
		
		addBar("cargocap", (Building build) -> new Bar(
			() -> Core.bundle.format("bar.cargocap", CargoCap.count(build.team), CargoCap.capString(build.team)),
			() -> Pal.power,
			() -> {
				int cap = CargoCap.cap(build.team);
				return cap == Integer.MAX_VALUE || cap <= 0 ? 0f : Math.min(1f, (float)CargoCap.count(build.team) / cap);
			}
		));
	}
	
	@Override
	public void setStats(){
		super.setStats();
		
		stats.add(Stat.maxUnits, cargoCap);
	}
}
