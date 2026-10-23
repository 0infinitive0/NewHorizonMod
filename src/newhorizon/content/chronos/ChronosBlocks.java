package newhorizon.content.chronos;

import mindustry.content.Items;
import mindustry.gen.Sounds;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.defense.OverdriveProjector;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.PowerTurret;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.draw.DrawDefault;
import mindustry.world.draw.DrawMulti;
import mindustry.world.draw.DrawRegion;
import mindustry.world.meta.Env;
import newhorizon.expand.block.chronos.ChronoRelay;
import newhorizon.expand.block.chronos.ChronoWall;
import newhorizon.expand.block.chronos.StasisTower;
import newhorizon.expand.block.chronos.TimeDilationField;
import newhorizon.expand.block.chronos.WarpPad;

import static mindustry.type.ItemStack.with;

/**
 * Every Chronos block is built from silicon, graphite, thorium and phase fabric and from the two Chronos items, which are
 * themselves made from silicon and graphite: nothing needs an item that only one planet has, so it can be built on any of them.
 */
public class ChronosBlocks{
	public static Block
		temporalFoundry, paradoxForge,
		chronoWall, chronoWallLarge,
		stasisTower, dilationField, chronoAccelerator,
		arcFreezer, dissonanceArray, eventHorizon,
		chronoRelay, warpPad,
		temporalFabricator;

	public static void load(){
		//Resource chain: silicon + graphite -> chronite; chronite + phase fabric + thorium -> paradox core
		temporalFoundry = new GenericCrafter("temporal-foundry"){{
			requirements(Category.crafting, with(Items.silicon, 70, Items.graphite, 60));
			size = 2;
			health = 320;
			craftTime = 60f;
			outputItem = new ItemStack(ChronosItems.chronite, 1);
			consumeItems(with(Items.silicon, 2, Items.graphite, 1));
			consumePower(1.2f);
			craftEffect = ChronosFx.sparkBurst;
			drawer = new DrawMulti(new DrawDefault(), new DrawRegion("-top"));
			envEnabled = Env.any;
		}};

		paradoxForge = new GenericCrafter("paradox-forge"){{
			requirements(Category.crafting, with(Items.silicon, 150, Items.graphite, 110, Items.thorium, 60, ChronosItems.chronite, 80));
			size = 3;
			health = 840;
			craftTime = 150f;
			outputItem = new ItemStack(ChronosItems.paradoxCore, 1);
			consumeItems(with(ChronosItems.chronite, 3, Items.phaseFabric, 1, Items.thorium, 1));
			consumePower(3.5f);
			craftEffect = ChronosFx.goldBurst;
			drawer = new DrawMulti(new DrawDefault(), new DrawRegion("-top"));
			envEnabled = Env.any;
		}};

		//Walls
		chronoWall = new ChronoWall("chrono-wall"){{
			requirements(Category.defense, with(ChronosItems.chronite, 6, Items.silicon, 4));
			health = 520;
			restoreDelay = 60f * 6f;
			envEnabled = Env.any;
		}};

		chronoWallLarge = new ChronoWall("chrono-wall-large"){{
			requirements(Category.defense, with(ChronosItems.chronite, 24, Items.silicon, 16));
			size = 2;
			health = 520 * 4;
			restoreDelay = 60f * 6f;
			envEnabled = Env.any;
		}};

		//Stops time
		stasisTower = new StasisTower("stasis-tower"){{
			requirements(Category.effect, with(Items.silicon, 160, Items.graphite, 120, ChronosItems.chronite, 110, Items.thorium, 50));
			size = 3;
			health = 1100;
			range = 150f;
			consumePower(6f);
		}};

		dilationField = new TimeDilationField("time-dilation-field"){{
			requirements(Category.effect, with(Items.silicon, 130, Items.graphite, 90, ChronosItems.chronite, 90, Items.thorium, 40));
			size = 3;
			health = 900;
			range = 150f;
			slow = 0.25f;
			consumePower(3.5f);
		}};

		chronoAccelerator = new OverdriveProjector("chrono-accelerator"){{
			requirements(Category.effect, with(Items.silicon, 250, Items.graphite, 160, ChronosItems.chronite, 150, ChronosItems.paradoxCore, 50, Items.thorium, 80));
			size = 4;
			health = 1500;
			reload = 60f;
			range = 300f;
			speedBoost = 3.4f;
			speedBoostPhase = 1.2f;
			useTime = 420f;
			phaseRangeBoost = 80f;
			hasBoost = true;
			baseColor = ChronosColor.chrono;
			phaseColor = ChronosColor.gold;
			consumePower(18f);
			consumeItem(ChronosItems.paradoxCore).boost();
			envEnabled = Env.any;
		}};

		//Turrets
		arcFreezer = new PowerTurret("arc-freezer"){{
			requirements(Category.turret, with(Items.silicon, 90, Items.graphite, 70, ChronosItems.chronite, 70));
			size = 2;
			health = 700;
			range = 170f;
			reload = 42f;
			rotateSpeed = 8f;
			recoil = 1.5f;
			inaccuracy = 2f;
			shootType = ChronosBullets.arcBolt;
			shootSound = Sounds.shootArc;
			outlineColor = ChronosColor.outline;
			consumePower(4f);
			coolant = consumeCoolant(0.15f);
			envEnabled = Env.any;
		}};

		dissonanceArray = new PowerTurret("dissonance-array"){{
			requirements(Category.turret, with(Items.silicon, 170, Items.graphite, 110, ChronosItems.chronite, 120, Items.thorium, 70));
			size = 3;
			health = 1500;
			range = 235f;
			reload = 70f;
			rotateSpeed = 5f;
			recoil = 2.5f;
			shootType = ChronosBullets.dissonanceShot;
			shootSound = Sounds.shootLaser;
			outlineColor = ChronosColor.outline;
			targetAir = false;
			consumePower(6.5f);
			coolant = consumeCoolant(0.2f);
			envEnabled = Env.any;
		}};

		eventHorizon = new ItemTurret("event-horizon"){{
			requirements(Category.turret, with(Items.silicon, 300, Items.graphite, 200, ChronosItems.chronite, 200, ChronosItems.paradoxCore, 80, Items.thorium, 150));
			size = 4;
			health = 3200;
			range = 430f;
			minRange = 70f;
			reload = 60f * 7f;
			rotateSpeed = 2f;
			recoil = 5f;
			shake = 3f;
			targetAir = false;
			shootSound = Sounds.shootArtillery;
			outlineColor = ChronosColor.outline;
			consumePower(9f);
			coolant = consumeCoolant(0.3f);
			envEnabled = Env.any;

			ammo(ChronosItems.paradoxCore, ChronosBullets.blackHoleShell);
		}};

		//Teleportation
		chronoRelay = new ChronoRelay("chrono-relay"){{
			requirements(Category.distribution, with(Items.silicon, 60, Items.graphite, 40, ChronosItems.chronite, 50));
			size = 2;
			health = 400;
			consumePower(1.5f);
		}};

		warpPad = new WarpPad("warp-pad"){{
			requirements(Category.units, with(Items.silicon, 120, Items.graphite, 80, ChronosItems.chronite, 100, Items.thorium, 30));
			size = 3;
			health = 700;
			consumePower(4f);
		}};

		//Units
		temporalFabricator = new UnitFactory("temporal-fabricator"){{
			requirements(Category.units, with(Items.silicon, 200, Items.graphite, 130, ChronosItems.chronite, 110));
			size = 3;
			health = 1100;
			consumePower(3.5f);
			envEnabled = Env.any;
			plans.add(
				new UnitFactory.UnitPlan(ChronosUnits.nanobot, 60f * 14f, with(Items.silicon, 12, ChronosItems.chronite, 8)),
				new UnitFactory.UnitPlan(ChronosUnits.siphon, 60f * 30f, with(Items.silicon, 50, Items.graphite, 20, ChronosItems.chronite, 35)),
				new UnitFactory.UnitPlan(ChronosUnits.dissonance, 60f * 50f, with(Items.silicon, 100, Items.graphite, 50, ChronosItems.chronite, 70, ChronosItems.paradoxCore, 6)),
				new UnitFactory.UnitPlan(ChronosUnits.singularity, 60f * 95f, with(Items.silicon, 220, Items.graphite, 100, ChronosItems.chronite, 140, ChronosItems.paradoxCore, 28))
			);
		}};
	}
}
