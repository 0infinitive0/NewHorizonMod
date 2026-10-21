package newhorizon.expand.block.omni;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.ctype.UnlockableContent;
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
import newhorizon.content.nuke.NukeItems;

/**
 * Turns omni-matter into any resource of the base game, one omni-matter at a time ({@link OmniRatios} says how much comes out). Which
 * resource is chosen in its menu, or by a logic processor with {@code control config factory @copper} (an item or a liquid). Items are
 * pushed out into whatever is next to it; liquids into the pipes. It needs power.
 */
public class OmniFactory extends Block{
	/** Ticks to turn one omni-matter into its resource. */
	public float craftTime = 30f;
	public Color color = Color.valueOf("c885ff");
	public TextureRegion topRegion;

	public OmniFactory(String name){
		super(name);
		size = 3;
		update = true;
		solid = true;
		hasItems = true;
		hasLiquids = true;
		hasPower = true;
		outputsLiquid = true;
		configurable = true;
		logicConfigurable = true;
		saveConfig = true;
		clearOnDoubleTap = true;
		unloadable = false;
		itemCapacity = 40;
		liquidCapacity = 600f;
		group = BlockGroup.none;
		envEnabled = Env.any;
		buildType = OmniFactoryBuild::new;

		config(Item.class, (OmniFactoryBuild b, Item item) -> b.choose(item));
		config(Liquid.class, (OmniFactoryBuild b, Liquid liquid) -> b.choose(liquid));
		configClear((OmniFactoryBuild b) -> b.choose(null));
	}

	@Override
	public void load(){
		super.load();
		topRegion = Core.atlas.find(name + "-top");
	}

	@Override
	public void setBars(){
		super.setBars();
		addBar("nh-omni-craft", (OmniFactoryBuild b) -> new Bar(
			() -> b.target == null ? Core.bundle.get("nh.omni.none") : Core.bundle.format("nh.omni.making", b.target.localizedName),
			() -> Pal.accent,
			() -> b.progress
		));
	}

	public class OmniFactoryBuild extends Building{
		/** What it is making: an item or a liquid, or nothing. */
		public UnlockableContent target;
		public float progress, warmup;

		public void choose(UnlockableContent content){
			//Something that cannot be made is ignored; nothing at all clears the choice.
			if(content != null && !OmniRatios.allowed(content)) return;
			if(content == target) return;
			target = content;
			progress = 0f;
		}

		/** How many pieces, or units, one omni-matter makes of the target. */
		public float amount(){
			return target == null ? 0f : OmniRatios.perOmni(target);
		}

		@Override
		public boolean acceptItem(Building source, Item item){
			return item == NukeItems.omniMatter && items.get(item) < itemCapacity;
		}

		/** Whether what one omni-matter makes would fit. */
		public boolean room(){
			if(target instanceof Item) return items.get((Item)target) + Math.round(amount()) <= itemCapacity;
			if(target instanceof Liquid){
				Liquid liquid = (Liquid)target;
				return liquids.get(liquid) + amount() <= liquidCapacity && (liquids.current() == liquid || liquids.currentAmount() < 0.1f);
			}
			return false;
		}

		@Override
		public boolean shouldConsume(){
			return target != null && items.has(NukeItems.omniMatter) && room();
		}

		@Override
		public void updateTile(){
			if(shouldConsume() && efficiency > 0f){
				progress += edelta() / craftTime;
				warmup = Mathf.lerpDelta(warmup, 1f, 0.05f);
			}else{
				warmup = Mathf.lerpDelta(warmup, 0f, 0.05f);
			}

			if(progress >= 1f){
				progress = 0f;
				craft();
			}
			dumpOutputs();
		}

		protected void craft(){
			if(target == null || !items.has(NukeItems.omniMatter) || !room()) return;
			items.remove(NukeItems.omniMatter, 1);
			if(target instanceof Item) items.add((Item)target, Math.round(amount()));
			else liquids.add((Liquid)target, amount());
			Fx.pulverizeMedium.at(x, y, 0f, color);
		}

		/** Pushes what has been made out: every item but the omni-matter, and the liquid. */
		protected void dumpOutputs(){
			for(Item item : Vars.content.items()){
				if(item != NukeItems.omniMatter && items.has(item)) dump(item);
			}
			if(liquids.currentAmount() > 0.01f) dumpLiquid(liquids.current(), 2f);
		}

		@Override
		public boolean canDump(Building to, Item item){
			return item != NukeItems.omniMatter;
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
			ItemSelection.buildTable(OmniFactory.this, table, OmniRatios.outputs(), () -> target, this::configure);
		}

		@Override
		public void draw(){
			Draw.rect(region, x, y);
			if(warmup > 0.01f && topRegion != null && topRegion.found()){
				Draw.z(Layer.blockOver);
				Draw.color(color, warmup * (0.55f + 0.45f * Mathf.absin(Time.time, 8f, 1f)));
				Draw.rect(topRegion, x, y);
				Draw.color();
			}
			if(target != null && target.fullIcon != null){
				Draw.z(Layer.blockOver + 0.1f);
				Draw.rect(target.fullIcon, x, y, 12f, 12f);
			}
		}

		@Override
		public void write(Writes write){
			super.write(write);
			write.b(target instanceof Item ? 1 : target instanceof Liquid ? 2 : 0);
			write.s(target == null ? -1 : target.id);
			write.f(progress);
		}

		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			int kind = read.b(), id = read.s();
			target = kind == 1 ? Vars.content.item(id) : kind == 2 ? Vars.content.liquid(id) : null;
			progress = read.f();
		}
	}
}
