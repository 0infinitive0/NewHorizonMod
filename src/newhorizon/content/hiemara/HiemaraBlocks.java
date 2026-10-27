package newhorizon.content.hiemara;

import mindustry.content.Items;
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
	
	public static void load(){
		filterMk1 = new FilterBlock("water-filter-1"){{
			requirements(Category.production, with(Items.copper, 35, Items.lead, 25));
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
			requirements(Category.distribution, with(Items.copper, 60, Items.lead, 40));
			size = 2;
			health = 200;
			maxBound = 4;
			alwaysUnlocked = true;
		}};
		depot = new Depot("cargo-depot"){{
			requirements(Category.distribution, with(Items.copper, 50, Items.lead, 30));
			size = 2;
			health = 200;
			alwaysUnlocked = true;
		}};
		
		airPad = new CargoUnitPad("cargo-air-pad"){{
			requirements(Category.units, with(Items.copper, 100, Items.lead, 80));
			size = 3;
			health = 400;
			alwaysUnlocked = true;
			plans.add(
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
			plans.add(
				new UnitPlan(HiemaraUnits.cargoSkiff, 10f * 60f, with(Items.copper, 40, Items.lead, 30, Items.scrap, 10)),
				new UnitPlan(HiemaraUnits.cargoBarge, 20f * 60f, with(Items.copper, 90, Items.lead, 90, Items.sand, 50, Items.coal, 30)),
				new UnitPlan(HiemaraUnits.cargoCarrier, 40f * 60f, with(Items.lead, 120, Items.titanium, 100, Items.coal, 60, Items.thorium, 20))
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
	}
}
