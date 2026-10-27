package newhorizon.content.hiemara;

import arc.struct.Seq;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.type.LiquidStack;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;

import static mindustry.type.ItemStack.with;

/**
 * Floating factories for the base game resources that the first factories of Hiemara do not make: graphite, metaglass,
 * plastanium, phase fabric, surge alloy, blast compound, oxide, carbide and the liquids slag, cryofluid, nitrogen,
 * arkycite, cyanogen and neoplasm. They run on power only: the Erekir recipes that need heat in the base game need more
 * power here instead. The arkycite fermenter and the neoplasm culture are not in the base game, which only gets those
 * liquids from pools of them and from a reactor that explodes.
 */
public class HiemaraFactories{
	public static GenericCrafter graphitePress, kiln, plastaniumCompressor, phaseWeaver, surgeSmelter, blastMixer, oxidationChamber, carbideCrucible,
		melter, cryofluidMixer, airCondenser, arkyciteFermenter, cyanogenSynthesizer, neoplasmCulture;
	
	public static final Seq<Block> all = new Seq<>();
	
	public static void load(){
		graphitePress = new GenericCrafter("floating-graphite-press"){{
			requirements(Category.crafting, with(Items.copper, 75, Items.lead, 30));
			size = 2;
			health = 260;
			outputItem = new ItemStack(Items.graphite, 1);
			craftTime = 90f;
			hasItems = true;
			placeableLiquid = true;
			consumeItem(Items.coal, 2);
		}};
		
		kiln = new GenericCrafter("floating-kiln"){{
			requirements(Category.crafting, with(Items.copper, 60, Items.lead, 40, Items.sand, 30));
			size = 2;
			health = 260;
			outputItem = new ItemStack(Items.metaglass, 1);
			craftTime = 30f;
			hasItems = hasPower = true;
			placeableLiquid = true;
			consumeItems(with(Items.lead, 1, Items.sand, 1));
			consumePower(0.6f);
		}};
		
		plastaniumCompressor = new GenericCrafter("floating-plastanium-compressor"){{
			requirements(Category.crafting, with(Items.silicon, 80, Items.lead, 115, Items.titanium, 80, Items.copper, 60));
			size = 2;
			health = 320;
			outputItem = new ItemStack(Items.plastanium, 1);
			craftTime = 60f;
			liquidCapacity = 60f;
			hasItems = hasPower = hasLiquids = true;
			placeableLiquid = true;
			consumeLiquid(Liquids.oil, 0.25f);
			consumeItem(Items.titanium, 2);
			consumePower(3f);
		}};
		
		phaseWeaver = new GenericCrafter("floating-phase-weaver"){{
			requirements(Category.crafting, with(Items.silicon, 130, Items.lead, 120, Items.thorium, 75));
			size = 2;
			health = 320;
			outputItem = new ItemStack(Items.phaseFabric, 1);
			craftTime = 120f;
			hasItems = hasPower = true;
			itemCapacity = 20;
			placeableLiquid = true;
			consumeItems(with(Items.thorium, 4, Items.sand, 10));
			consumePower(5f);
		}};
		
		surgeSmelter = new GenericCrafter("floating-surge-smelter"){{
			requirements(Category.crafting, with(Items.silicon, 80, Items.lead, 80, Items.thorium, 70, Items.titanium, 60));
			size = 3;
			health = 600;
			outputItem = new ItemStack(Items.surgeAlloy, 1);
			craftTime = 75f;
			hasItems = hasPower = true;
			itemCapacity = 20;
			placeableLiquid = true;
			consumeItems(with(Items.copper, 3, Items.lead, 4, Items.titanium, 2, Items.silicon, 3));
			consumePower(4f);
		}};
		
		blastMixer = new GenericCrafter("floating-blast-mixer"){{
			requirements(Category.crafting, with(Items.lead, 30, Items.titanium, 20, Items.silicon, 15, Items.copper, 30));
			size = 2;
			health = 240;
			outputItem = new ItemStack(Items.blastCompound, 1);
			craftTime = 40f;
			hasItems = hasPower = true;
			placeableLiquid = true;
			consumeItems(with(Items.pyratite, 1, Items.sporePod, 1));
			consumePower(0.4f);
		}};
		
		oxidationChamber = new GenericCrafter("floating-oxidation-chamber"){{
			requirements(Category.crafting, with(Items.tungsten, 120, Items.silicon, 100, Items.beryllium, 120, Items.lead, 80));
			size = 3;
			health = 600;
			outputItem = new ItemStack(Items.oxide, 1);
			craftTime = 120f;
			liquidCapacity = 40f;
			hasItems = hasPower = hasLiquids = true;
			placeableLiquid = true;
			consumeLiquid(Liquids.ozone, 2f / 60f);
			consumeItem(Items.beryllium);
			consumePower(0.5f);
		}};
		
		carbideCrucible = new GenericCrafter("floating-carbide-crucible"){{
			requirements(Category.crafting, with(Items.tungsten, 110, Items.thorium, 150, Items.silicon, 100, Items.beryllium, 60));
			size = 3;
			health = 700;
			outputItem = new ItemStack(Items.carbide, 1);
			craftTime = 60f * 2.25f / 4f;
			hasItems = hasPower = true;
			itemCapacity = 20;
			placeableLiquid = true;
			consumeItems(with(Items.tungsten, 2, Items.graphite, 3));
			consumePower(6f);
		}};
		
		melter = new GenericCrafter("floating-melter"){{
			requirements(Category.crafting, with(Items.copper, 30, Items.lead, 35, Items.coal, 30));
			size = 2;
			health = 260;
			outputLiquid = new LiquidStack(Liquids.slag, 12f / 60f);
			craftTime = 10f;
			liquidCapacity = 40f;
			hasItems = hasPower = hasLiquids = true;
			placeableLiquid = true;
			consumeItem(Items.scrap, 1);
			consumePower(1f);
		}};
		
		cryofluidMixer = new GenericCrafter("floating-cryofluid-mixer"){{
			requirements(Category.crafting, with(Items.lead, 65, Items.silicon, 40, Items.titanium, 60));
			size = 2;
			health = 300;
			outputLiquid = new LiquidStack(Liquids.cryofluid, 12f / 60f);
			craftTime = 120f;
			liquidCapacity = 36f;
			hasItems = hasPower = hasLiquids = true;
			placeableLiquid = true;
			consumeLiquid(Liquids.water, 12f / 60f);
			consumeItem(Items.titanium);
			consumePower(1f);
		}};
		
		airCondenser = new GenericCrafter("floating-air-condenser"){{
			requirements(Category.crafting, with(Items.beryllium, 180, Items.silicon, 150, Items.tungsten, 40, Items.lead, 100));
			size = 3;
			health = 600;
			outputLiquid = new LiquidStack(Liquids.nitrogen, 16f / 60f);
			liquidCapacity = 60f;
			hasPower = hasLiquids = true;
			placeableLiquid = true;
			consumePower(4f);
		}};
		
		arkyciteFermenter = new GenericCrafter("floating-arkycite-fermenter"){{
			requirements(Category.crafting, with(Items.lead, 100, Items.silicon, 60, Items.beryllium, 80));
			size = 2;
			health = 320;
			outputLiquid = new LiquidStack(Liquids.arkycite, 40f / 60f);
			craftTime = 60f;
			liquidCapacity = 60f;
			hasItems = hasPower = hasLiquids = true;
			placeableLiquid = true;
			consumeLiquid(Liquids.water, 18f / 60f);
			consumeItem(Items.sporePod);
			consumePower(1.5f);
		}};
		
		cyanogenSynthesizer = new GenericCrafter("floating-cyanogen-synthesizer"){{
			requirements(Category.crafting, with(Items.tungsten, 80, Items.silicon, 120, Items.beryllium, 140, Items.thorium, 40));
			size = 3;
			health = 600;
			outputLiquid = new LiquidStack(Liquids.cyanogen, 12f / 60f);
			craftTime = 20f;
			liquidCapacity = 60f;
			hasItems = hasPower = hasLiquids = true;
			placeableLiquid = true;
			consumeLiquid(Liquids.arkycite, 160f / 60f);
			consumeItem(Items.graphite);
			consumePower(4f);
		}};
		
		neoplasmCulture = new GenericCrafter("floating-neoplasm-culture"){{
			requirements(Category.crafting, with(Items.tungsten, 150, Items.silicon, 180, Items.beryllium, 150, Items.thorium, 100));
			size = 3;
			health = 700;
			outputLiquid = new LiquidStack(Liquids.neoplasm, 20f / 60f);
			craftTime = 180f;
			liquidCapacity = 80f;
			hasItems = hasPower = hasLiquids = true;
			placeableLiquid = true;
			consumeLiquids(LiquidStack.with(Liquids.arkycite, 80f / 60f, Liquids.water, 10f / 60f));
			consumeItem(Items.phaseFabric);
			consumePower(3f);
		}};
		
		all.addAll(graphitePress, kiln, plastaniumCompressor, phaseWeaver, surgeSmelter, blastMixer, oxidationChamber, carbideCrucible,
			melter, cryofluidMixer, airCondenser, arkyciteFermenter, cyanogenSynthesizer, neoplasmCulture);
	}
}
