package newhorizon.expand.block.cargo;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.blocks.ItemSelection;

/** Where tankers unload. It can be set to one liquid, and passes the liquid on to the blocks next to it. */
public class LiquidDepot extends Block{
	/** Every liquid depot block, filled by the constructor. */
	public static final Seq<LiquidDepot> all = new Seq<>();
	
	public LiquidDepot(String name){
		super(name);
		
		update = true;
		solid = true;
		hasLiquids = true;
		outputsLiquid = true;
		configurable = true;
		saveConfig = true;
		clearOnDoubleTap = true;
		placeableLiquid = true;
		liquidCapacity = 1000f;
		
		config(Liquid.class, (LiquidDepotBuild build, Liquid liquid) -> build.liquid = liquid);
		configClear((LiquidDepotBuild build) -> build.liquid = null);
		
		all.add(this);
	}
	
	public class LiquidDepotBuild extends Building{
		/** Only this liquid is accepted, any if null. */
		public Liquid liquid;
		
		/** @return whether a tanker may bring this liquid. */
		public boolean wants(Liquid liquid){
			if(this.liquid != null && this.liquid != liquid)return false;
			return (liquids.currentAmount() < 0.01f || liquids.current() == liquid) && liquids.get(liquid) < liquidCapacity - 1f;
		}
		
		@Override
		public boolean acceptLiquid(Building source, Liquid liquid){
			//Only tankers fill a depot.
			return false;
		}
		
		@Override
		public boolean canDumpLiquid(Building to, Liquid liquid){
			return !(to instanceof LiquidDepotBuild) && !(to instanceof HubBlock.HubBuild);
		}
		
		@Override
		public void updateTile(){
			if(liquids.currentAmount() > 0.01f)dumpLiquid(liquids.current());
		}
		
		@Override
		public void buildConfiguration(Table table){
			ItemSelection.buildTable(LiquidDepot.this, table, Vars.content.liquids(), () -> liquid, this::configure);
		}
		
		@Override
		public Liquid config(){
			return liquid;
		}
		
		@Override
		public void drawSelect(){
			drawItemSelection(liquid);
		}
		
		@Override
		public void write(Writes write){
			super.write(write);
			write.s(liquid == null ? -1 : liquid.id);
		}
		
		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			int id = read.s();
			liquid = id == -1 ? null : Vars.content.liquid(id);
		}
	}
}
