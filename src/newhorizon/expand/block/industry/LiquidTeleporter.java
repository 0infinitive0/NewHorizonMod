package newhorizon.expand.block.industry;

import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.world.blocks.liquid.LiquidBlock;
import mindustry.world.meta.BlockGroup;
import newhorizon.expand.block.chronos.ChannelBlock;

/**
 * The Liquid Teleporter: liquid put into one is moved at once to another one set to the same channel (over any distance), and each end
 * lets what it receives flow out into the conduits and tanks next to it. Both ends work both ways. One liquid at a time, as in any
 * block: an end that already holds another liquid is not sent a different one.
 *
 * <p>Liquid that comes in from a neighbour is held in a staging buffer and sent on from there; liquid that arrived from another teleporter
 * goes straight to the output and is never sent on, so nothing bounces back and forth. With no partner an end is an ordinary router.
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
	}

	@Override
	public void setBars(){
		super.setBars();
		addLiquidBar(b -> b.liquids.current());
	}

	public class LiquidTeleporterBuild extends ChannelBuild{
		/** Liquid taken in from a neighbour and waiting to be sent. */
		public Liquid stageLiquid;
		public float stage;
		protected int cursor;

		protected boolean linked(){
			return partners().size > 0;
		}

		public boolean holdsOnly(Liquid liquid){
			return liquids.currentAmount() < 0.2f || liquids.current() == liquid;
		}

		/** A partner that could take this liquid now (it works, and holds nothing else), the one with the most room first from a rotating start. */
		protected LiquidTeleporterBuild route(Liquid liquid, float amount){
			Seq<ChannelBuild> options = partners();
			LiquidTeleporterBuild best = null;
			float room = 0f;
			for(int i = 0; i < options.size; i++){
				LiquidTeleporterBuild other = (LiquidTeleporterBuild)options.get((cursor + i) % options.size);
				if(other.efficiency <= 0.01f || !other.holdsOnly(liquid)) continue;
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
			if(source instanceof LiquidTeleporterBuild) return holdsOnly(liquid);
			if(!linked()) return holdsOnly(liquid);
			return stage < 0.01f || stageLiquid == liquid;
		}

		@Override
		public void handleLiquid(Building source, Liquid liquid, float amount){
			if(source instanceof LiquidTeleporterBuild || !linked()){
				super.handleLiquid(source, liquid, amount);
				return;
			}
			//Staged; what does not fit goes to the output so that nothing is lost.
			float room = Math.max(0f, liquidCapacity - stage);
			float take = Math.min(room, amount);
			stageLiquid = liquid;
			stage += take;
			if(amount > take) super.handleLiquid(source, liquid, amount - take);
		}

		@Override
		public boolean canDumpLiquid(Building to, Liquid liquid){
			//Never hand liquid to another teleporter: that would bounce it.
			return !(to instanceof LiquidTeleporterBuild);
		}

		@Override
		public void updateTile(){
			if(stage > 0.001f && stageLiquid != null){
				if(!linked()){
					//No partner: it is an ordinary router.
					if(holdsOnly(stageLiquid)){
						float move = Math.min(stage, liquidCapacity - liquids.currentAmount());
						liquids.add(stageLiquid, move);
						stage -= move;
					}
				}else if(efficiency > 0.01f){
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
				if(stage < 0.001f){
					stage = 0f;
				}
			}
			if(liquids.currentAmount() > 0.001f) dumpLiquid(liquids.current());
		}

		@Override
		public void draw(){
			super.draw();
			drawChannel();
		}

		@Override
		public byte version(){
			return 1;
		}

		@Override
		public void write(Writes write){
			super.write(write);
			write.i(stageLiquid == null ? -1 : stageLiquid.id);
			write.f(stage);
		}

		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			if(revision >= 1){
				int id = read.i();
				stageLiquid = id < 0 ? null : mindustry.Vars.content.liquid(id);
				stage = read.f();
			}
		}
	}
}
