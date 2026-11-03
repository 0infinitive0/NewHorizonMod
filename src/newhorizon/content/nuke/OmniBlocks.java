package newhorizon.content.nuke;

import arc.graphics.Color;
import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.world.Block;
import newhorizon.expand.block.omni.OmniExtractor;
import newhorizon.expand.block.omni.OmniFactory;
import newhorizon.expand.block.omni.OmniShield;
import newhorizon.expand.block.omni.OmniStabilizer;
import newhorizon.expand.units.serpent.SerpentGrowAI;
import newhorizon.expand.block.omni.OmniWall;
import newhorizon.expand.block.omni.SerpentNest;
import mindustry.world.blocks.units.UnitFactory;

import static mindustry.Vars.tilesize;

import static mindustry.type.ItemStack.with;

/**
 * What omni-matter is for: a factory that turns it into the resources of the base game, an extractor that turns it into power, a shield
 * that is paid for in it and a wall that stores the damage it takes and gives it back as a blast. They belong to no faction and are made
 * of items every planet has, but the shield and the wall also need omni-matter to build.
 */
public class OmniBlocks{
	public static Block omniFactory, omniExtractor, omniShield, omniWall, omniStabilizer, antiMatterCannon, crabDock, modularFoundry, refitBay;
	/** Makes the head of the Serpent and its body sections. */
	public static Block serpentNest;

	public static void load(){
		omniFactory = new OmniFactory("omni-factory"){{
			requirements(Category.crafting, with(Items.silicon, 150, Items.graphite, 100, Items.thorium, 80, Items.metaglass, 60, Items.surgeAlloy, 40));
			health = 1200;
			consumePower(2f);
		}};

		omniExtractor = new OmniExtractor("omni-extractor"){{
			requirements(Category.power, with(Items.silicon, 40, Items.graphite, 30, Items.thorium, 15, Items.surgeAlloy, 10));
			health = 200;
			consumePowerBuffered(20000f);
			energyPerItem = 10000f;
		}};

		omniShield = new OmniShield("omni-shield"){{
			requirements(Category.effect, with(Items.silicon, 120, Items.thorium, 100, Items.surgeAlloy, 60, Items.phaseFabric, 40, NukeItems.omniMatter, 60));
			size = 3;
			health = 1500;
			radius = 200f;
			sides = 6;
			shieldColor = Color.valueOf("c885ff");
			pointsPerItem = 400f;
		}};

		serpentNest = new SerpentNest("serpent-nest"){{
			requirements(Category.units, with(Items.silicon, 300, Items.thorium, 250, Items.surgeAlloy, 150, Items.phaseFabric, 100, NukeItems.omniMatter, 100));
			size = 4;
			health = 2800;
			consumePower(6f);
			plans.add(new UnitFactory.UnitPlan(SerpentUnits.head, 60f * 180f, with(Items.silicon, 800, Items.thorium, 600, Items.surgeAlloy, 400, Items.plastanium, 300, NukeItems.omniMatter, 400)));
			//A section costs what the Add section command costs: omni-matter only, delivered to the Nest instead of taken from the core. Only while there is a head.
			plans.add(new UnitFactory.UnitPlan(SerpentUnits.body, 60f * 20f, with(NukeItems.omniMatter, SerpentGrowAI.cost)));
		}};

		omniStabilizer = new OmniStabilizer("omni-stabilizer"){{
			requirements(Category.effect, with(Items.silicon, 80, Items.thorium, 60, Items.phaseFabric, 40, NukeItems.omniMatter, 40));
			size = 1;
			health = 300;
			consumePower(1f);
			range = 12f * tilesize;
		}};

		//Charged by holding fire (it does not keep the charge), fired by hand only: it destroys what it hits, and the destruction spreads.
		antiMatterCannon = new newhorizon.expand.block.turrets.AntiMatterCannon("anti-matter-cannon"){{
			requirements(Category.turret, with(Items.silicon, 1500, Items.thorium, 1200, Items.surgeAlloy, 800, Items.plastanium, 600, Items.phaseFabric, 500, NukeItems.omniMatter, 600));
			size = 5;
			health = 12000;
			consumePower(80f);
			range = 760f;
			rotateSpeed = 1.6f;
			shootType = new mindustry.entities.bullet.BasicBulletType(0f, 0f);
		}};

		//Builds the King Crab, one to a team: slow and very expensive.
		crabDock = new newhorizon.expand.block.nuke.CrabDock("crab-dock"){{
			requirements(Category.units, with(Items.silicon, 1500, Items.thorium, 1500, Items.surgeAlloy, 800, Items.plastanium, 600, Items.phaseFabric, 500, NukeItems.omniMatter, 300));
			size = 7;
			health = 9000;
			consumePower(30f);
			plans.add(new UnitFactory.UnitPlan(CrabUnits.kingCrab, 60f * 300f, with(Items.silicon, 3000, Items.thorium, 2500, Items.surgeAlloy, 1500, Items.plastanium, 1200, Items.phaseFabric, 800, NukeItems.omniMatter, 800)));
		}};

		//Builds the Chimera frame and the pods that fit into it.
		modularFoundry = new UnitFactory("modular-foundry"){{
			requirements(Category.units, with(Items.silicon, 1200, Items.titanium, 1200, Items.thorium, 600, Items.plastanium, 400, Items.surgeAlloy, 300, NukeItems.omniMatter, 150));
			size = 6;
			health = 4500;
			envEnabled = mindustry.world.meta.Env.any;
			consumePower(16f);
			plans.add(new UnitFactory.UnitPlan(ModularUnits.frame, 60f * 90f, with(Items.silicon, 1500, Items.titanium, 1500, Items.thorium, 800, Items.plastanium, 500, Items.surgeAlloy, 400, NukeItems.omniMatter, 200)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.autocannon.pod, 60f * 20f, with(Items.silicon, 120, Items.titanium, 100, Items.plastanium, 30)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.lance.pod, 60f * 24f, with(Items.silicon, 140, Items.titanium, 120, Items.plastanium, 40, Items.surgeAlloy, 20)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.missile.pod, 60f * 22f, with(Items.silicon, 130, Items.titanium, 100, Items.plastanium, 40)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.shield.pod, 60f * 24f, with(Items.silicon, 150, Items.titanium, 100, Items.phaseFabric, 40)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.repair.pod, 60f * 22f, with(Items.silicon, 140, Items.titanium, 100, Items.phaseFabric, 30)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.overdrive.pod, 60f * 24f, with(Items.silicon, 150, Items.thorium, 80, Items.phaseFabric, 40)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.plating.pod, 60f * 20f, with(Items.silicon, 100, Items.titanium, 200, Items.thorium, 60)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.servos.pod, 60f * 20f, with(Items.silicon, 160, Items.titanium, 100, Items.plastanium, 20)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.overcharge.pod, 60f * 22f, with(Items.silicon, 150, Items.thorium, 100, Items.surgeAlloy, 30)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.howitzer.pod, 60f * 30f, with(Items.silicon, 180, Items.titanium, 200, Items.thorium, 120, Items.plastanium, 60)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.siege.pod, 60f * 32f, with(Items.silicon, 200, Items.titanium, 160, Items.plastanium, 80, Items.surgeAlloy, 60)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.beam.pod, 60f * 30f, with(Items.silicon, 200, Items.thorium, 120, Items.plastanium, 60, Items.surgeAlloy, 60)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.tractor.pod, 60f * 24f, with(Items.silicon, 150, Items.titanium, 120, Items.phaseFabric, 40)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.tesla.pod, 60f * 22f, with(Items.silicon, 140, Items.titanium, 100, Items.surgeAlloy, 40)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.pulse.pod, 60f * 22f, with(Items.silicon, 140, Items.titanium, 140, Items.plastanium, 40)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.mender.pod, 60f * 22f, with(Items.silicon, 140, Items.titanium, 100, Items.phaseFabric, 30)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.flak.pod, 60f * 20f, with(Items.silicon, 130, Items.titanium, 100, Items.plastanium, 30)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.sword.pod, 60f * 24f, with(Items.silicon, 150, Items.thorium, 120, Items.surgeAlloy, 50)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.blink.pod, 60f * 24f, with(Items.silicon, 180, Items.titanium, 100, Items.phaseFabric, 60)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.jump.pod, 60f * 40f, with(Items.silicon, 300, Items.thorium, 150, Items.phaseFabric, 140, Items.surgeAlloy, 80)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.cloak.pod, 60f * 30f, with(Items.silicon, 240, Items.titanium, 100, Items.phaseFabric, 100)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.pointdefense.pod, 60f * 24f, with(Items.silicon, 180, Items.titanium, 120, Items.plastanium, 50)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.arcshield.pod, 60f * 26f, with(Items.silicon, 180, Items.plastanium, 100, Items.phaseFabric, 50)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.hitcap.pod, 60f * 30f, with(Items.silicon, 200, Items.thorium, 200, Items.surgeAlloy, 80)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.laserblock.pod, 60f * 26f, with(Items.silicon, 180, Items.plastanium, 120, Items.surgeAlloy, 40)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.builder.pod, 60f * 22f, with(Items.silicon, 160, Items.titanium, 140, Items.graphite, 100)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.suppress.pod, 60f * 26f, with(Items.silicon, 170, Items.titanium, 100, Items.surgeAlloy, 50)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.sprint.pod, 60f * 22f, with(Items.silicon, 160, Items.titanium, 100, Items.plastanium, 50)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.rapid.pod, 60f * 24f, with(Items.silicon, 180, Items.titanium, 120, Items.plastanium, 60)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.emplacement.pod, 60f * 30f, with(Items.silicon, 220, Items.thorium, 160, Items.plastanium, 100, Items.surgeAlloy, 60)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.bulk.pod, 60f * 24f, with(Items.silicon, 120, Items.titanium, 260, Items.thorium, 80)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.glass.pod, 60f * 24f, with(Items.silicon, 200, Items.surgeAlloy, 100, Items.phaseFabric, 40)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.comet.pod, 60f * 26f, with(Items.silicon, 220, Items.plastanium, 100, Items.phaseFabric, 80)));
			plans.add(new UnitFactory.UnitPlan(ModularUnits.bastion.pod, 60f * 30f, with(Items.silicon, 160, Items.titanium, 300, Items.thorium, 200, Items.plastanium, 80)));
		}};

		//Takes a frame and pods in and fits the one out with the others.
		refitBay = new newhorizon.expand.block.modular.RefitBay("refit-bay"){{
			requirements(Category.units, with(Items.silicon, 800, Items.titanium, 800, Items.thorium, 400, Items.plastanium, 300, Items.surgeAlloy, 150, NukeItems.omniMatter, 100));
			health = 3500;
		}};

		omniWall = new OmniWall("omni-wall"){{
			requirements(Category.defense, with(Items.thorium, 12, Items.plastanium, 8, NukeItems.omniMatter, 8));
			health = 5000;
			maxCharge = 3000f;
			threshold = 100f;
		}};
	}
}
