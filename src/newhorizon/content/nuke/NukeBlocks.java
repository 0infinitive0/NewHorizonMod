package newhorizon.content.nuke;

import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.meta.Env;
import newhorizon.expand.block.nuke.MineBlock;
import newhorizon.expand.block.nuke.MultiNukeLauncher;
import newhorizon.expand.block.nuke.NukeLauncher;
import newhorizon.expand.block.nuke.OrbitalLauncher;

import static mindustry.type.ItemStack.with;

/**
 * The two nuke blocks. Like everything cross-planet in this mod they are made only of items every planet has (silicon, graphite,
 * thorium and surge alloy), so they can be built anywhere.
 */
public class NukeBlocks{
	public static Block nukeBuilder, nukeLauncher, multiNukeLauncher, orbitalLauncher;
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
				new UnitFactory.UnitPlan(NukeUnits.overload, 60f * 40f, with(Items.silicon, 100, Items.graphite, 120, Items.thorium, 50, Items.surgeAlloy, 20)),
				new UnitFactory.UnitPlan(NukeUnits.laser, 60f * 40f, with(Items.silicon, 100, Items.graphite, 100, Items.thorium, 60, Items.surgeAlloy, 30)),
				new UnitFactory.UnitPlan(NukeUnits.swarm, 60f * 35f, with(Items.silicon, 100, Items.graphite, 140, Items.thorium, 60)),
				new UnitFactory.UnitPlan(NukeUnits.storm, 60f * 35f, with(Items.silicon, 90, Items.graphite, 110, Items.thorium, 50, Items.phaseFabric, 15)),
				new UnitFactory.UnitPlan(NukeUnits.lightning, 60f * 38f, with(Items.silicon, 120, Items.graphite, 80, Items.thorium, 40, Items.surgeAlloy, 25)),
				new UnitFactory.UnitPlan(NukeUnits.quake, 60f * 35f, with(Items.silicon, 80, Items.graphite, 150, Items.thorium, 80)),
				new UnitFactory.UnitPlan(NukeUnits.decoy, 60f * 8f, with(Items.silicon, 40, Items.graphite, 50, Items.thorium, 20)),
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

		//One nuke at a time, but it is thrown so high that nothing can target or hit it on the way: slow to arm, slow to recover.
		orbitalLauncher = new OrbitalLauncher("orbital-launcher"){{
			requirements(Category.units, with(Items.silicon, 900, Items.graphite, 600, Items.thorium, 500, Items.surgeAlloy, 200, Items.phaseFabric, 150));
			health = 4600;
			consumePower(20f);
			armTime = 240f;
			launchCooldown = 60f * 12f;
			turnSpeed = 1.2f;
		}};

		//Holds four nukes to start with and arms and fires them together; more slots can be added from its menu, up to twenty.
		multiNukeLauncher = new MultiNukeLauncher("multi-nuke-launcher"){{
			requirements(Category.units, with(Items.silicon, 700, Items.graphite, 520, Items.thorium, 440, Items.surgeAlloy, 120));
			health = 5400;
			consumePower(14f);
			slots = 4;
			maxSlots = 20;
			//Every slot costs more than the one before, and from the fifth upgrade on phase fabric is wanted too.
			upgradeCosts = new ItemStack[maxSlots - slots][];
			for(int i = 0; i < upgradeCosts.length; i++){
				upgradeCosts[i] = i < 4 ?
					with(Items.silicon, 80 + 25 * i, Items.graphite, 60 + 20 * i, Items.thorium, 50 + 20 * i, Items.surgeAlloy, 15 + 8 * i) :
					with(Items.silicon, 80 + 25 * i, Items.graphite, 60 + 20 * i, Items.thorium, 50 + 20 * i, Items.surgeAlloy, 15 + 8 * i, Items.phaseFabric, 10 * (i - 3));
			}
		}};
	}
}
