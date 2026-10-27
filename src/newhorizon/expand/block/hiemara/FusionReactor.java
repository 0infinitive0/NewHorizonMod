package newhorizon.expand.block.hiemara;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.util.Strings;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Liquids;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

/**
 * Burns a lot of hydrogen and ice for a huge amount of power. It cannot start by itself: while it is cold it draws a
 * lot of power from the grid until it ignites, and after that the output builds up slowly. If the fuel runs out the
 * plasma cools down, and it has to be started again.
 */
public class FusionReactor extends PowerGenerator{
	public Liquid fuelLiquid = Liquids.hydrogen;
	public Item fuelItem;
	/** Hydrogen burnt per tick, and ticks per piece of ice. */
	public float liquidPerTick = 0.9f, itemInterval = 40f;
	/** Power drawn per tick until the reactor has ignited. */
	public float startPower = 12f;
	/** Ticks of full power and fuel until ignition, ticks from ignition to full output, and ticks to cool down. */
	public float igniteTime = 30f * 60f, buildUpTime = 60f * 60f, coolTime = 20f * 60f;
	
	public FusionReactor(String name){
		super(name);
		
		hasLiquids = true;
		hasItems = true;
		hasPower = true;
		placeableLiquid = true;
		liquidCapacity = 400f;
		itemCapacity = 40;
		outputsPower = true;
		consumePower(startPower);
	}
	
	@Override
	public void init(){
		//The kickstart power is set after the fields of the instance are known.
		if(consPower != null)consPower.usage = startPower;
		super.init();
		//A generator is not a consumer by default, but the reactor draws power while it starts.
		consumesPower = true;
	}
	
	@Override
	public void setStats(){
		super.setStats();
		
		stats.add(Stat.input, table -> {
			table.row();
			table.add(fuelLiquid.localizedName + " " + Strings.fixed(liquidPerTick * 60f, 0) + "/s").left().row();
			table.add(fuelItem.localizedName + " " + Strings.fixed(60f / itemInterval, 1) + "/s").left();
		});
		stats.add(Stat.basePowerGeneration, powerProduction * 60f, StatUnit.powerSecond);
		stats.add(Stat.boostEffect, Core.bundle.format("fusion.start", Strings.fixed(startPower * 60f, 0), Strings.fixed(igniteTime / 60f, 0), Strings.fixed(buildUpTime / 60f, 0)));
	}
	
	@Override
	public void setBars(){
		super.setBars();
		
		removeBar("power");
		addBar("power", (FusionReactorBuild build) -> new Bar(
			() -> Core.bundle.format("bar.poweroutput", Strings.fixed(build.getPowerProduction() * 60f * build.timeScale(), 1)),
			() -> Pal.powerBar,
			() -> build.productionEfficiency
		));
		addBar("ignition", (FusionReactorBuild build) -> new Bar(
			() -> Core.bundle.get(build.burning ? "fusion.burning" : "fusion.ignition"),
			() -> build.burning ? Pal.accent : Pal.lightishOrange,
			() -> build.burning ? build.heat : build.ignition
		));
	}
	
	public class FusionReactorBuild extends GeneratorBuild{
		/** Progress of the kickstart, 0 to 1. */
		public float ignition;
		/** Output level while burning, 0 to 1. */
		public float heat;
		public boolean burning;
		
		protected float itemTimer;
		
		@Override
		public boolean acceptItem(Building source, Item item){
			return item == fuelItem && items.get(item) < itemCapacity;
		}
		
		@Override
		public boolean acceptLiquid(Building source, Liquid liquid){
			return liquid == fuelLiquid && liquids.get(liquid) < liquidCapacity - 0.01f;
		}
		
		/** The kickstart power is only drawn until the reactor burns. */
		@Override
		public boolean shouldConsume(){
			return !burning && enabled;
		}
		
		protected boolean fuelReady(){
			return liquids.get(fuelLiquid) >= liquidPerTick && items.get(fuelItem) > 0;
		}
		
		@Override
		public void updateTile(){
			boolean fuel = fuelReady() && enabled;
			
			if(fuel && (burning || ignition > 0f || efficiency > 0f)){
				liquids.remove(fuelLiquid, liquidPerTick * Time.delta);
				itemTimer += Time.delta;
				if(itemTimer >= itemInterval){
					itemTimer -= itemInterval;
					items.remove(fuelItem, 1);
				}
			}
			
			if(!burning){
				//Only a grid that can really supply the kickstart power gets it going.
				if(fuel){
					ignition = Mathf.approach(ignition, 1f, efficiency / igniteTime * Time.delta);
				}else{
					ignition = Mathf.approach(ignition, 0f, Time.delta / igniteTime);
				}
				heat = Mathf.approach(heat, 0f, Time.delta / coolTime);
				if(ignition >= 1f){
					burning = true;
					ignition = 1f;
				}
			}else if(fuel){
				heat = Mathf.approach(heat, 1f, Time.delta / buildUpTime);
			}else{
				heat = Mathf.approach(heat, 0f, Time.delta / coolTime);
				if(heat <= 0.02f){
					burning = false;
					ignition = 0f;
					heat = 0f;
				}
			}
			
			productionEfficiency = burning ? heat : 0f;
		}
		
		@Override
		public void draw(){
			super.draw();
			
			float glow = burning ? 0.25f + heat * 0.75f : ignition * 0.3f;
			if(glow > 0.01f){
				Draw.z(Layer.blockOver);
				Draw.color(Color.valueOf("b5e4ff"), Color.white, Mathf.absin(Time.time, 6f, 0.4f));
				Draw.alpha(glow * 0.8f);
				Fill.circle(x, y, size * Vars.tilesize * 0.28f * (0.6f + glow * 0.4f));
				Draw.reset();
			}
		}
		
		@Override
		public float warmup(){
			return burning ? heat : ignition * 0.3f;
		}
		
		@Override
		public void write(Writes write){
			super.write(write);
			write.f(ignition);
			write.f(heat);
			write.bool(burning);
		}
		
		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			ignition = read.f();
			heat = read.f();
			burning = read.bool();
		}
	}
}
