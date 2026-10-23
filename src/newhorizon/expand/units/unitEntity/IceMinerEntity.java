package newhorizon.expand.units.unitEntity;

import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.gen.UnitEntity;
import mindustry.type.Item;
import mindustry.world.Tile;
import newhorizon.content.NHItems;
import newhorizon.expand.block.env.IceFloor;
import newhorizon.expand.entities.EntityRegister;

/** Drone that mines ice floors. Every mined tile melts back to water after a few mining cycles. */
public class IceMinerEntity extends UnitEntity{
	/** Mining cycles (one item each) until the mined tile turns into water. */
	public static int cyclesPerTile = 3;
	
	protected transient Tile cycleTile;
	protected transient int cycles;
	
	@Override public int classId(){return EntityRegister.getID(IceMinerEntity.class);}
	
	@Override
	public Item getMineResult(Tile tile){
		if(tile != null && tile.block() == Blocks.air && tile.floor() instanceof IceFloor && canMine(NHItems.ice)){
			return NHItems.ice;
		}
		return null;
	}
	
	@Override
	public void update(){
		Tile tile = mineTile;
		float timer = mineTimer;
		
		super.update();
		
		//The mine timer is only reset to 0 while the tile is kept after a finished cycle.
		if(!Vars.net.client() && tile != null && timer > 0f && mineTimer == 0f && mineTile == tile){
			if(cycleTile != tile){
				cycleTile = tile;
				cycles = 0;
			}
			
			if(++cycles >= cyclesPerTile){
				cycles = 0;
				cycleTile = null;
				mineTile = null;
				
				if(tile.floor() instanceof IceFloor){
					((IceFloor)tile.floor()).melt(tile);
				}
			}
		}
	}
}
