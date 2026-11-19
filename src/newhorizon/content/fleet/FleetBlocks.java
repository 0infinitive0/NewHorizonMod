package newhorizon.content.fleet;

import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.world.blocks.payloads.PayloadConveyor;
import newhorizon.expand.block.fleet.FleetReconstructor;

import static mindustry.type.ItemStack.with;

/**
 * The Fleet Reconstructor, which upgrades the Mothership through all its tiers, and the payload conveyors that are big enough to carry the Motherships.
 * A payload conveyor only takes a unit up to its own size ({@code hitSize / 8} tiles at most), and the standard one is 3x3 (24 px).
 */
public class FleetBlocks{
	public static FleetReconstructor fleetReconstructor;
	public static PayloadConveyor largePayloadConveyor, hugePayloadConveyor, giantPayloadConveyor, colossalPayloadConveyor;

	public static void load(){
		fleetReconstructor = new FleetReconstructor("fleet-reconstructor"){{
			requirements(Category.units, with(Items.lead, 5000, Items.silicon, 4000, Items.titanium, 2500, Items.thorium, 1500, Items.plastanium, 1000, Items.surgeAlloy, 400));
			size = 9;
			constructTime = 60f * 60f;
			consumePower(30f);
			//Every upgrade has its own cost and time. A future upgrade is one more line.
			addUpgrade(FleetUnits.mothership, FleetUnits.mothershipII, with(Items.silicon, 1200, Items.titanium, 900, Items.thorium, 600, Items.plastanium, 500, Items.surgeAlloy, 250), 60f);
			addUpgrade(FleetUnits.mothershipII, FleetUnits.mothershipIII, with(Items.silicon, 2500, Items.titanium, 1800, Items.thorium, 1200, Items.plastanium, 1000, Items.surgeAlloy, 600), 90f);
		}};

		largePayloadConveyor = conveyor("large-payload-conveyor", 5, with(Items.graphite, 30, Items.titanium, 40, Items.silicon, 20));
		hugePayloadConveyor = conveyor("huge-payload-conveyor", 7, with(Items.graphite, 60, Items.titanium, 80, Items.silicon, 40));
		giantPayloadConveyor = conveyor("giant-payload-conveyor", 9, with(Items.graphite, 120, Items.titanium, 160, Items.silicon, 80, Items.plastanium, 40));
		colossalPayloadConveyor = conveyor("colossal-payload-conveyor", 11, with(Items.graphite, 200, Items.titanium, 300, Items.silicon, 140, Items.plastanium, 100, Items.thorium, 60));
	}

	/** A payload conveyor of an odd size (the base class aligns to a centre tile) that takes units up to its own size. */
	protected static PayloadConveyor conveyor(String name, int sizeTiles, mindustry.type.ItemStack[] cost){
		return new PayloadConveyor(name){{
			requirements(Category.units, cost);
			size = sizeTiles;
			payloadLimit = sizeTiles;
			canOverdrive = false;
		}};
	}
}
