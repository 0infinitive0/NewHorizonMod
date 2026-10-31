package newhorizon.expand.units.hiemara;

import mindustry.type.UnitType;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;
import mindustry.world.meta.StatUnit;

/**
 * A unit that hauls items between a port and the depots. Most carry one kind of item; the big ships carry
 * {@link #holdTypes} kinds at once, {@link #itemCapacity} of each.
 */
public class CargoUnitType extends UnitType{
	/** How many different items the unit carries at the same time. */
	public int holdTypes = 1;
	/** The tier in its line, for display. */
	public int cargoTier = 1;
	
	/** How far from a port or depot, on top of the size of the ship and the block, the ship loads and unloads, in world units. */
	public float pickupRange = 14f;
	
	public static final Stat pickupStat = new Stat("nh-pickup-range", StatCat.function);
	
	public CargoUnitType(String name){
		super(name);
	}
	
	@Override
	public void setStats(){
		super.setStats();
		
		stats.add(pickupStat, (4f + pickupRange + hitSize * 0.75f) / 8f, StatUnit.blocks);
	}
	
	public boolean multi(){
		return holdTypes > 1;
	}
}
