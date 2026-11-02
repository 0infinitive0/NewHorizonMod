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
	
	/** @return whether the inputs of a block are filled by a distributor or a supply hub. */
	public static boolean fillable(Building build){
		return build.items != null && !(build.block instanceof StorageBlock) && !(build instanceof HubBlock.HubBuild)
			&& !(build instanceof Depot.DepotBuild) && !(build instanceof DistributorBuild);
	}
	
	/** Fills the inputs of the blocks around a building, from what it holds. */
	public static void fillAround(Building hub, float range, int perSecond){
		Vars.indexer.eachBlock(hub.team, hub.x, hub.y, range * tilesize, b -> b != hub && fillable(b), to -> feed(hub, to, perSecond));
	}
	
	/** Gives a block what it takes of the items the hub holds. */
	public static void feed(Building hub, Building to, int perSecond){
		for(Item item : Vars.content.items()){
			for(int i = 0; i < perSecond && hub.items.get(item) > 0 && to.acceptItem(hub, item); i++){
				to.handleItem(hub, item);
				hub.items.remove(item, 1);
			}
		}
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
			fillAround(this, range, perSecond);
		}
		
		@Override
		public void drawSelect(){
			mindustry.graphics.Drawf.dashCircle(x, y, range * tilesize, team.color);
		}
	}
}
