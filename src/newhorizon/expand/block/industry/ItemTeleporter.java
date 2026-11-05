package newhorizon.expand.block.industry;

import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.type.Item;
import newhorizon.expand.block.chronos.ChronoRelay;

/**
 * The Item Teleporter. Each end is set to be an <b>input</b> (the default) or an <b>output</b>: an input takes items from the conveyors
 * next to it and sends them at once, over any distance, to the outputs on the same channel; an output puts out what the inputs send into
 * whatever is next to it. An input never puts anything out and an output never takes anything in. The behaviour (instant, a full output
 * backs the line up, items shared between outputs) is the Chronos relay's, with a bigger buffer and an output that hands items on every
 * tick instead of every fifth.
 */
public class ItemTeleporter extends ChronoRelay{
	public ItemTeleporter(String name){
		super(name);
		itemCapacity = 60;
		dumpTime = 1;
		buildType = ItemTeleporterBuild::new;
		//Channel and mode together (the inherited channel buttons still set the channel alone).
		config(Point2.class, (ItemTeleporterBuild build, Point2 p) -> {
			build.channel = Mathf.mod(p.x, channels);
			build.output = p.y != 0;
		});
	}

	public class ItemTeleporterBuild extends ChronoRelayBuild{
		/** Whether this end puts items out (otherwise it takes them in). */
		public boolean output;

		/** The output an item would go to now: only an input sends, only to outputs that work and have room for it. */
		@Override
		public ChannelBuild route(Item item){
			if(output || efficiency <= 0.01f) return null;
			Seq<ChannelBuild> options = partners();
			for(int i = 0; i < options.size; i++){
				ItemTeleporterBuild other = (ItemTeleporterBuild)options.get((cursor + i) % options.size);
				if(other.output && other.efficiency > 0.01f && other.items.get(item) < other.getMaximumAccepted(item)) return other;
			}
			return null;
		}

		@Override
		public void updateTile(){
			if(efficiency <= 0.01f || !items.any()) return;
			if(output){
				dump();
			}else{
				//An input that holds something (its mode was changed) sends it on, one a tick.
				for(Item item : Vars.content.items()){
					if(!items.has(item)) continue;
					ChannelBuild target = route(item);
					if(target != null){
						cursor++;
						target.items.add(item, 1);
						items.remove(item, 1);
					}
					break;
				}
			}
		}

		@Override
		public boolean canDump(Building to, Item item){
			return output && super.canDump(to, item);
		}

		@Override
		public void buildConfiguration(Table table){
			super.buildConfiguration(table);
			TeleporterMode.buttons(table, () -> output, () -> channel, this);
		}

		@Override
		public Object config(){
			return TeleporterMode.pack(channel, output);
		}

		@Override
		public void draw(){
			super.draw();
			TeleporterMode.marks(this, output);
		}

		@Override
		public byte version(){
			return 1;
		}

		@Override
		public void write(Writes write){
			super.write(write);
			write.bool(output);
		}

		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			output = revision >= 1 && read.bool();
		}
	}
}
