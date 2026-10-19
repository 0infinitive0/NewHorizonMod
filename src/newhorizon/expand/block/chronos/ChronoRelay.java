package newhorizon.expand.block.chronos;

import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.type.Item;

/**
 * Item teleporter. Whatever is put into one relay comes out of another relay set to the same channel, at once and over any distance.
 * Both ends work both ways. An end that is full (it cannot get rid of what it holds) stops taking items, so a clogged line backs up
 * instead of losing anything.
 */
public class ChronoRelay extends ChannelBlock{
	public ChronoRelay(String name){
		super(name);
		hasItems = true;
		solid = true;
		acceptsItems = true;
		unloadable = false;
		itemCapacity = 20;
	}

	public class ChronoRelayBuild extends ChannelBuild{
		protected int cursor;

		/** The partner an item would go to now: one that works and has room for it. */
		public ChannelBuild route(Item item){
			if(efficiency <= 0.01f) return null;
			Seq<ChannelBuild> options = partners();
			for(int i = 0; i < options.size; i++){
				ChronoRelayBuild other = (ChronoRelayBuild)options.get((cursor + i) % options.size);
				if(other.efficiency > 0.01f && other.items.get(item) < other.getMaximumAccepted(item)) return other;
			}
			return null;
		}

		@Override
		public boolean acceptItem(Building source, Item item){
			//Items coming from another relay are the delivery itself.
			return route(item) != null;
		}

		@Override
		public void handleItem(Building source, Item item){
			ChannelBuild target = route(item);
			if(target == null){
				items.add(item, 1);
				return;
			}
			cursor++;
			target.items.add(item, 1);
			newhorizon.content.chronos.ChronosFx.sparkBurst.at(x, y, channelColor());
		}

		@Override
		public void updateTile(){
			if(items.any() && efficiency > 0.01f) dump();
		}

		@Override
		public boolean canDump(Building to, Item item){
			//Never hand items back to another relay: that would bounce them forever.
			return !(to instanceof ChronoRelayBuild);
		}

		@Override
		public int getMaximumAccepted(Item item){
			return itemCapacity;
		}

		@Override
		public void draw(){
			super.draw();
			drawChannel();
		}
	}
}
