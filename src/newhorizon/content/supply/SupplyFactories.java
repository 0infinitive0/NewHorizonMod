package newhorizon.content.supply;

import arc.graphics.Color;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.entities.Effect;
import mindustry.gen.Sounds;
import mindustry.type.Category;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.draw.DrawDefault;
import mindustry.world.draw.DrawFlame;
import mindustry.world.draw.DrawMulti;

import static mindustry.type.ItemStack.with;

/**
 * Better versions of the base game's Plastanium Compressor, Phase Weaver and Surge Smelter, made of and fed with base game items. Each has the base
 * block's crafting time, so it makes exactly 3 times (Advanced) or 8 times (Ultimate) as much; the Ultimate uses a quarter less input for each item.
 * The Surge Crucible makes surge alloy from coal and slag instead, 1.5 times as much as the Ultimate Surge Smelter, and uses a great deal of power.
 */
public class SupplyFactories{
	public static Block advancedPlastanium, ultimatePlastanium, advancedPhase, ultimatePhase, advancedSurge, ultimateSurge, surgeCrucible;

	public static void load(){
		//Plastanium: the base Compressor takes 2 titanium and 0.25 oil a tick for 1 plastanium a second.
		advancedPlastanium = crafter("advanced-plastanium-compressor", 3, 60f, Items.plastanium, 3, with(Items.titanium, 6), Liquids.oil, 0.75f, 10f, 900,
			with(Items.lead, 300, Items.graphite, 200, Items.titanium, 250, Items.silicon, 250, Items.thorium, 80), Fx.formsmoke, Color.valueOf("b8e07a"));
		ultimatePlastanium = crafter("ultimate-plastanium-compressor", 4, 60f, Items.plastanium, 8, with(Items.titanium, 12), Liquids.oil, 1.5f, 26f, 1600,
			with(Items.lead, 600, Items.graphite, 400, Items.titanium, 500, Items.silicon, 500, Items.thorium, 200, Items.plastanium, 150, Items.surgeAlloy, 80), Fx.formsmoke, Color.valueOf("c8f08a"));

		//Phase fabric: the base Weaver takes 4 thorium and 10 sand every 2 seconds for 1.
		advancedPhase = crafter("advanced-phase-weaver", 3, 120f, Items.phaseFabric, 3, with(Items.thorium, 12, Items.sand, 30), null, 0f, 17f, 900,
			with(Items.lead, 300, Items.thorium, 200, Items.silicon, 300, Items.plastanium, 60), Fx.smeltsmoke, Color.valueOf("ffc272"));
		ultimatePhase = crafter("ultimate-phase-weaver", 4, 120f, Items.phaseFabric, 8, with(Items.thorium, 24, Items.sand, 60), null, 0f, 44f, 1600,
			with(Items.lead, 600, Items.thorium, 500, Items.silicon, 600, Items.plastanium, 200, Items.phaseFabric, 150, Items.surgeAlloy, 100), Fx.smeltsmoke, Color.valueOf("ffd08c"));

		//Surge alloy: the base Smelter takes 3 copper, 4 lead, 2 titanium and 3 silicon every 1.25 seconds for 1.
		advancedSurge = crafter("advanced-surge-smelter", 4, 75f, Items.surgeAlloy, 3, with(Items.copper, 9, Items.lead, 12, Items.titanium, 6, Items.silicon, 9), null, 0f, 13f, 1600,
			with(Items.lead, 300, Items.thorium, 180, Items.silicon, 300, Items.plastanium, 100), Fx.smeltsmoke, Color.valueOf("f3e979"));
		ultimateSurge = crafter("ultimate-surge-smelter", 5, 75f, Items.surgeAlloy, 8, with(Items.copper, 18, Items.lead, 24, Items.titanium, 12, Items.silicon, 18), null, 0f, 35f, 2600,
			with(Items.lead, 600, Items.thorium, 450, Items.silicon, 600, Items.plastanium, 200, Items.phaseFabric, 120, Items.surgeAlloy, 150), Fx.smeltsmoke, Color.valueOf("fff08a"));

		//Coal and slag in, 12 surge alloy a craft (1.5 times the Ultimate Smelter's 8) out, and a great deal of power.
		surgeCrucible = crafter("slag-surge-crucible", 6, 75f, Items.surgeAlloy, 12, with(Items.coal, 12), Liquids.slag, 2.4f, 90f, 4000,
			with(Items.silicon, 1500, Items.graphite, 800, Items.thorium, 700, Items.plastanium, 300, Items.phaseFabric, 250, Items.surgeAlloy, 300), Fx.smeltsmoke, Color.valueOf("ff9a3c"));
	}

	/**
	 * (The names of the parameters differ from the fields' on purpose: inside the block a field's name would hide the parameter.)
	 * @param inputs what one craft uses; {@code fluid} (may be null) a tick's worth of liquid
	 */
	private static Block crafter(String id, int sz, float time, Item out, int amount, ItemStack[] inputs, Liquid fluid, float fluidPerTick, float powerPerTick, int hitpoints,
								 ItemStack[] costs, Effect effect, Color flame){
		int biggest = 0;
		for(ItemStack s : inputs) biggest = Math.max(biggest, s.amount);
		int cap = Math.max(30, biggest * 5);
		float liquidCap = fluid == null ? 0f : Math.max(120f, fluidPerTick * 160f);
		return new GenericCrafter(id){{
			requirements(Category.crafting, costs);
			size = sz;
			health = hitpoints;
			craftTime = time;
			outputItem = new ItemStack(out, amount);
			itemCapacity = cap;
			hasPower = hasItems = true;
			consumeItems(inputs);
			if(fluid != null){
				hasLiquids = true;
				liquidCapacity = liquidCap;
				consumeLiquid(fluid, fluidPerTick);
			}
			consumePower(powerPerTick);
			craftEffect = effect;
			updateEffect = effect;
			drawer = new DrawMulti(new DrawDefault(), new DrawFlame(flame));
			ambientSound = Sounds.loopSmelter;
			ambientSoundVolume = 0.1f;
		}};
	}
}
