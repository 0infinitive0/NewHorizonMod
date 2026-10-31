package newhorizon.expand.block.cargo;

import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Teamc;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.blocks.storage.StorageBlock;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

import static mindustry.Vars.tilesize;

/**
 * Holds a lot of items, up to its capacity of each kind, and every second fills the inputs of the factories, turrets and generators around it.
 * Cargo units do not unload here: fill it from a depot or any block next to it.
 */
public class Distributor extends Block{
	/** Reach in tiles. */
	public float range = 8f;
	/** Most of one item given to one block in one second. */
	public int perSecond = 20;
	
	protected final int timerFill = timers++;
	
	public Distributor(String name){
		super(name);
		
		update = true;
		solid = true;
		hasItems = true;
		placeableLiquid = true;
		itemCapacity = 1000;
	}
	
	@Override
	public void setStats(){
		super.setStats();
		
		stats.add(Stat.range, range, StatUnit.blocks);
		stats.add(Stat.itemCapacity, itemCapacity, StatUnit.items);
	}
	
	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		super.drawPlace(x, y, rotation, valid);
		
		mindustry.graphics.Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range * tilesize, mindustry.graphics.Pal.accent);
	}
	
	public class DistributorBuild extends Building{
		@Override
		public boolean acceptItem(Building source, Item item){
			//The capacity is for each item, so a lot of one item can not keep the others out.
			return items.get(item) < itemCapacity && !(source instanceof DistributorBuild) && !(source instanceof HubBlock.HubBuild);
		}
		
		@Override
		public int acceptStack(Item item, int amount, Teamc source){
			return 0;
		}
		
		@Override
		public int getMaximumAccepted(Item item){
			return itemCapacity;
		}
		
		@Override
		public void updateTile(){
			if(!Vars.net.client() && timer(timerFill, 60f) && items.total() > 0)fill();
		}
		
		protected void fill(){
			Vars.indexer.eachBlock(team, x, y, range * tilesize, b -> b != this && b.items != null && fillable(b), this::feed);
		}
		
		protected boolean fillable(Building build){
			return !(build.block instanceof StorageBlock) && !(build instanceof HubBlock.HubBuild)
				&& !(build instanceof Depot.DepotBuild) && !(build instanceof DistributorBuild);
		}
		
		/** Gives a block what it takes of the items held. */
		protected void feed(Building to){
			for(Item item : Vars.content.items()){
				for(int i = 0; i < perSecond && items.get(item) > 0 && to.acceptItem(this, item); i++){
					to.handleItem(this, item);
					items.remove(item, 1);
				}
			}
		}
		
		@Override
		public void drawSelect(){
			mindustry.graphics.Drawf.dashCircle(x, y, range * tilesize, team.color);
		}
	}
}
