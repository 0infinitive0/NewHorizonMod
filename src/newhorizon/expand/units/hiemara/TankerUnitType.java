package newhorizon.expand.units.hiemara;

import mindustry.type.UnitType;

/** A cargo ship that carries a single liquid. */
public class TankerUnitType extends UnitType{
	/** How much liquid the ship holds. */
	public float liquidCapacity = 300f;
	public int cargoTier = 1;
	
	public TankerUnitType(String name){
		super(name);
	}
}
