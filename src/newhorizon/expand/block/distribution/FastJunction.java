package newhorizon.expand.block.distribution;

import arc.util.Time;
import mindustry.gen.BufferItem;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.blocks.distribution.Conveyor;
import mindustry.world.blocks.distribution.Junction;

import static mindustry.Vars.content;

/**
 * A junction that can pass as many items as the fast belts carry. The base junction holds {@code capacity} items per direction for {@code speed} ticks and
 * hands on at most one a tick; this one hands on <b>every</b> item that is due, and into a {@link FastConveyor.FastConveyorBuild} it puts the item as far into
 * the belt as the stream ahead allows (0.4 behind the last item, at most one tick of the belt's movement), so that more than one item can enter the belt in a tick. Everything
 * else (the buffers, saving, what it accepts) is the base junction. Set {@code speed} (ticks inside), {@code capacity} (items per direction) and {@code displayedSpeed} (items a second).
 */
public class FastJunction extends Junction{
	public FastJunction(String name){
		super(name);
	}

	public class FastJunctionBuild extends JunctionBuild{
		@Override
		public void updateTile(){
			for(int i = 0; i < 4; i++){
				if(buffer.indexes[i] > capacity) buffer.indexes[i] = capacity;
				while(buffer.indexes[i] > 0){
					long l = buffer.buffers[i][0];
					float time = BufferItem.time(l);
					float due = time + speed / timeScale;
					if(!(Time.time >= due || Time.time < time)) break;

					Item item = content.item(BufferItem.item(l));
					Building dest = nearby(i);
					//one that is not wanted keeps waiting until it is
					if(item == null || dest == null || dest.team != team) break;

					if(dest instanceof FastConveyor.FastConveyorBuild f && f.rearRoom(this, item) >= 0f){
						//as far in as the stream ahead allows, but not more than one tick of the belt's movement
						f.giveRear(this, item, Math.min(((Conveyor)f.block).speed, f.rearRoom(this, item)));
					}else{
						if(!dest.acceptItem(this, item)) break;
						dest.handleItem(this, item);
					}

					System.arraycopy(buffer.buffers[i], 1, buffer.buffers[i], 0, buffer.indexes[i] - 1);
					buffer.indexes[i]--;
				}
			}
		}
	}
}
