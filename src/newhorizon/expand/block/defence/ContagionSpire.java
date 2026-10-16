package newhorizon.expand.block.defence;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.Effect;
import mindustry.content.Items;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.meta.StatValues;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import newhorizon.content.jade.JadeColor;
import newhorizon.content.jade.JadeInfection;
import newhorizon.content.jade.JadeStatus;

import static mindustry.Vars.tilesize;

/**
 * Infects every enemy unit inside its range, over and over, for as long as it has power.
 *
 * <p>It applies exactly what the Sporeling's blast does ({@link JadeStatus#infested}) and records it with {@link JadeInfection}:
 * the host takes damage over time until it dies, which bursts it into Sporelings for this block's team (one per 100 max health),
 * or until it is healed back to full health. A host that was cured and is still inside the range is simply infected again, so
 * the only way to get rid of it is to leave. It does nothing to buildings and cannot hurt anything by itself: the damage comes
 * from the infestation.
 *
 * <p>It has to be fed constantly: power, a steady flow of water, and spore pods (one lasts {@link #itemDuration} ticks). It
 * only draws water and power while it has a spore to burn, and stops the moment any of the three runs out.
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

	@Override
	public void setStats(){
		super.setStats();
		stats.add(Stat.range, range / tilesize, StatUnit.blocks);
		stats.add(Stat.input, StatValues.items(itemDuration, ItemStack.with(fuel, 1)));
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

		@Override
		public boolean acceptItem(Building source, Item item){
			return item == fuel && items.get(fuel) < itemCapacity;
		}

		/** Water and power are only drawn while there is something burning or something to burn. */
		@Override
		public boolean shouldConsume(){
			return burnTime > 0f || items.has(fuel);
		}

		/** Whether it is running right now. */
		public boolean active(){
			return burnTime > 0f && efficiency > 0f;
		}

		@Override
		public void updateTile(){
			//light the next spore when the last one is spent, but only if power and water are there
			if(burnTime <= 0f && efficiency > 0f && items.has(fuel)){
				items.remove(fuel, 1);
				burnTime = itemDuration;
			}

			boolean running = active();
			if(running) burnTime = Math.max(burnTime - delta(), 0f);

			smoothEfficiency = Mathf.lerpDelta(smoothEfficiency, running ? 1f : 0f, 0.08f);
			if(!running) return;

			pulseTimer += edelta();
			if(pulseEffect != null && pulseTimer >= pulseTime){
				pulseTimer = 0f;
				pulseEffect.at(x, y);
			}

			sweepTimer += edelta();
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
	}
}
