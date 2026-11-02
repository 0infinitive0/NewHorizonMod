package newhorizon.expand.block.hiemara;

import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.world.blocks.production.GenericCrafter;

/** A crafter that can also be sped up with ozone, see {@link OzoneBoost}. */
public class OzoneCrafter extends GenericCrafter{
	public OzoneCrafter(String name){
		super(name);
	}
	
	@Override
	public void init(){
		OzoneBoost.setup(this);
		super.init();
	}
	
	@Override
	public void setStats(){
		super.setStats();
		OzoneBoost.stats(this);
	}
	
	public class OzoneCrafterBuild extends GenericCrafterBuild{
		/** Speed multiplier from the ozone of this tick. */
		public float ozone = 1f;
		
		@Override
		public boolean acceptLiquid(Building source, Liquid liquid){
			return super.acceptLiquid(source, liquid) || OzoneBoost.accepts(this, liquid);
		}
		
		@Override
		public void updateTile(){
			ozone = efficiency > 0f && shouldConsume() ? OzoneBoost.take(this, delta()) : 1f;
			super.updateTile();
		}
		
		@Override
		public float getProgressIncrease(float baseTime){
			return super.getProgressIncrease(baseTime) * ozone;
		}
	}
}
