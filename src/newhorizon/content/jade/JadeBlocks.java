package newhorizon.content.jade;

import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.gen.Sounds;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.blocks.defense.MendProjector;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.PowerTurret;
import mindustry.world.blocks.power.ConsumeGenerator;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.blocks.units.RepairTurret;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.consumers.ConsumeItemFlammable;
import mindustry.world.draw.DrawDefault;
import mindustry.world.draw.DrawMulti;
import mindustry.world.draw.DrawRegion;
import newhorizon.expand.block.defence.ContagionSpire;
import newhorizon.expand.block.defence.RegrowthWall;
import newhorizon.expand.block.defence.SporeWall;
import newhorizon.expand.block.distribution.LiquidMassDriver;

import static mindustry.type.ItemStack.with;

public class JadeBlocks{
	public static Block
		resinTapper, silkLoom, bloomConservatory,
		sporeFurnace, sporeWaterwheel, liquidMassDriver,
		thornSpitter, briarLance, canopyMortar, sporeCatapult,
		jadeWall, jadeWallLarge, sporeWall, sporeWallLarge, lifebloomPylon,
		jadeNursery, verdantCultivator, groveCultivator,
		verdantClinic, contagionSpire;

	public static void load(){
		//Resource chain: spore pods -> resin -> silk -> everbloom core
		resinTapper = new GenericCrafter("resin-tapper"){{
			requirements(Category.crafting, with(Items.copper, 60, Items.lead, 40, Items.graphite, 25));
			size = 2;
			health = 260;
			hasLiquids = true;
			craftTime = 60f;
			outputItem = new ItemStack(JadeItems.verdantResin, 2);
			consumeItem(Items.sporePod, 1);
			consumeLiquid(Liquids.water, 0.1f);
			consumePower(0.6f);
			craftEffect = JadeFx.leafBurst;
			ambientSound = Sounds.loopBio;
			ambientSoundVolume = 0.04f;
		}};

		silkLoom = new GenericCrafter("silk-loom"){{
			requirements(Category.crafting, with(Items.copper, 120, Items.lead, 90, Items.silicon, 60, Items.titanium, 50));
			size = 3;
			health = 640;
			craftTime = 90f;
			outputItem = new ItemStack(JadeItems.jadeSilk, 1);
			consumeItems(with(JadeItems.verdantResin, 3, Items.silicon, 1));
			consumePower(1.5f);
			craftEffect = JadeFx.leafBurstBig;
			drawer = new DrawMulti(new DrawDefault(), new DrawRegion("-top"));
		}};

		bloomConservatory = new GenericCrafter("bloom-conservatory"){{
			requirements(Category.crafting, with(Items.titanium, 150, Items.silicon, 120, Items.plastanium, 40, JadeItems.jadeSilk, 60));
			size = 3;
			health = 900;
			hasLiquids = true;
			craftTime = 200f;
			outputItem = new ItemStack(JadeItems.everbloomCore, 1);
			consumeItems(with(JadeItems.jadeSilk, 3, Items.phaseFabric, 1, Items.sporePod, 4));
			consumeLiquid(Liquids.water, 0.2f);
			consumePower(3f);
			craftEffect = JadeFx.pollenBurst;
			drawer = new DrawMulti(new DrawDefault(), new DrawRegion("-top"));
		}};

		sporeFurnace = new ConsumeGenerator("spore-furnace"){{
			requirements(Category.power, with(Items.copper, 80, Items.lead, 60, Items.graphite, 50, Items.silicon, 30));
			size = 2;
			health = 320;
			powerProduction = 3.4f;
			itemDuration = 150f;
			consume(new ConsumeItemFlammable(0.3f));
			generateEffect = JadeFx.leafBurst;
			ambientSound = Sounds.loopBio;
			ambientSoundVolume = 0.06f;
		}};

		//Spores love water: this turns a steady flow of it into power, no fuel needed. One mechanical pump on water feeds about one.
		sporeWaterwheel = new ConsumeGenerator("spore-waterwheel"){{
			requirements(Category.power, with(Items.copper, 80, Items.lead, 70, Items.graphite, 40, JadeItems.verdantResin, 30));
			size = 2;
			health = 340;
			powerProduction = 2.6f;
			consumeLiquid(Liquids.water, 0.12f);
			generateEffect = JadeFx.leafBurst;
			effectChance = 0.02f;
			drawer = new DrawMulti(new DrawDefault(), new DrawRegion("-rotor", 3f, true));
		}};

		//Fires liquid in bolts to a linked driver, over anything in between. Needs a pump on one end and a conduit on the other.
		liquidMassDriver = new LiquidMassDriver("liquid-mass-driver"){{
			requirements(Category.liquid, with(Items.titanium, 125, Items.silicon, 75, Items.metaglass, 125, JadeItems.jadeSilk, 40));
			size = 3;
			health = 640;
			range = 440f;
			reload = 120f;
			payload = 600f;
			minDistribute = 100f;
			liquidCapacity = 1800f;
			consumePower(1.75f);
		}};

		//Turrets
		thornSpitter = new ItemTurret("thorn-spitter"){{
			requirements(Category.turret, with(Items.copper, 90, Items.graphite, 50, JadeItems.verdantResin, 30));
			size = 2;
			health = 520;
			range = 150f;
			reload = 24f;
			rotateSpeed = 7f;
			inaccuracy = 3f;
			shoot = new mindustry.entities.pattern.ShootSpread(3, 7f);
			recoil = 2f;
			shootSound = Sounds.shootDuo;
			outlineColor = JadeColor.outline;
			coolant = consumeCoolant(0.15f);

			ammo(
				JadeItems.verdantResin, JadeBullets.thornShard,
				JadeItems.jadeSilk, JadeBullets.thornShardSilk
			);
		}};

		briarLance = new PowerTurret("briar-lance"){{
			requirements(Category.turret, with(Items.titanium, 120, Items.silicon, 110, Items.graphite, 80, JadeItems.jadeSilk, 60));
			size = 3;
			health = 1600;
			range = 200f;
			reload = 85f;
			rotateSpeed = 4f;
			recoil = 3.5f;
			shootType = JadeBullets.lanceBeam;
			shoot.firstShotDelay = 28f;
			moveWhileCharging = false;
			accurateDelay = false;
			shootSound = Sounds.shootLaser;
			outlineColor = JadeColor.outline;
			consumePower(5.5f);
			coolant = consumeCoolant(0.2f);
		}};

		canopyMortar = new ItemTurret("canopy-mortar"){{
			requirements(Category.turret, with(Items.titanium, 250, Items.plastanium, 100, Items.silicon, 200, JadeItems.jadeSilk, 150, JadeItems.everbloomCore, 40));
			size = 4;
			health = 3000;
			range = 340f;
			minRange = 40f;
			reload = 110f;
			rotateSpeed = 2.6f;
			recoil = 5f;
			shake = 2f;
			targetAir = false;
			shootSound = Sounds.shootArtillery;
			outlineColor = JadeColor.outline;
			coolant = consumeCoolant(0.3f);

			ammo(
				JadeItems.jadeSilk, JadeBullets.canopyShell,
				JadeItems.everbloomCore, JadeBullets.canopyShellCore
			);
		}};

		//Lobs pods that burst into Sporelings far behind enemy lines; the Sporelings then hunt on their own.
		sporeCatapult = new ItemTurret("spore-catapult"){{
			requirements(Category.turret, with(Items.titanium, 220, Items.silicon, 180, Items.plastanium, 70, JadeItems.jadeSilk, 120));
			size = 3;
			health = 2400;
			range = 480f;
			minRange = 60f;
			reload = 150f;
			rotateSpeed = 3f;
			recoil = 4f;
			shake = 1.5f;
			inaccuracy = 3f;
			velocityRnd = 0.08f;
			ammoPerShot = 3;
			targetAir = false;
			shootSound = Sounds.shootArtillery;
			outlineColor = JadeColor.outline;
			consumePower(3f);
			coolant = consumeCoolant(0.2f);

			ammo(
				JadeItems.verdantResin, JadeBullets.sporePod,
				JadeItems.jadeSilk, JadeBullets.sporePodSilk
			);
		}};

		//Defense
		jadeWall = new RegrowthWall("jade-wall"){{
			requirements(Category.defense, with(Items.titanium, 4, JadeItems.jadeSilk, 2));
			health = 130 * 4;
			armor = 2f;
			regrowEffect = JadeFx.leafBurst;
		}};

		jadeWallLarge = new RegrowthWall("jade-wall-large"){{
			requirements(Category.defense, ItemStack.mult(jadeWall.requirements, 4));
			size = 2;
			health = 130 * 4 * 4;
			armor = 2f;
			regrowEffect = JadeFx.leafBurst;
		}};

		sporeWall = new SporeWall("bloom-wall"){{
			requirements(Category.defense, with(Items.sporePod, 6, JadeItems.verdantResin, 3));
			health = 1100;
			armor = 3f;
		}};

		sporeWallLarge = new SporeWall("bloom-wall-large"){{
			requirements(Category.defense, ItemStack.mult(sporeWall.requirements, 4));
			size = 2;
			health = 1100 * 4;
			armor = 3f;
		}};

		lifebloomPylon = new MendProjector("lifebloom-pylon"){{
			requirements(Category.effect, with(Items.lead, 50, Items.silicon, 40, Items.titanium, 25, JadeItems.jadeSilk, 30));
			size = 2;
			health = 380;
			reload = 180f;
			range = 120f;
			healPercent = 13f;
			phaseBoost = 6f;
			phaseRangeBoost = 36f;
			baseColor = JadeColor.jade;
			phaseColor = JadeColor.pollen;
			consumePower(0.5f);
			consumeItem(JadeItems.jadeSilk).boost();
		}};

		//Heals units, ground and air, in range. Idle damaged Jade units walk here on their own (HealSeekAbility).
		verdantClinic = new RepairTurret("verdant-clinic"){{
			requirements(Category.units, with(Items.lead, 60, Items.silicon, 50, Items.graphite, 30, JadeItems.jadeSilk, 20));
			size = 2;
			health = 480;
			repairSpeed = 1.6f;
			repairRadius = 125f;
			powerUse = 2.5f;
			length = 5f;
			beamWidth = 0.9f;
			pulseRadius = 5f;
			laserColor = JadeColor.jadeLight;
			laserTopColor = JadeColor.jade;
			acceptCoolant = false;
		}};

		//Infects every enemy unit in range, the same infestation a Sporeling leaves: damage over time until it dies (into Sporelings) or is healed to full.
		//Has to be fed constantly with spore pods, water and power.
		contagionSpire = new ContagionSpire("contagion-spire"){{
			requirements(Category.effect, with(Items.silicon, 90, Items.lead, 80, Items.titanium, 60, JadeItems.verdantResin, 50, JadeItems.jadeSilk, 40));
			size = 2;
			health = 520;
			range = 170f;
			//Power, water and spores each have a store sized so that a full block runs 8 minutes, until all are empty together.
			itemDuration = 60f * 12f;
			itemCapacity = 40;
			waterUse = 0.2f;
			powerUse = 1.2f;
			chargeRate = 6f;
			pulseEffect = JadeFx.bloomRing(JadeColor.infection, 170f);
		}};

		//Unit production
		jadeNursery = new UnitFactory("jade-nursery"){{
			requirements(Category.units, with(Items.copper, 80, Items.lead, 70, Items.silicon, 60, JadeItems.verdantResin, 40));
			size = 3;
			health = 800;
			consumePower(1.4f);
			plans.addAll(
				new UnitFactory.UnitPlan(JadeUnits.sprig, 60f * 12f, with(Items.silicon, 18, JadeItems.verdantResin, 12)),
				new UnitFactory.UnitPlan(JadeUnits.wisp, 60f * 14f, with(Items.silicon, 22, Items.lead, 10, JadeItems.verdantResin, 14)),
				new UnitFactory.UnitPlan(JadeUnits.sporeling, 60f * 8f, with(Items.silicon, 10, JadeItems.verdantResin, 20)),
				new UnitFactory.UnitPlan(JadeUnits.gardener, 60f * 18f, with(Items.silicon, 30, Items.lead, 20, JadeItems.verdantResin, 25)),
				new UnitFactory.UnitPlan(JadeUnits.herbalist, 60f * 16f, with(Items.silicon, 30, Items.lead, 25, JadeItems.verdantResin, 20)),
				new UnitFactory.UnitPlan(JadeUnits.scavenger, 60f * 14f, with(Items.silicon, 20, Items.lead, 15, JadeItems.verdantResin, 15)),
				new UnitFactory.UnitPlan(JadeUnits.mimic, 60f * 20f, with(Items.silicon, 40, Items.lead, 30, JadeItems.verdantResin, 30, JadeItems.jadeSilk, 10)),
				new UnitFactory.UnitPlan(JadeUnits.bogguard, 60f * 40f, with(Items.silicon, 70, Items.lead, 60, Items.titanium, 50, JadeItems.verdantResin, 70, JadeItems.jadeSilk, 20))
			);
		}};

		verdantCultivator = new Reconstructor("verdant-cultivator"){{
			requirements(Category.units, with(Items.copper, 200, Items.lead, 120, Items.silicon, 90, JadeItems.jadeSilk, 40));
			size = 3;
			health = 1300;
			consumePower(3f);
			consumeItems(with(Items.silicon, 40, JadeItems.jadeSilk, 20));
			constructTime = 60f * 10f;
			upgrades.addAll(
				new UnitType[]{JadeUnits.sprig, JadeUnits.thornback},
				new UnitType[]{JadeUnits.wisp, JadeUnits.luminar}
			);
		}};

		groveCultivator = new Reconstructor("grove-cultivator"){{
			requirements(Category.units, with(Items.lead, 650, Items.silicon, 450, Items.titanium, 350, Items.thorium, 250, JadeItems.jadeSilk, 200, JadeItems.everbloomCore, 40));
			size = 5;
			health = 4200;
			consumePower(6f);
			consumeItems(with(Items.silicon, 130, Items.titanium, 80, JadeItems.jadeSilk, 50, JadeItems.everbloomCore, 4));
			constructTime = 60f * 30f;
			upgrades.addAll(
				new UnitType[]{JadeUnits.thornback, JadeUnits.briarlord},
				new UnitType[]{JadeUnits.luminar, JadeUnits.verdance}
			);
		}};
	}
}
