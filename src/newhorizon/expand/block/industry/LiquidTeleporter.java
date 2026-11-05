package newhorizon.expand.block.industry;

import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.world.blocks.liquid.LiquidBlock;
import mindustry.world.meta.BlockGroup;
import newhorizon.expand.block.chronos.ChannelBlock;

/**
 * The Liquid Teleporter. Each end is an <b>input</b> (the default) or an <b>output</b>: an input takes liquid from the conduits and tanks
 * next to it and moves it at once, over any distance, to the outputs on the same channel; an output lets what it receives flow out into
 * the conduits and tanks next to it. An input never puts anything out and an output never takes anything in from its neighbours. One
 * liquid at a time, as in any block: an output that already holds another liquid is not sent a different one.
 *
 * <p>Liquid that comes in from a neighbour is held in a staging buffer and sent on from there, up to {@link #transfer} a tick.
 */
public class LiquidTeleporter extends ChannelBlock{
	/** Most liquid sent through in a tick (1,200 units a second). */
	public float transfer = 20f;

	public LiquidTeleporter(String name){
		super(name);
		hasLiquids = true;
		outputsLiquid = true;
		solid = true;
		update = true;
		group = BlockGroup.liquids;
		liquidCapacity = 2000f;
		buildType = LiquidTeleporterBuild::new;
		//Channel and mode together (the inherited channel buttons still set the channel alone).
		config(Point2.class, (LiquidTeleporterBuild build, Point2 p) -> {
			build.channel = Mathf.mod(p.x, channels);
			build.output = p.y != 0;
		});
	}

	@Override
	public void setBars(){
		super.setBars();
		addLiquidBar(b -> b.liquids.current());
	}

	public class LiquidTeleporterBuild extends ChannelBuild{
		/** Whether this end puts liquid out (otherwise it takes it in). */
		public boolean output;
		/** Liquid taken in from a neighbour and waiting to be sent. */
		public Liquid stageLiquid;
		public float stage;
		protected int cursor;

		public boolean holdsOnly(Liquid liquid){
			return liquids.currentAmount() < 0.2f || liquids.current() == liquid;
		}

		/** The output that could take this liquid now (it works, and holds nothing else), the one with the most room first from a rotating start. */
		protected LiquidTeleporterBuild route(Liquid liquid, float amount){
			Seq<ChannelBuild> options = partners();
			LiquidTeleporterBuild best = null;
			float room = 0f;
			for(int i = 0; i < options.size; i++){
				LiquidTeleporterBuild other = (LiquidTeleporterBuild)options.get((cursor + i) % options.size);
				if(!other.output || other.efficiency <= 0.01f || !other.holdsOnly(liquid)) continue;
				float free = other.block.liquidCapacity - other.liquids.currentAmount();
				if(free > room){
					room = free;
					best = other;
				}
			}
			return room >= Math.min(amount, 1f) ? best : null;
		}

		@Override
		public boolean acceptLiquid(Building source, Liquid liquid){
			//An output takes only what the inputs send; an input takes only from its neighbours.
			if(source instanceof LiquidTeleporterBuild) return output && holdsOnly(liquid);
			if(output) return false;
			return stage < liquidCapacity - 0.01f && (stage < 0.01f || stageLiquid == liquid);
		}

		@Override
		public void handleLiquid(Building source, Liquid liquid, float amount){
			if(source instanceof LiquidTeleporterBuild){
				super.handleLiquid(source, liquid, amount);
				return;
			}
			//Staged; what does not fit goes to the output buffer so that nothing is lost.
			float take = Math.min(Math.max(0f, liquidCapacity - stage), amount);
			stageLiquid = liquid;
			stage += take;
			if(amount > take) super.handleLiquid(source, liquid, amount - take);
		}

		@Override
		public boolean canDumpLiquid(Building to, Liquid liquid){
			//Only an output puts liquid out, and never into another teleporter: that would bounce it.
			return output && !(to instanceof LiquidTeleporterBuild);
		}

		@Override
		public void updateTile(){
			if(output){
				//Whatever was staged when the mode was changed becomes output.
				if(stage > 0.001f && stageLiquid != null && holdsOnly(stageLiquid)){
					float move = Math.min(stage, liquidCapacity - liquids.currentAmount());
					liquids.add(stageLiquid, move);
					stage -= move;
				}
				if(liquids.currentAmount() > 0.001f) dumpLiquid(liquids.current());
			}else{
				//Whatever was in the output buffer when the mode was changed is staged to be sent.
				if(liquids.currentAmount() > 0.001f && (stage < 0.01f || stageLiquid == liquids.current())){
					Liquid held = liquids.current();
					float move = Math.min(liquids.currentAmount(), liquidCapacity - stage);
					stageLiquid = held;
					stage += move;
					liquids.remove(held, move);
				}
				if(stage > 0.001f && stageLiquid != null && efficiency > 0.01f){
					LiquidTeleporterBuild target = route(stageLiquid, stage);
					if(target != null){
						float free = target.block.liquidCapacity - target.liquids.currentAmount();
						float move = Math.min(Math.min(stage, transfer * edelta()), free);
						if(move > 0f){
							target.liquids.add(stageLiquid, move);
							stage -= move;
							cursor++;
						}
					}
				}
			}
			if(stage < 0.001f) stage = 0f;
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
			drawChannel();
			TeleporterMode.marks(this, output);
		}

		@Override
		public byte version(){
			return 2;
		}

		@Override
		public void write(Writes write){
			super.write(write);
			write.i(stageLiquid == null ? -1 : stageLiquid.id);
			write.f(stage);
			write.bool(output);
		}

		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			if(revision >= 1){
				int id = read.i();
				stageLiquid = id < 0 ? null : mindustry.Vars.content.liquid(id);
				stage = read.f();
			}
			output = revision >= 2 && read.bool();
		}
	}
}
