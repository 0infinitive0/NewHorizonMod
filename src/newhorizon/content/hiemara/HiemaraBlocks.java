package newhorizon.content.hiemara;

import arc.struct.Seq;
import mindustry.content.Items;
import mindustry.type.LiquidStack;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.content.Liquids;
import mindustry.world.blocks.liquid.LiquidRouter;
import mindustry.world.blocks.power.Battery;
import mindustry.world.blocks.power.SolarGenerator;
import mindustry.world.blocks.production.Pump;
import newhorizon.content.NHItems;
import newhorizon.content.NHUnitTypes;
import newhorizon.expand.block.hiemara.FusionReactor;
import newhorizon.expand.block.hiemara.HydroGenerator;
import newhorizon.expand.block.hiemara.WindTurbine;
import newhorizon.expand.block.production.SandDredger;
import newhorizon.expand.block.production.ice.IceMiningStation;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.blocks.units.UnitFactory.UnitPlan;
import newhorizon.expand.block.cargo.*;
import newhorizon.expand.block.hiemara.FloatingGenerator;
import newhorizon.expand.block.hiemara.FloatingHeater;
import newhorizon.expand.block.hiemara.FloatingNode;
import newhorizon.expand.block.production.filter.FilterBlock;

import static mindustry.type.ItemStack.with;

/** Everything that can be built on Hiemara. All of it can be placed on deep water. */
public class HiemaraBlocks{
	public static FilterBlock filterMk1, filterMk2, filterMk3, filterMk4;
	public static Port port;
	public static Depot depot;
	public static CargoUnitPad airPad, boatYard;
	public static CargoCapBlock hangar, dock;
	public static FloatingHeater heater;
	public static FloatingGenerator generator;
	public static FloatingNode node;
	
	public static ImprovedPort improvedPort1, improvedPort2, improvedPort3;
	public static Distributor distributor1, distributor2, distributor3;
	public static LiquidPort liquidPort;
	public static LiquidDepot liquidDepot;
	public static SandDredger sandDredger;
	public static IceMiningStation iceHarvester;
	
	public static WindTurbine windTurbine;
	public static HydroGenerator hydroGenerator;
	public static SolarGenerator floatingSolar;
	public static Battery battery1, battery2, battery3;
	public static FloatingNode nodeLarge, surgeTower;
	public static GenericCrafter electricSeparator;
	public static FusionReactor fusionReactor;
	public static Pump floatingPump, floatingRotaryPump;
	public static LiquidRouter liquidContainer, liquidTank;
	public static GenericCrafter sporeCultivator, floatingSporePress, floatingCoalCentrifuge, floatingPyratiteMixer, floatingSiliconSmelter, floatingSiliconCrucible;
	
	/** Every block of Hiemara. */
	public static final Seq<Block> all = new Seq<>();
	
	public static void load(){
		filterMk1 = new FilterBlock("water-filter-1"){{
			requirements(Category.production, with(Items.copper, 8, Items.lead, 4));
			size = 2;
			tier = 1;
			baseRate = 0.010f;
			health = 120;
			alwaysUnlocked = true;
		}};
		filterMk2 = new FilterBlock("water-filter-2"){{
			requirements(Category.production, with(Items.copper, 90, Items.lead, 70, Items.sand, 40));
			size = 3;
			tier = 2;
			baseRate = 0.012f;
			health = 300;
		}};
		filterMk3 = new FilterBlock("water-filter-3"){{
			requirements(Category.production, with(Items.copper, 140, Items.lead, 110, Items.sand, 80, Items.coal, 50));
			size = 3;
			tier = 3;
			baseRate = 0.014f;
			health = 420;
		}};
		filterMk4 = new FilterBlock("water-filter-4"){{
			requirements(Category.production, with(Items.lead, 200, Items.titanium, 120, Items.coal, 100, Items.sand, 120));
			size = 4;
			tier = 5;
			baseRate = 0.016f;
			health = 640;
		}};
		
		port = new Port("cargo-port"){{
			requirements(Category.distribution, with(Items.copper, 6, Items.lead, 3));
			size = 1;
			health = 200;
			maxBound = 4;
			alwaysUnlocked = true;
		}};
		depot = new Depot("cargo-depot"){{
			requirements(Category.distribution, with(Items.copper, 5, Items.lead, 2));
			size = 1;
			health = 200;
			alwaysUnlocked = true;
		}};
		
		airPad = new CargoUnitPad("cargo-air-pad"){{
			requirements(Category.units, with(Items.copper, 100, Items.lead, 80));
			size = 3;
			health = 400;
			alwaysUnlocked = true;
			plans.addAll(
				new UnitPlan(HiemaraUnits.cargoDrone, 8f * 60f, with(Items.copper, 25, Items.lead, 20)),
				new UnitPlan(HiemaraUnits.cargoHauler, 16f * 60f, with(Items.copper, 60, Items.lead, 60, Items.sand, 40, Items.coal, 20)),
				new UnitPlan(HiemaraUnits.cargoFreighter, 30f * 60f, with(Items.lead, 80, Items.titanium, 80, Items.sand, 60, Items.coal, 40))
			);
		}};
		boatYard = new CargoUnitPad("cargo-boat-yard"){{
			requirements(Category.units, with(Items.copper, 120, Items.lead, 90));
			size = 3;
			health = 400;
			alwaysUnlocked = true;
			plans.addAll(
				new UnitPlan(HiemaraUnits.cargoSkiff, 10f * 60f, with(Items.copper, 40, Items.lead, 30, Items.scrap, 10)),
				new UnitPlan(HiemaraUnits.cargoBarge, 20f * 60f, with(Items.copper, 90, Items.lead, 90, Items.sand, 50, Items.coal, 30)),
				new UnitPlan(HiemaraUnits.cargoCarrier, 40f * 60f, with(Items.lead, 120, Items.titanium, 100, Items.coal, 60, Items.thorium, 20)),
				new UnitPlan(HiemaraUnits.cargoGalleon, 60f * 60f, with(Items.lead, 600, Items.titanium, 250, Items.silicon, 150, Items.coal, 100)),
				new UnitPlan(HiemaraUnits.cargoLeviathan, 100f * 60f, with(Items.lead, 500, Items.titanium, 600, Items.silicon, 400, Items.thorium, 120)),
				new UnitPlan(HiemaraUnits.cargoTitan, 160f * 60f, with(Items.lead, 800, Items.titanium, 1400, Items.silicon, 900, Items.thorium, 400, Items.tungsten, 200)),
				new UnitPlan(HiemaraUnits.tankerCutter, 10f * 60f, with(Items.copper, 40, Items.lead, 40, Items.sand, 20)),
				new UnitPlan(HiemaraUnits.tankerBulk, 20f * 60f, with(Items.copper, 100, Items.lead, 120, Items.sand, 60, Items.coal, 30)),
				new UnitPlan(HiemaraUnits.tankerSuper, 36f * 60f, with(Items.lead, 200, Items.titanium, 120, Items.silicon, 40, Items.coal, 60))
			);
		}};
		
		hangar = new CargoCapBlock("cargo-hangar"){{
			requirements(Category.units, with(Items.copper, 120, Items.lead, 100, Items.sand, 60));
			size = 3;
			health = 500;
			cargoCap = 6;
		}};
		dock = new CargoCapBlock("cargo-dock"){{
			requirements(Category.units, with(Items.copper, 120, Items.lead, 100, Items.scrap, 60));
			size = 3;
			health = 500;
			cargoCap = 6;
		}};
		
		generator = new FloatingGenerator("floating-generator"){{
			requirements(Category.power, with(Items.copper, 60, Items.lead, 50, Items.sand, 20));
			size = 2;
			health = 160;
			powerProduction = 1.5f;
			alwaysUnlocked = true;
		}};
		node = new FloatingNode("floating-node"){{
			requirements(Category.power, with(Items.copper, 3, Items.lead, 10));
			maxNodes = 10;
			laserRange = 8f;
			alwaysUnlocked = true;
		}};
		heater = new FloatingHeater("floating-heater"){{
			requirements(Category.effect, with(Items.copper, 50, Items.lead, 70, Items.sand, 30));
			size = 2;
			health = 200;
			radius = 8f;
			consumePower(1.2f);
		}};
		
		improvedPort1 = improvedPort("improved-port-1", 3, 2000, 8f, 40, with(Items.lead, 150, Items.copper, 120, Items.sand, 100, Items.silicon, 40));
		improvedPort2 = improvedPort("improved-port-2", 3, 6000, 16f, 80, with(Items.lead, 300, Items.titanium, 120, Items.silicon, 100));
		improvedPort3 = improvedPort("improved-port-3", 4, 20000, 28f, 200, with(Items.titanium, 300, Items.silicon, 300, Items.thorium, 60));
		
		distributor1 = distributor("distributor-1", 2, 1000, 8f, 20, with(Items.copper, 120, Items.lead, 100, Items.silicon, 30));
		distributor2 = distributor("distributor-2", 3, 4000, 16f, 40, with(Items.lead, 250, Items.titanium, 100, Items.silicon, 100));
		distributor3 = distributor("distributor-3", 4, 12000, 28f, 100, with(Items.titanium, 250, Items.silicon, 250, Items.thorium, 50));
		
		liquidPort = new LiquidPort("liquid-port"){{
			requirements(Category.liquid, with(Items.copper, 60, Items.lead, 60, Items.sand, 40));
			size = 1;
			health = 200;
		}};
		liquidDepot = new LiquidDepot("liquid-depot"){{
			requirements(Category.liquid, with(Items.copper, 60, Items.lead, 60, Items.sand, 40));
			size = 1;
			health = 200;
		}};
		
		sandDredger = new SandDredger("sand-dredger"){{
			requirements(Category.production, with(Items.copper, 100, Items.lead, 80));
			size = 3;
			health = 360;
			outputItem = new ItemStack(Items.sand, 4);
			craftTime = 60f;
			hasItems = true;
			itemCapacity = 60;
			placeableLiquid = true;
		}};
		
		sporeCultivator = new GenericCrafter("floating-cultivator"){{
			requirements(Category.production, with(Items.copper, 50, Items.lead, 50, Items.sand, 30));
			size = 2;
			health = 200;
			outputItem = new ItemStack(Items.sporePod, 1);
			craftTime = 120f;
			hasItems = true;
			placeableLiquid = true;
		}};
		floatingSporePress = new GenericCrafter("floating-spore-press"){{
			requirements(Category.crafting, with(Items.lead, 70, Items.silicon, 30, Items.copper, 40));
			size = 2;
			health = 320;
			liquidCapacity = 60f;
			craftTime = 20f;
			outputLiquid = new LiquidStack(Liquids.oil, 18f / 60f);
			hasLiquids = true;
			hasPower = true;
			placeableLiquid = true;
			consumeItem(Items.sporePod, 1);
			consumePower(0.7f);
		}};
		floatingCoalCentrifuge = new GenericCrafter("floating-coal-centrifuge"){{
			requirements(Category.crafting, with(Items.titanium, 20, Items.lead, 60, Items.silicon, 30));
			size = 2;
			health = 320;
			outputItem = new ItemStack(Items.coal, 1);
			craftTime = 30f;
			hasPower = hasItems = hasLiquids = true;
			placeableLiquid = true;
			consumeLiquid(Liquids.oil, 0.1f);
			consumePower(0.7f);
		}};
		floatingPyratiteMixer = new GenericCrafter("floating-pyratite-mixer"){{
			requirements(Category.crafting, with(Items.copper, 80, Items.lead, 60));
			size = 2;
			health = 240;
			outputItem = new ItemStack(Items.pyratite, 1);
			hasItems = hasPower = true;
			placeableLiquid = true;
			consumePower(0.2f);
			consumeItems(with(Items.coal, 1, Items.lead, 2, Items.sand, 2));
		}};
		floatingSiliconSmelter = new GenericCrafter("floating-silicon-smelter"){{
			requirements(Category.crafting, with(Items.copper, 60, Items.lead, 50, Items.sand, 30));
			size = 2;
			health = 240;
			outputItem = new ItemStack(Items.silicon, 1);
			craftTime = 40f;
			hasPower = true;
			placeableLiquid = true;
			consumeItems(with(Items.coal, 1, Items.sand, 2));
			consumePower(0.5f);
		}};
		floatingSiliconCrucible = new GenericCrafter("floating-silicon-crucible"){{
			requirements(Category.crafting, with(Items.titanium, 120, Items.lead, 100, Items.silicon, 60, Items.copper, 100));
			size = 3;
			health = 700;
			outputItem = new ItemStack(Items.silicon, 8);
			craftTime = 90f;
			hasPower = true;
			itemCapacity = 30;
			placeableLiquid = true;
			consumeItems(with(Items.coal, 4, Items.sand, 6, Items.pyratite, 1));
			consumePower(4f);
		}};
		
		all.addAll(
			filterMk1, filterMk2, filterMk3, filterMk4, port, depot, airPad, boatYard, hangar, dock, heater, generator, node,
			improvedPort1, improvedPort2, improvedPort3, distributor1, distributor2, distributor3, liquidPort, liquidDepot,
			sandDredger, sporeCultivator, floatingSporePress, floatingCoalCentrifuge, floatingPyratiteMixer, floatingSiliconSmelter, floatingSiliconCrucible
		);
		
		iceHarvester = new IceMiningStation("floating-ice-harvester"){{
			requirements(Category.production, with(Items.copper, 100, Items.lead, 80, Items.silicon, 30));
			size = 3;
			health = 360;
			unitType = NHUnitTypes.iceMiner;
			maxUnits = 3;
			radius = 12 * 8f;
			consumePower(1f);
			placeableLiquid = true;
		}};
		
		windTurbine = new WindTurbine("wind-turbine"){{
			requirements(Category.power, with(Items.copper, 60, Items.lead, 40, Items.sand, 20));
			size = 2;
			health = 160;
			powerProduction = 0.6f;
		}};
		hydroGenerator = new HydroGenerator("hydro-generator"){{
			requirements(Category.power, with(Items.copper, 120, Items.lead, 100, Items.silicon, 40));
			size = 3;
			health = 360;
			powerProduction = 2.4f;
		}};
		floatingSolar = new SolarGenerator("floating-solar-panel"){{
			requirements(Category.power, with(Items.lead, 100, Items.silicon, 70, Items.copper, 60));
			size = 3;
			health = 240;
			powerProduction = 1.2f;
			placeableLiquid = true;
		}};
		
		battery1 = battery("floating-battery-1", 1, 4000f, with(Items.copper, 5, Items.lead, 20));
		battery2 = battery("floating-battery-2", 2, 20000f, with(Items.copper, 40, Items.lead, 100, Items.silicon, 30));
		battery3 = battery("floating-battery-3", 3, 100000f, with(Items.lead, 150, Items.titanium, 60, Items.silicon, 90));
		
		nodeLarge = new FloatingNode("floating-node-large"){{
			requirements(Category.power, with(Items.lead, 20, Items.titanium, 5, Items.silicon, 5));
			size = 2;
			health = 200;
			maxNodes = 15;
			laserRange = 15f;
		}};
		surgeTower = new FloatingNode("floating-surge-tower"){{
			requirements(Category.power, with(Items.lead, 100, Items.titanium, 80, Items.silicon, 80));
			size = 2;
			health = 300;
			maxNodes = 2;
			laserRange = 40f;
		}};
		
		floatingPump = new Pump("floating-pump"){{
			requirements(Category.liquid, with(Items.copper, 20, Items.lead, 15));
			pumpAmount = 7f / 60f;
			liquidCapacity = 20f;
			placeableLiquid = true;
		}};
		floatingRotaryPump = new Pump("floating-rotary-pump"){{
			requirements(Category.liquid, with(Items.copper, 70, Items.lead, 60, Items.silicon, 20, Items.titanium, 30));
			size = 2;
			pumpAmount = 0.2f;
			liquidCapacity = 80f;
			hasPower = true;
			consumePower(0.3f);
			placeableLiquid = true;
		}};
		liquidContainer = new LiquidRouter("floating-liquid-container"){{
			requirements(Category.liquid, with(Items.copper, 40, Items.lead, 60, Items.sand, 20));
			size = 2;
			solid = true;
			liquidCapacity = 700f;
			placeableLiquid = true;
		}};
		liquidTank = new LiquidRouter("floating-liquid-tank"){{
			requirements(Category.liquid, with(Items.lead, 120, Items.titanium, 40, Items.silicon, 30));
			size = 3;
			solid = true;
			health = 500;
			liquidCapacity = 1800f;
			placeableLiquid = true;
		}};
		
		electricSeparator = new GenericCrafter("electric-separator"){{
			requirements(Category.crafting, with(Items.copper, 100, Items.lead, 150, Items.silicon, 60, Items.titanium, 60));
			size = 3;
			health = 400;
			craftTime = 10f;
			itemCapacity = 0;
			liquidCapacity = 120f;
			hasLiquids = true;
			hasPower = true;
			placeableLiquid = true;
			consumeLiquid(Liquids.water, 0.5f);
			consumePower(1.5f);
			outputLiquids = LiquidStack.with(Liquids.ozone, 0.2f, Liquids.hydrogen, 0.3f);
			//Ozone nobody needs is let out into the air, so the hydrogen never waits for it.
			ignoreLiquidFullness = true;
			dumpExtraLiquid = true;
		}};
		
		fusionReactor = new FusionReactor("fusion-reactor"){{
			requirements(Category.power, with(Items.lead, 800, Items.copper, 400, Items.titanium, 600, Items.silicon, 500, Items.thorium, 200));
			size = 5;
			health = 3000;
			fuelItem = NHItems.ice;
			powerProduction = 80f;
		}};
		
		all.addAll(
			iceHarvester, windTurbine, hydroGenerator, floatingSolar, battery1, battery2, battery3, nodeLarge, surgeTower,
			floatingPump, floatingRotaryPump, liquidContainer, liquidTank, electricSeparator, fusionReactor
		);
	}
	
	private static Battery battery(String name, int blockSize, float stored, ItemStack[] cost){
		return new Battery(name){{
			requirements(Category.power, cost);
			size = blockSize;
			health = 80 * blockSize * blockSize;
			consumePowerBuffered(stored);
			placeableLiquid = true;
		}};
	}
	
	private static ImprovedPort improvedPort(String name, int blockSize, int stored, float reach, int collect, ItemStack[] cost){
		return new ImprovedPort(name){{
			requirements(Category.distribution, cost);
			size = blockSize;
			health = 150 * blockSize * blockSize;
			itemCapacity = stored;
			range = reach;
			collectPerItem = collect;
			maxBound = 4 + blockSize * 2;
		}};
	}
	
	private static Distributor distributor(String name, int blockSize, int stored, float reach, int rate, ItemStack[] cost){
		return new Distributor(name){{
			requirements(Category.distribution, cost);
			size = blockSize;
			health = 150 * blockSize * blockSize;
			itemCapacity = stored;
			range = reach;
			perSecond = rate;
		}};
	}
}
