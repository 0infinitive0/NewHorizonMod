package newhorizon.expand.block.distribution;

import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.gen.Building;
import mindustry.world.blocks.distribution.Conveyor;

/**
 * A conveyor that can be faster than the base game's belt can ever be. The base belt hands over at most one item per tick (an item enters the next tile
 * only once the one before it is 0.4 tile ahead, and it enters at the very start of the tile), so it tops out at 60 items a second whatever its speed, and
 * loses part of the distance an item went past the end of its tile at every tile.
 *
 * <p>Here a straight run of these tiles is updated as one: whichever tile of the run updates first in a tick finds its start and moves the whole run, in
 * {@code k} small steps (a step is at most 0.4 tile, the most the base belt's rules allow), each step from the last tile back to the first, so that the tile
 * ahead is always done when the one behind hands an item over. An item goes into the next tile with the distance it went past the end of its own, so no
 * distance is lost. The throughput is {@code speed * 150} items a second (speed 0.96 is 144). With a speed of 0.4 or less it is one step per tick and the
 * belt behaves as the base belt does. Everything else is the base belt: the item layout (3 a tile), side input, unloading onto other blocks, saving.
 * Where the run ends (a corner, or another kind of block) the hand-over is the base belt's, as is what a side input does.
 */
public class FastConveyor extends Conveyor{
	/** The base belt's spacing between items, and the capacity of a tile (private constants there). */
	public static final float itemSpace = 0.4f;
	public static final int capacity = 3;

	public FastConveyor(String name){
		super(name);
	}

	public class FastConveyorBuild extends ConveyorBuild{
		/** The tick in which the run this tile is in was last moved. */
		protected float runTick = -1f;
		protected int idleTicks;

		/** The tile behind this one, if it is one of these and hands its items straight on to this one. */
		protected FastConveyorBuild before(){
			Building rear = nearby(Mathf.mod(rotation + 2, 4));
			return rear instanceof FastConveyorBuild f && f.team == team && f.rotation == rotation && f.nextc == this ? f : null;
		}

		/** The tile ahead of this one, if it is one of these and this one hands its items straight on to it. */
		protected FastConveyorBuild after(){
			return aligned && nextc instanceof FastConveyorBuild f && f.before() == this ? f : null;
		}

		@Override
		public void updateTile(){
			if(runTick == Time.time) return;
			FastConveyorBuild head = this;
			for(FastConveyorBuild b = before(); b != null && b != this; b = b.before()) head = b;
			head.moveRun();
		}

		private static final Seq<FastConveyorBuild> run = new Seq<>();

		/** Moves the whole run that starts at this tile for one tick. */
		protected void moveRun(){
			run.clear();
			for(FastConveyorBuild b = this; b != null && b.runTick != Time.time; b = b.after()){
				b.runTick = Time.time;
				run.add(b);
			}
			float moved = speed * edelta();
			int steps = Math.max(1, (int)Math.ceil(moved / itemSpace - 0.001f));
			float part = moved / steps;
			for(int s = 0; s < steps; s++){
				for(int i = run.size - 1; i >= 0; i--){
					run.get(i).step(part, s == steps - 1);
				}
			}
			run.clear();
		}

		/** One step of this tile, {@code moved} tile of movement: the base belt's update, with the hand-over to a tile of the same run keeping the overshoot. */
		protected void step(float moved, boolean last){
			minitem = 1f;
			mid = 0;

			//skip updates if possible
			if(len == 0 && Mathf.equal(timeScale, 1f)){
				clogHeat = 0f;
				if(last && ++idleTicks > 30) sleep();
				return;
			}
			idleTicks = 0;

			FastConveyorBuild fast = after();
			float nextMax = aligned ? 1f - Math.max(itemSpace - nextc.minitem, 0f) : 1f;
			boolean blocked = false;

			for(int i = len - 1; i >= 0; i--){
				float nextpos = (i == len - 1 ? 100f : ys[i + 1]) - itemSpace;
				float maxmove = Mathf.clamp(nextpos - ys[i], 0, moved);

				ys[i] += maxmove;
				boolean passed;
				if(fast != null){
					//into the next tile of the run with the distance it went past the end; one that cannot go holds back all behind it
					float room = fast.minitem - itemSpace;
					if(!blocked && ys[i] >= 1f && fast.len < capacity && room >= 0f){
						float over = Math.min(ys[i] - 1f, room);
						fast.handleItem(this, ids[i]);
						fast.ys[fast.lastInserted] = over;
						fast.minitem = Math.min(fast.minitem, over);
						passed = true;
					}else{
						blocked = true;
						passed = false;
						float limit = fast.len < capacity ? 1f + room : 1f;
						if(ys[i] > limit) ys[i] = limit;
					}
				}else{
					if(ys[i] > nextMax) ys[i] = nextMax;
					passed = ys[i] >= 1f && pass(ids[i]);
				}

				if(ys[i] > 0.5 && i > 0) mid = i - 1;
				xs[i] = Mathf.approach(xs[i], 0, moved * 2);

				if(passed){
					//align X position if passing forwards
					if(aligned){
						nextc.xs[nextc.lastInserted] = xs[i];
					}
					//remove last item
					items.remove(ids[i], len - i);
					len = Math.min(i, len);
				}else if(ys[i] < minitem){
					minitem = ys[i];
				}
			}

			if(minitem < itemSpace + (blendbits == 1 ? 0.3f : 0f)){
				clogHeat = Mathf.approachDelta(clogHeat, 1f, 1f / 60f);
			}else{
				clogHeat = 0f;
			}

			noSleep();
		}
	}
}
