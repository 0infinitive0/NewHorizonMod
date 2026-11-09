package newhorizon.expand.block.supply;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.type.StatusEffect;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;
import mindustry.world.meta.StatUnit;

import static mindustry.Vars.tilesize;

/**
 * A tower that gives a status effect to the units around it, according to what it is fed. Each item or liquid it takes is tied to one
 * status effect and to a side: a <b>buff</b> goes to every unit of the tower's own team inside the range, a <b>debuff</b> to every enemy
 * unit. It needs power, burns one item every {@link #itemTime} ticks (or a liquid at {@link #liquidUse} a tick) for as long as that effect
 * is running, and runs at most {@link #maxActive} effects at once: of what it holds, the ones already burning go on first, then the rest
 * in the order the tower lists them. It does nothing to buildings.
 */
public class StatusTower extends Block{
	/** One supply the tower accepts: an item or a liquid, the effect it gives and to whom. */
	public static class Slot{
		public Item item;
		public Liquid liquid;
		public StatusEffect effect;
		/** True for a buff for the tower's own team, false for a debuff for its enemies. */
		public boolean friendly;

		public String name(){
			return item != null ? item.localizedName : liquid.localizedName;
		}

		public Color color(){
			return effect != null && effect.color != null ? effect.color : Color.white;
		}
	}

	public static final Stat slotsStat = new Stat("nh-tower-slots", StatCat.function);

	public final Seq<Slot> slots = new Seq<>();
	public float range = 110f;
	/** Ticks between two applications of the effects, and how long each application lasts (a little over three sweeps). */
	public float reload = 20f, effectDuration = 70f;
	/** Ticks one item keeps its effect running, and a liquid's use a tick. */
	public float itemTime = 600f, liquidUse = 0.05f;
	/** How many effects can run at once. */
	public int maxActive = 2;
	/** Power satisfaction under which it does nothing. */
	public float minEfficiency = 0.5f;
	public Color color = Color.white;
	public TextureRegion topRegion;

	public StatusTower(String name){
		super(name);
		update = true;
		solid = true;
		hasPower = true;
		hasItems = true;
		hasLiquids = true;
		group = BlockGroup.projectors;
		envEnabled |= mindustry.world.meta.Env.space;
		buildType = StatusTowerBuild::new;
	}

	/** Adds an item that gives {@code effect}; {@code friendly} sends it to the tower's own team. */
	public StatusTower item(Item item, StatusEffect effect, boolean friendly){
		Slot s = new Slot();
		s.item = item;
		s.effect = effect;
		s.friendly = friendly;
		slots.add(s);
		return this;
	}

	public StatusTower liquid(Liquid liquid, StatusEffect effect, boolean friendly){
		Slot s = new Slot();
		s.liquid = liquid;
		s.effect = effect;
		s.friendly = friendly;
		slots.add(s);
		return this;
	}

	public int slotOf(Item item){
		for(int i = 0; i < slots.size; i++) if(slots.get(i).item == item) return i;
		return -1;
	}

	public int slotOf(Liquid liquid){
		for(int i = 0; i < slots.size; i++) if(slots.get(i).liquid == liquid) return i;
		return -1;
	}

	@Override
	public void load(){
		super.load();
		topRegion = Core.atlas.find(name + "-top");
	}

	@Override
	public void setStats(){
		super.setStats();
		stats.add(Stat.range, range / tilesize, StatUnit.blocks);
		StringBuilder sb = new StringBuilder();
		for(Slot s : slots){
			if(s.effect == null) continue;
			sb.append("\n[#").append(s.color().toString(), 0, 6).append("]").append(s.effect.localizedName).append("[] <- ").append(s.name()).append(": ")
				.append(Core.bundle.get(s.friendly ? "stat.nh-tower-friendly" : "stat.nh-tower-enemy"));
		}
		stats.add(slotsStat, Core.bundle.format("stat.nh-tower-slots.value", maxActive, (int)(itemTime / 60f), (int)(liquidUse * 60f * 10f) / 10f) + sb);
	}

	@Override
	public void setBars(){
		super.setBars();
		addBar("nh-tower-active", (StatusTowerBuild b) -> new Bar(
			() -> Core.bundle.format("bar.nh-tower-active", b.activeCount(), maxActive),
			() -> color,
			() -> maxActive == 0 ? 0f : b.activeCount() / (float)maxActive));
		for(int i = 0; i < maxActive; i++){
			int index = i;
			addBar("nh-tower-slot-" + i, (StatusTowerBuild b) -> new Bar(
				() -> {
					Slot s = b.activeSlot(index);
					return s == null ? "-" : s.effect.localizedName + (s.friendly ? " (+)" : " (-)");
				},
				() -> {
					Slot s = b.activeSlot(index);
					return s == null ? Pal.gray : s.color();
				},
				() -> {
					Slot s = b.activeSlot(index);
					return s == null ? 0f : b.fill(slots.indexOf(s));
				}));
		}
	}

	@Override
	public boolean consumesItem(Item item){
		return slotOf(item) >= 0;
	}

	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		super.drawPlace(x, y, rotation, valid);
		Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, color);
	}

	public class StatusTowerBuild extends Building{
		/** Ticks left on the item burning in each slot (0: none). */
		public float[] burn = new float[slots.size];
		/** The slots running now, in order. */
		public final Seq<Integer> active = new Seq<>();
		public float sweep, ring;

		@Override
		public boolean acceptItem(Building source, Item item){
			int i = slotOf(item);
			return i >= 0 && items.get(item) < itemCapacity;
		}

		@Override
		public boolean acceptLiquid(Building source, Liquid liquid){
			return slotOf(liquid) >= 0 && liquids.get(liquid) < liquidCapacity - 0.001f;
		}

		/** Whether the slot has something to run on right now. */
		protected boolean fed(int i){
			Slot s = slots.get(i);
			if(s.item != null) return burn[i] > 0f || items.has(s.item);
			return liquids.get(s.liquid) > 0.001f;
		}

		/** It draws power only while it has something to run on. */
		@Override
		public boolean shouldConsume(){
			for(int i = 0; i < slots.size; i++) if(fed(i)) return true;
			return false;
		}

		public int activeCount(){
			return active.size;
		}

		public Slot activeSlot(int index){
			return index < active.size ? slots.get(active.get(index)) : null;
		}

		/** How full the slot's supply is (the item burning, or the liquid held), for the bars. */
		public float fill(int i){
			Slot s = slots.get(i);
			if(s.item != null) return Mathf.clamp(burn[i] / itemTime);
			return Mathf.clamp(liquids.get(s.liquid) / liquidCapacity);
		}

		/** Whether it is powered well enough to work. */
		public boolean powered(){
			return efficiency >= minEfficiency;
		}

		@Override
		public void updateTile(){
			float dt = delta();

			//which supplies run: the ones already burning first, then the others in the tower's order
			active.clear();
			if(powered()){
				for(int i = 0; i < slots.size && active.size < maxActive; i++){
					if(slots.get(i).effect != null && slots.get(i).item != null && burn[i] > 0f) active.add(i);
				}
				for(int i = 0; i < slots.size && active.size < maxActive; i++){
					Slot s = slots.get(i);
					if(s.effect == null || active.contains(i)) continue;
					if(s.item != null){
						if(burn[i] <= 0f && items.has(s.item)){
							if(!Vars.net.client()) items.remove(s.item, 1);
							burn[i] = itemTime;
							active.add(i);
						}
					}else if(liquids.get(s.liquid) > 0.001f){
						active.add(i);
					}
				}
			}

			//burn what runs
			for(int k = 0; k < active.size; k++){
				int i = active.get(k);
				Slot s = slots.get(i);
				if(s.item != null){
					burn[i] = Math.max(0f, burn[i] - dt);
				}else{
					liquids.remove(s.liquid, Math.min(liquids.get(s.liquid), liquidUse * dt));
				}
			}

			//and give the effects out
			if(active.size == 0){
				sweep = 0f;
				return;
			}
			sweep += dt;
			ring = Mathf.clamp(sweep / reload);
			if(sweep >= reload){
				sweep = 0f;
				if(!Vars.net.client()) applyEffects();
			}
		}

		/** One sweep: each running effect to its side, inside the range. */
		public void applyEffects(){
			for(int k = 0; k < active.size; k++){
				Slot s = slots.get(active.get(k));
				if(s.effect == null) continue;
				if(s.friendly){
					Units.nearby(team, x, y, range, unit -> {
						if(unit.dead() || !unit.isAdded()) return;
						unit.apply(s.effect, effectDuration);
					});
				}else{
					Units.nearbyEnemies(team, x - range, y - range, range * 2f, range * 2f, unit -> {
						if(unit.dead() || !unit.isAdded() || !unit.targetable(team) || !unit.within(this, range + unit.hitSize / 2f)) return;
						unit.apply(s.effect, effectDuration);
					});
				}
			}
		}

		@Override
		public void drawSelect(){
			Drawf.dashCircle(x, y, range, color);
		}

		@Override
		public void draw(){
			super.draw();
			if(topRegion != null && topRegion.found()){
				Draw.z(Layer.block + 0.1f);
				Draw.rect(topRegion, x, y);
			}
			if(active.size > 0){
				Draw.z(Layer.effect - 0.5f);
				for(int k = 0; k < active.size; k++){
					Slot s = slots.get(active.get(k));
					Draw.color(s.color(), 0.35f * (1f - ring));
					Lines.stroke(1.6f * (1f - ring) + 0.4f);
					Lines.circle(x, y, range * Mathf.lerp(0.15f, 1f, Mathf.pow(ring, 0.6f)) * (0.97f - 0.04f * k));
				}
				Draw.reset();
			}
		}

		@Override
		public void write(Writes write){
			super.write(write);
			write.b(burn.length);
			for(float v : burn) write.f(v);
		}

		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			if(revision >= 1){
				int n = read.b();
				for(int i = 0; i < n; i++){
					float v = read.f();
					if(i < burn.length) burn[i] = v;
				}
			}
		}

		@Override
		public byte version(){
			return 1;
		}
	}
}
