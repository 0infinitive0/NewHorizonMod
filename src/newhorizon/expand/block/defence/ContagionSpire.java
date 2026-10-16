package newhorizon.expand.block.defence;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import mindustry.world.meta.StatValues;
import newhorizon.content.jade.JadeColor;
import newhorizon.content.jade.JadeInfection;
import newhorizon.content.jade.JadeStatus;

import static mindustry.Vars.tilesize;

/**
 * Infects every enemy unit inside its range, over and over, for as long as it is supplied.
 *
 * <p>It applies exactly what the Sporeling's blast does ({@link JadeStatus#infested}) and records it with {@link JadeInfection}:
 * the host takes damage over time until it dies, which bursts it into Sporelings for this block's team (one per 100 max health),
 * or until it is healed back to full health. A host that was cured and is still inside the range is simply infected again, so
 * the only way to get rid of it is to leave. It does nothing to buildings and cannot hurt anything by itself: the damage comes
 * from the infestation.
 *
 * <p>It runs on three supplies at once: spore pods (one lasts {@link #itemDuration} ticks), water ({@link #waterUse} per tick) and
 * power ({@link #powerUse} per tick). All three have a store sized so that a completely full block runs for the same time, until
 * every store is empty together: {@code itemCapacity * itemDuration} ticks. The power is stored inside the block (it charges at
 * {@link #chargeRate} per tick whenever it is not full, and then needs nothing from the grid until it runs low), the water in its
 * tank. It stops the moment any of the three runs out, and uses nothing while it has no spore to burn.
 */
public class ContagionSpire extends Block{
	public float range = 170f;
	/** Ticks between two sweeps for units to infect. */
	public float reload = 15f;
	/** Duration passed to the status. Infested never times out, so this only matters for other effects given here. */
	public float statusDuration = 60f * 10f;
	/** Ticks one spore pod keeps it running. */
	public float itemDuration = 60f * 12f;
	/** The item it burns. */
	public Item fuel = Items.sporePod;
	/** Water used per tick while running. */
	public float waterUse = 0.2f;
	/** Power used per tick while running, taken from the internal store. */
	public float powerUse = 1.2f;
	/** Power it asks the grid for per tick while its store is not full. */
	public float chargeRate = 6f;
	/** Power store; set in {@link #init()} so that a full block runs as long as a full spore store lasts. */
	public float energyCapacity;
	public Effect pulseEffect = null;
	public float pulseTime = 100f;

	public ContagionSpire(String name){
		super(name);
		update = true;
		solid = true;
		hasPower = true;
		hasItems = true;
		hasLiquids = true;
		itemCapacity = 40;
		group = BlockGroup.projectors;
		envEnabled |= mindustry.world.meta.Env.space;
	}

	/** How long a completely full block runs, in ticks. */
	public float fullRunTime(){
		return itemCapacity * itemDuration;
	}

	@Override
	public void init(){
		energyCapacity = powerUse * fullRunTime();
		liquidCapacity = waterUse * fullRunTime();

		//The grid only sees the charging. What it drinks is taken from the tank by hand, so that it is not wasted while charging.
		consumePower(chargeRate);
		consumeLiquid(Liquids.water, waterUse).update(false);
		super.init();
	}

	@Override
	public void setStats(){
		super.setStats();
		stats.add(Stat.range, range / tilesize, StatUnit.blocks);
		stats.add(Stat.input, StatValues.items(itemDuration, ItemStack.with(fuel, 1)));
		stats.add(Stat.input, StatValues.liquid(Liquids.water, waterUse * 60f, true));
		stats.add(Stat.powerUse, powerUse * 60f, StatUnit.powerSecond);
		stats.add(Stat.itemCapacity, itemCapacity, StatUnit.items);
		stats.add(Stat.liquidCapacity, liquidCapacity, StatUnit.liquidUnits);
		stats.add(Stat.powerCapacity, energyCapacity, StatUnit.powerUnits);
		stats.add(Stat.productionTime, fullRunTime() / 60f, StatUnit.seconds);
	}

	@Override
	public void setBars(){
		super.setBars();
		addBar("energy", (ContagionSpireBuild b) -> new Bar(
			() -> Core.bundle.format("bar.new-horizon-energy", (int)b.energy, (int)energyCapacity),
			() -> Pal.powerBar,
			() -> b.energy / energyCapacity));
	}

	/** Spore pods are taken by hand (conveyors, unloaders): the consumers only cover power and water. */
	@Override
	public boolean consumesItem(Item item){
		return item == fuel;
	}

	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		super.drawPlace(x, y, rotation, valid);
		Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, JadeColor.infection);
	}

	public class ContagionSpireBuild extends Building{
		public float sweepTimer = 0f, pulseTimer = 0f, smoothEfficiency = 0f;
		/** Ticks left on the spore pod being burned. */
		public float burnTime = 0f;
		/** Power stored inside the block. */
		public float energy = 0f;

		@Override
		public boolean acceptItem(Building source, Item item){
			return item == fuel && items.get(fuel) < itemCapacity;
		}

		/** It draws from the grid while its store is not full, and burns while there is a spore to burn. */
		@Override
		public boolean shouldConsume(){
			return energy < energyCapacity - 0.001f || burnTime > 0f || items.has(fuel);
		}

		/** Whether everything it needs is there to run this tick. */
		protected boolean supplied(){
			return energy > 0f && liquids.get(Liquids.water) > 0f;
		}

		/** Whether it is running right now. */
		public boolean active(){
			return burnTime > 0f && supplied();
		}

		@Override
		public void updateTile(){
			//charge the store from whatever the grid gives it, which is what it asked for
			if(energy < energyCapacity){
				energy = Math.min(energyCapacity, energy + power.status * chargeRate * delta());
			}

			//light the next spore when the last one is spent, but only if it can run
			if(burnTime <= 0f && supplied() && items.has(fuel)){
				items.remove(fuel, 1);
				burnTime = itemDuration;
			}

			boolean running = active();
			if(running){
				float dt = delta();
				//the last tick of a store may be shorter than a whole tick
				float used = Math.min(dt, Math.min(burnTime, Math.min(energy / powerUse, liquids.get(Liquids.water) / waterUse)));
				burnTime -= used;
				energy = Math.max(energy - powerUse * used, 0f);
				liquids.remove(Liquids.water, waterUse * used);
			}

			smoothEfficiency = Mathf.lerpDelta(smoothEfficiency, running ? 1f : 0f, 0.08f);
			if(!running) return;

			pulseTimer += delta();
			if(pulseEffect != null && pulseTimer >= pulseTime){
				pulseTimer = 0f;
				pulseEffect.at(x, y);
			}

			sweepTimer += delta();
			if(sweepTimer >= reload){
				sweepTimer = 0f;
				infectInRange();
			}
		}

		/** Gives the infestation to each enemy unit in range, ground or air. Server side, like every status. */
		public void infectInRange(){
			if(Vars.net.client()) return;

			Units.nearbyEnemies(team, x - range, y - range, range * 2f, range * 2f, unit -> {
				if(!unit.isValid() || unit.dead() || !unit.targetable(team) || !unit.within(this, range + unit.hitSize / 2f)) return;
				infect(unit);
			});
		}

		protected void infect(Unit unit){
			unit.apply(JadeStatus.infested, statusDuration);
			JadeInfection.infect(unit, team);
		}

		@Override
		public void drawSelect(){
			Drawf.dashCircle(x, y, range, JadeColor.infection);
		}

		@Override
		public void draw(){
			super.draw();

			if(smoothEfficiency > 0.01f){
				Draw.z(Layer.blockOver);
				float s = Mathf.absin(Time.time, 9f, 1f);
				Draw.color(JadeColor.infection, 0.28f * smoothEfficiency);
				Lines.stroke(1.2f + s * 0.5f);
				Lines.circle(x, y, size * tilesize / 2f + 3f + s * 2f);
				Draw.reset();
			}
		}

		@Override
		public byte version(){
			return 1;
		}

		@Override
		public void write(Writes write){
			super.write(write);
			write.f(burnTime);
			write.f(energy);
		}

		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			if(revision >= 1){
				burnTime = read.f();
				energy = read.f();
			}
		}
	}
}
