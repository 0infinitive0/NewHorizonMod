package newhorizon.expand.block.distribution;

import arc.util.Time;
import mindustry.gen.Building;
import mindustry.gen.Teamc;
import mindustry.type.Item;
import mindustry.world.blocks.distribution.Conveyor;
import mindustry.world.blocks.distribution.Router;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

/**
 * A router (and, at size 2, a distributor) that can pass as many items as the fast belts carry. The base router holds one item and hands it on to
 * one block, so it moves about one item a tick at best. This one keeps a <b>queue</b> of {@link #queueSize} items, each with the block it came from and
 * the tick it came in, and every tick hands on each item that can go (up to {@link #perTick}) to the next block that takes it, in turn, never back to
 * the block it came from. Into a {@link FastConveyor.FastConveyorBuild} an item goes with the distance it has been waiting, so the stream keeps its
 * spacing. As with the base router, an item does not go straight on into another router (it waits {@link #delay} ticks first), so two routers do not pass
 * items back and forth. The items are also in the block's item module, so saving is the base game's; a queue is rebuilt from it after loading.
 */
public class FastRouter extends Router{
	/** Items a second (shown in the stats), how many items it holds, how many it hands on in a tick at most, and ticks before an item may go into another router. */
	public float rate = 36f;
	public int queueSize = 4;
	public int perTick = 2;
	public float delay = 2f;

	public FastRouter(String name){
		super(name);
	}

	@Override
	public void setStats(){
		super.setStats();
		stats.add(Stat.itemsMoved, rate, StatUnit.itemsSecond);
	}

	public class FastRouterBuild extends RouterBuild{
		protected final Item[] qItem = new Item[queueSize];
		protected final Building[] qFrom = new Building[queueSize];
		protected final float[] qTime = new float[queueSize];
		protected int count;
		/** Where the next search for a block to give to starts, so that outputs take turns. */
		protected int cursor;

		@Override
		public boolean acceptItem(Building source, Item item){
			return team == source.team && count < queueSize;
		}

		@Override
		public void handleItem(Building source, Item item){
			items.add(item, 1);
			qItem[count] = item;
			qFrom[count] = source;
			qTime[count] = Time.time;
			count++;
		}

		@Override
		public int acceptStack(Item item, int amount, Teamc source){
			return 0;
		}

		@Override
		public int removeStack(Item item, int amount){
			return 0;
		}

		@Override
		public void updateTile(){
			//items that are in the module but not in the queue (after loading a save)
			if(items.total() > count){
				for(Item item : mindustry.Vars.content.items()){
					int missing = items.get(item) - countOf(item);
					for(int k = 0; k < missing && count < queueSize; k++){
						qItem[count] = item;
						qFrom[count] = null;
						qTime[count] = Time.time;
						count++;
					}
				}
			}

			int sent = 0;
			for(int i = 0; i < count && sent < perTick; ){
				if(give(i)){
					remove(i);
					sent++;
				}else{
					i++;
				}
			}
		}

		protected int countOf(Item item){
			int n = 0;
			for(int i = 0; i < count; i++) if(qItem[i] == item) n++;
			return n;
		}

		protected void remove(int i){
			items.remove(qItem[i], 1);
			for(int k = i + 1; k < count; k++){
				qItem[k - 1] = qItem[k];
				qFrom[k - 1] = qFrom[k];
				qTime[k - 1] = qTime[k];
			}
			count--;
			qItem[count] = null;
			qFrom[count] = null;
		}

		/** Tries to hand queue item {@code i} to a block, taking turns; true if it went. */
		protected boolean give(int i){
			Item item = qItem[i];
			float age = Time.time - qTime[i];
			int n = proximity.size;
			for(int k = 0; k < n; k++){
				int at = (cursor + k) % n;
				Building other = proximity.get(at);
				if(other == qFrom[i] || other.team != team) continue;
				//not at once into another router
				if(other.block instanceof Router && age < delay / timeScale) continue;

				if(other instanceof FastConveyor.FastConveyorBuild f && f.rearRoom(this, item) >= 0f){
					//as far in as the stream ahead allows, but not more than one tick of the belt's movement
					f.giveRear(this, item, Math.min(((Conveyor)f.block).speed, f.rearRoom(this, item)));
				}else if(other.acceptItem(this, item)){
					other.handleItem(this, item);
				}else{
					continue;
				}
				cursor = (at + 1) % n;
				return true;
			}
			return false;
		}
	}
}
