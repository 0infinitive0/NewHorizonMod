package newhorizon.content.nuke;

import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.meta.Env;
import arc.graphics.Color;
import newhorizon.expand.block.nuke.DefenseField;
import newhorizon.expand.block.nuke.InterceptTurret;
import newhorizon.expand.bullets.nuke.InterceptBulletType;
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
	/** The defences: radar, cloak, decoy beacon, jammer and the turret that takes nukes. */
	public static Block trackingRadar, longRangeRadar, cloakProjector, decoyBeacon, nukeJammer, nukeInterceptor;
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
				new UnitFactory.UnitPlan(NukeUnits.siphon, 60f * 60f, with(Items.silicon, 240, Items.graphite, 180, Items.thorium, 100, Items.phaseFabric, 50, Items.surgeAlloy, 50)),
				new UnitFactory.UnitPlan(NukeUnits.quake, 60f * 35f, with(Items.silicon, 80, Items.graphite, 150, Items.thorium, 80)),
				new UnitFactory.UnitPlan(NukeUnits.scatter, 60f * 35f, with(Items.silicon, 90, Items.graphite, 100, Items.thorium, 60, Items.phaseFabric, 10)),
				new UnitFactory.UnitPlan(NukeUnits.magnet, 60f * 35f, with(Items.silicon, 90, Items.graphite, 90, Items.thorium, 60, Items.phaseFabric, 15)),
				new UnitFactory.UnitPlan(NukeUnits.hijack, 60f * 45f, with(Items.silicon, 140, Items.phaseFabric, 60, Items.surgeAlloy, 40)),
				new UnitFactory.UnitPlan(NukeUnits.scramble, 60f * 40f, with(Items.silicon, 110, Items.phaseFabric, 30, Items.surgeAlloy, 20)),
				new UnitFactory.UnitPlan(NukeUnits.sabotage, 60f * 35f, with(Items.silicon, 90, Items.graphite, 120, Items.thorium, 50, Items.surgeAlloy, 15)),
				new UnitFactory.UnitPlan(NukeUnits.breach, 60f * 40f, with(Items.silicon, 100, Items.graphite, 130, Items.thorium, 80, Items.surgeAlloy, 25)),
				new UnitFactory.UnitPlan(NukeUnits.devolve, 60f * 40f, with(Items.silicon, 100, Items.graphite, 80, Items.phaseFabric, 40, Items.surgeAlloy, 20)),
				new UnitFactory.UnitPlan(NukeUnits.decoy, 60f * 8f, with(Items.silicon, 40, Items.graphite, 50, Items.thorium, 20)),
				new UnitFactory.UnitPlan(NukeUnits.infantry, 60f * 25f, with(Items.silicon, 90, Items.graphite, 60, Items.thorium, 30))
			);
		}};

		//Ships that carry nukes are built in the naval factory.
		if(mindustry.content.Blocks.navalFactory instanceof UnitFactory){
			((UnitFactory)mindustry.content.Blocks.navalFactory).plans.add(
				new UnitFactory.UnitPlan(NukeUnits.carrier, 60f * 60f, with(Items.silicon, 320, Items.graphite, 240, Items.thorium, 160, Items.surgeAlloy, 40))
			);
		}

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

		//Takes the high altitude off enemy nukes in range, so that they can be shot at again, and marks them.
		trackingRadar = new DefenseField("tracking-radar", DefenseField.Kind.radar){{
			requirements(Category.effect, with(Items.silicon, 160, Items.graphite, 90, Items.thorium, 50));
			size = 3;
			health = 1500;
			consumePower(4f);
			range = 430f;
			color = Color.valueOf("7fd8ff");
		}};

		//A bigger radar: reaches much further, and costs and draws much more.
		longRangeRadar = new DefenseField("long-range-radar", DefenseField.Kind.radar){{
			requirements(Category.effect, with(Items.silicon, 420, Items.graphite, 260, Items.thorium, 160, Items.phaseFabric, 80, Items.surgeAlloy, 50));
			size = 4;
			health = 2600;
			consumePower(11f);
			range = 1150f;
			color = Color.valueOf("7fd8ff");
		}};

		//Hides its area from enemy launchers: no target can be picked there, and nukes already heading for one lose it.
		cloakProjector = new DefenseField("cloak-projector", DefenseField.Kind.cloak){{
			requirements(Category.effect, with(Items.silicon, 200, Items.graphite, 100, Items.phaseFabric, 60, Items.surgeAlloy, 30));
			size = 3;
			health = 1800;
			consumePower(7f);
			range = 260f;
			color = Color.valueOf("8a7bff");
		}};

		//Every enemy nuke in range flies at it, whatever it was aimed at.
		decoyBeacon = new DefenseField("decoy-beacon", DefenseField.Kind.beacon){{
			requirements(Category.effect, with(Items.silicon, 120, Items.graphite, 100, Items.thorium, 60));
			size = 2;
			health = 2400;
			consumePower(3f);
			range = 480f;
			color = Color.valueOf("ffb347");
		}};

		//Enemy nukes in range lose their target and wander at random, so that they fly past.
		nukeJammer = new DefenseField("nuke-jammer", DefenseField.Kind.jammer){{
			requirements(Category.effect, with(Items.silicon, 200, Items.graphite, 90, Items.phaseFabric, 50, Items.surgeAlloy, 40));
			size = 3;
			health = 1700;
			consumePower(8f);
			range = 380f;
			color = Color.valueOf("ff5c5c");
		}};

		//A very fast shot that takes an active nuke it hits over to its side as an inactive one; a long recharge only after a success.
		nukeInterceptor = new InterceptTurret("nuke-interceptor"){{
			requirements(Category.turret, with(Items.silicon, 300, Items.graphite, 200, Items.thorium, 150, Items.surgeAlloy, 60));
			size = 3;
			health = 2600;
			consumePower(12f);
			range = 540f;
			reload = 30f;
			rotateSpeed = 18f;
			shootCone = 10f;
			inaccuracy = 0f;
			shootType = new InterceptBulletType(26f);
			successCooldown = 60f * 25f;
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
