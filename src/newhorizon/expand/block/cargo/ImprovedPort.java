package newhorizon.expand.block.cargo;

import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.blocks.storage.StorageBlock;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

import static mindustry.Vars.tilesize;

/**
 * A port that holds a lot, and every second collects what the blocks around it have made, so nothing has to carry
 * their output to it. Bigger tiers hold more and reach further.
 */
public class ImprovedPort extends Port{
	/** Reach in tiles. */
	public float range = 8f;
	/** Most of one item taken from one block in one second. */
	public int collectPerItem = 40;
	
	protected final int timerCollect = timers++;
	
	public ImprovedPort(String name){
		super(name);
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
	
	/** @return whether the output of a block is taken. Storage, the core and the logistics blocks are left alone. */
	public static boolean collectable(Building build){
		return build.items != null && build.block.unloadable && !(build.block instanceof StorageBlock)
			&& !(build instanceof HubBlock.HubBuild) && !(build instanceof Depot.DepotBuild) && !(build instanceof Distributor.DistributorBuild);
	}
	
	/** Takes the outputs of the blocks around a building into it, as far as it has room. */
	public static void collectAround(Building hub, float range, int perItem){
		Vars.indexer.eachBlock(hub.team, hub.x, hub.y, range * tilesize, b -> b != hub && collectable(b), from -> pull(hub, from, perItem));
	}
	
	/** Takes the output of a block, which is what it holds and does not use itself. */
	public static void pull(Building hub, Building from, int perItem){
		for(Item item : Vars.content.items()){
			int amount = from.items.get(item);
			if(amount <= 0 || from.block.consumesItem(item))continue;
			
			int space = hub.block.itemCapacity - hub.items.total();
			if(space <= 0)return;
			
			int taken = from.removeStack(item, Math.min(Math.min(amount, space), perItem));
			if(taken > 0)hub.items.add(item, taken);
		}
	}
	
	public class ImprovedPortBuild extends PortBuild{
		@Override
		public void updateTile(){
			super.updateTile();
			
			if(!Vars.net.client() && timer(timerCollect, 60f))collect();
		}
		
		protected void collect(){
			collectAround(this, range, collectPerItem);
		}
		
		@Override
		public void drawSelect(){
			mindustry.graphics.Drawf.dashCircle(x, y, range * tilesize, team.color);
		}
	}
}
