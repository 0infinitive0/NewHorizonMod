package newhorizon.expand.block.industry;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Strings;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.content.Liquids;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.logic.LAccess;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.Env;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;

/**
 * The Slag Refinery. It turns slag, with ozone to oxidise it, into one ore at a time, which is chosen in its menu (or by a logic processor with
 * {@code control config refinery @copper}). Each ore has its own recipe: how much slag and ozone one item takes and how long it takes to make;
 * the rarer the ore, the more of both. The liquids come in through pipes, the ore is pushed out into whatever is next to it, and it needs
 * power. It makes nothing while no ore is chosen, while a liquid or the power is missing, or while its buffer of the ore is full.
 */
public class SlagRefinery extends Block{
	/** What one item of an ore takes. */
	public static class Recipe{
		public final Item item;
		public final float slag, ozone, time;

		public Recipe(Item item, float slag, float ozone, float time){
			this.item = item;
			this.slag = slag;
			this.ozone = ozone;
			this.time = time;
		}
	}

	public final Seq<Recipe> recipes = new Seq<>();
	/** Crafting speed: a recipe's time is divided by this (and its slag and ozone are used that much faster). */
	public float speed = 1f;
	/** How big the chosen item is drawn on the block (world units): a small badge, not half the block. */
	public float iconSize = 6f;
	public Color color = Color.valueOf("ff9a4a");
	public TextureRegion topRegion;

	public static final Stat recipeStat = new Stat("nh-refinery-recipes", StatCat.crafting);

	public SlagRefinery(String name){
		super(name);
		size = 3;
		update = true;
		solid = true;
		hasItems = true;
		hasLiquids = true;
		hasPower = true;
		configurable = true;
		logicConfigurable = true;
		saveConfig = true;
		clearOnDoubleTap = true;
		itemCapacity = 20;
		liquidCapacity = 600f;
		group = BlockGroup.none;
		envEnabled = Env.any;
		buildType = SlagRefineryBuild::new;

		config(Item.class, (SlagRefineryBuild b, Item item) -> b.choose(item));
		configClear((SlagRefineryBuild b) -> b.choose(null));
	}

	/** Adds an ore it can make. */
	public SlagRefinery recipe(Item item, float slag, float ozone, float time){
		recipes.add(new Recipe(item, slag, ozone, time));
		return this;
	}

	public Recipe recipeFor(Item item){
		return item == null ? null : recipes.find(r -> r.item == item);
	}

	public Seq<Item> ores(){
		return recipes.map(r -> r.item);
	}

	@Override
	public void load(){
		super.load();
		topRegion = Core.atlas.find(name + "-top");
	}

	@Override
	public void setStats(){
		super.setStats();
		stats.add(recipeStat, table -> {
			table.row();
			for(Recipe r : recipes){
				table.table(t -> {
					t.left();
					t.image(r.item.uiIcon).size(24f).padRight(6f);
					t.add(Core.bundle.format("stat.nh-refinery-recipes.value", r.item.localizedName, Strings.autoFixed(r.slag, 1), Strings.autoFixed(r.ozone, 1),
						Strings.autoFixed(r.time / speed / 60f, 2))).left();
				}).left().padBottom(2f).row();
			}
		});
	}

	@Override
	public void setBars(){
		super.setBars();
		addBar("nh-refinery-craft", (SlagRefineryBuild b) -> new Bar(
			() -> b.target == null ? Core.bundle.get("nh.refinery.none") : Core.bundle.format("nh.refinery.making", b.target.localizedName),
			() -> Pal.accent,
			() -> b.progress));
		addBar("nh-refinery-slag", (SlagRefineryBuild b) -> new Bar(
			() -> Liquids.slag.localizedName,
			() -> Liquids.slag.barColor(),
			() -> b.liquids.get(Liquids.slag) / liquidCapacity));
		addBar("nh-refinery-ozone", (SlagRefineryBuild b) -> new Bar(
			() -> Liquids.ozone.localizedName,
			() -> Liquids.ozone.barColor(),
			() -> b.liquids.get(Liquids.ozone) / liquidCapacity));
	}

	public class SlagRefineryBuild extends Building{
		/** The ore it is making, or null. */
		public Item target;
		public float progress, warmup;

		public void choose(Item item){
		 	//An item it cannot make is ignored; nothing at all clears the choice.
			if(item != null && recipeFor(item) == null) return;
			if(item == target) return;
			target = item;
			progress = 0f;
		}

		public Recipe recipe(){
			return recipeFor(target);
		}

		@Override
		public boolean acceptItem(Building source, Item item){
			return false;
		}

		@Override
		public boolean acceptLiquid(Building source, Liquid liquid){
			return (liquid == Liquids.slag || liquid == Liquids.ozone) && liquids.get(liquid) < liquidCapacity - 0.001f;
		}

		/** Whether it can work now: an ore is chosen, there is room for it, and both liquids are there for one item. */
		@Override
		public boolean shouldConsume(){
			Recipe r = recipe();
			return r != null && items.get(r.item) < itemCapacity && liquids.get(Liquids.slag) >= r.slag - 0.0001f && liquids.get(Liquids.ozone) >= r.ozone - 0.0001f;
		}

		@Override
		public void updateTile(){
			Recipe r = recipe();
			if(r != null && shouldConsume() && efficiency > 0f){
				progress += edelta() * speed / r.time;
				warmup = Mathf.lerpDelta(warmup, 1f, 0.05f);
			}else{
				warmup = Mathf.lerpDelta(warmup, 0f, 0.05f);
			}

			if(r != null && progress >= 1f){
				progress = 0f;
				if(liquids.get(Liquids.slag) >= r.slag - 0.0001f && liquids.get(Liquids.ozone) >= r.ozone - 0.0001f && items.get(r.item) < itemCapacity){
					liquids.remove(Liquids.slag, r.slag);
					liquids.remove(Liquids.ozone, r.ozone);
					items.add(r.item, 1);
					Fx.pulverizeMedium.at(x, y, 0f, color);
				}
			}

			for(Item item : Vars.content.items()){
				if(items.has(item)) dump(item);
			}
		}

		@Override
		public Object config(){
			return target;
		}

		@Override
		public Object senseObject(LAccess sensor){
			if(sensor == LAccess.config) return target;
			return super.senseObject(sensor);
		}

		@Override
		public double sense(LAccess sensor){
			if(sensor == LAccess.progress) return progress;
			return super.sense(sensor);
		}

		@Override
		public void buildConfiguration(Table table){
			ItemSelection.buildTable(SlagRefinery.this, table, ores(), () -> target, this::configure);
		}

		@Override
		public void draw(){
			Draw.rect(region, x, y);
			if(warmup > 0.01f && topRegion != null && topRegion.found()){
				Draw.z(Layer.blockOver);
				Draw.color(color, warmup * (0.55f + 0.45f * Mathf.absin(Time.time, 9f, 1f)));
				Draw.rect(topRegion, x, y);
				Draw.color();
			}
			if(target != null && target.fullIcon != null){
				Draw.z(Layer.blockOver + 0.1f);
				Draw.rect(target.fullIcon, x, y, iconSize, iconSize);
			}
		}

		@Override
		public void write(Writes write){
			super.write(write);
			write.s(target == null ? -1 : target.id);
			write.f(progress);
		}

		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			int id = read.s();
			target = id < 0 ? null : Vars.content.item(id);
			progress = read.f();
		}
	}
}
