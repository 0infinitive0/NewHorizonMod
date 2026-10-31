package newhorizon.expand.units.hiemara;

import mindustry.type.UnitType;
import mindustry.world.meta.StatUnit;

/** A cargo ship that carries a single liquid. */
public class TankerUnitType extends UnitType{
	/** How much liquid the ship holds. */
	public float liquidCapacity = 300f;
	public int cargoTier = 1;
	
	/** How far from a port or depot, on top of the size of the ship and the block, the ship loads and unloads, in world units. */
	public float pickupRange = 14f;
	
	public TankerUnitType(String name){
		super(name);
	}
	
	@Override
	public void setStats(){
		super.setStats();
		
		stats.add(CargoUnitType.pickupStat, (4f + pickupRange + hitSize * 0.75f) / 8f, StatUnit.blocks);
	}
}
