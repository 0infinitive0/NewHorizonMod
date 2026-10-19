package newhorizon.content.nuke;

import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.meta.Env;
import newhorizon.expand.block.nuke.NukeLauncher;

import static mindustry.type.ItemStack.with;

/**
 * The two nuke blocks. Like everything cross-planet in this mod they are made only of items every planet has (silicon, graphite,
 * thorium and surge alloy), so they can be built anywhere.
 */
public class NukeBlocks{
	public static Block nukeBuilder, nukeLauncher;

	public static void load(){
		//Makes inactive nukes and pushes them out of its front, into a launcher if there is one there.
		nukeBuilder = new UnitFactory("nuke-builder"){{
			requirements(Category.units, with(Items.silicon, 260, Items.graphite, 200, Items.thorium, 150));
			size = 4;
			health = 2600;
			consumePower(5f);
			envEnabled = Env.any;
			plans.add(
				new UnitFactory.UnitPlan(NukeUnits.explosive, 60f * 25f, with(Items.silicon, 60, Items.graphite, 90, Items.thorium, 35)),
				new UnitFactory.UnitPlan(NukeUnits.incendiary, 60f * 30f, with(Items.silicon, 60, Items.graphite, 130, Items.thorium, 45)),
				new UnitFactory.UnitPlan(NukeUnits.radiation, 60f * 35f, with(Items.silicon, 70, Items.graphite, 70, Items.thorium, 110)),
				new UnitFactory.UnitPlan(NukeUnits.emp, 60f * 35f, with(Items.silicon, 140, Items.graphite, 60, Items.surgeAlloy, 20))
			);
		}};

		nukeLauncher = new NukeLauncher("nuke-launcher"){{
			requirements(Category.units, with(Items.silicon, 320, Items.graphite, 240, Items.thorium, 200, Items.surgeAlloy, 40));
			health = 3200;
			consumePower(8f);
		}};
	}
}
