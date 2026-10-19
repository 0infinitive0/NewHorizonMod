package newhorizon.content.nuke;

import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.meta.Env;
import newhorizon.expand.block.nuke.MineBlock;
import newhorizon.expand.block.nuke.NukeLauncher;

import static mindustry.type.ItemStack.with;

/**
 * The two nuke blocks. Like everything cross-planet in this mod they are made only of items every planet has (silicon, graphite,
 * thorium and surge alloy), so they can be built anywhere.
 */
public class NukeBlocks{
	public static Block nukeBuilder, nukeLauncher;
	/** Laid by cluster nukes; cannot be built by hand. */
	public static MineBlock mine;

	public static void load(){
		//Makes inactive nukes and pushes them out of its front, into a launcher if there is one there.
		nukeBuilder = new UnitFactory("nuke-builder"){{
			requirements(Category.units, with(Items.silicon, 260, Items.graphite, 200, Items.thorium, 150));
			size = 4;
			health = 2600;
			consumePower(5f);
			envEnabled = Env.any;
			plans.addAll(
				new UnitFactory.UnitPlan(NukeUnits.explosive, 60f * 25f, with(Items.silicon, 60, Items.graphite, 90, Items.thorium, 35)),
				new UnitFactory.UnitPlan(NukeUnits.incendiary, 60f * 30f, with(Items.silicon, 60, Items.graphite, 130, Items.thorium, 45)),
				new UnitFactory.UnitPlan(NukeUnits.radiation, 60f * 35f, with(Items.silicon, 70, Items.graphite, 70, Items.thorium, 110)),
				new UnitFactory.UnitPlan(NukeUnits.emp, 60f * 35f, with(Items.silicon, 140, Items.graphite, 60, Items.surgeAlloy, 20)),
				new UnitFactory.UnitPlan(NukeUnits.psychic, 60f * 40f, with(Items.silicon, 100, Items.phaseFabric, 40, Items.surgeAlloy, 20)),
				new UnitFactory.UnitPlan(NukeUnits.gravity, 60f * 35f, with(Items.silicon, 80, Items.graphite, 80, Items.thorium, 60, Items.phaseFabric, 20)),
				new UnitFactory.UnitPlan(NukeUnits.spatial, 60f * 40f, with(Items.silicon, 100, Items.phaseFabric, 50, Items.surgeAlloy, 20)),
				new UnitFactory.UnitPlan(NukeUnits.shockwave, 60f * 30f, with(Items.silicon, 60, Items.graphite, 100, Items.thorium, 40)),
				new UnitFactory.UnitPlan(NukeUnits.cluster, 60f * 30f, with(Items.silicon, 80, Items.graphite, 120, Items.thorium, 40)),
				new UnitFactory.UnitPlan(NukeUnits.wall, 60f * 25f, with(Items.silicon, 50, Items.graphite, 80, Items.thorium, 30)),
				new UnitFactory.UnitPlan(NukeUnits.temporal, 60f * 40f, with(Items.silicon, 100, Items.phaseFabric, 40, Items.thorium, 40)),
				new UnitFactory.UnitPlan(NukeUnits.infantry, 60f * 25f, with(Items.silicon, 90, Items.graphite, 60, Items.thorium, 30))
			);
		}};

		mine = new MineBlock("nuke-mine"){{
			requirements(Category.effect, BuildVisibility.hidden, with(Items.silicon, 1));
			health = 40;
			blastRadius = 38f;
			blastDamage = 130f;
		}};

		nukeLauncher = new NukeLauncher("nuke-launcher"){{
			requirements(Category.units, with(Items.silicon, 320, Items.graphite, 240, Items.thorium, 200, Items.surgeAlloy, 40));
			health = 3200;
			consumePower(8f);
		}};
	}
}
