package newhorizon.content.industry;

import arc.graphics.Color;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.type.LiquidStack;
import mindustry.world.Block;
import mindustry.world.blocks.distribution.MassDriver;
import mindustry.world.blocks.liquid.LiquidRouter;
import mindustry.world.blocks.defense.turrets.LiquidTurret;
import newhorizon.expand.bullets.plasma.PlasmaBoltBulletType;
import newhorizon.expand.bullets.plasma.PlasmaMineBulletType;
import mindustry.world.blocks.power.Battery;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.production.Pump;
import mindustry.world.draw.DrawDefault;
import mindustry.world.draw.DrawFlame;
import mindustry.world.draw.DrawGlowRegion;
import mindustry.world.draw.DrawLiquidOutputs;
import mindustry.world.draw.DrawMulti;
import mindustry.world.draw.DrawRegion;
import mindustry.world.meta.Env;
import newhorizon.expand.block.distribution.LiquidMassDriver;
import newhorizon.expand.block.industry.FusionReactor;
import newhorizon.expand.block.industry.ItemTeleporter;
import newhorizon.expand.block.industry.LiquidTeleporter;
import newhorizon.expand.block.industry.SlagRefinery;

import static mindustry.type.ItemStack.with;

/**
 * The industry blocks. They are made only of silicon, graphite, thorium, surge alloy and phase fabric, which every planet has, and work
 * on every planet ({@link Env#any}).
 */
public class IndustryBlocks{
	public static Block ultimateSiliconFactory;
	public static Block turbinePump, quantumPump, singularityPump;
	public static Block armoredContainer, armoredTank, singularityTank;
	public static Block capacitorBank, surgeBattery, singularityBattery;
	public static Block electricSeparator, fusionReactor, slagRefinery, plasmaSmelter, plasmaCannon, plasmaMineCannon;
	public static Block advancedMassDriver, advancedLiquidMassDriver, itemTeleporter, liquidTeleporter;

	public static void load(){
		//Sand and graphite (what every planet has: Serpulo's coal makes graphite, Erekir's furnace uses it too) in, a great deal of silicon out.
		ultimateSiliconFactory = new GenericCrafter("ultimate-silicon-factory"){{
			requirements(Category.crafting, with(Items.silicon, 1200, Items.graphite, 800, Items.thorium, 500, Items.surgeAlloy, 300));
			size = 5;
			health = 2600;
			envEnabled = Env.any;
			hasPower = true;
			hasItems = true;
			itemCapacity = 400;
			craftTime = 60f;
			outputItem = new ItemStack(Items.silicon, 40);
			consumeItems(with(Items.sand, 80, Items.graphite, 40));
			consumePower(40f);
			craftEffect = Fx.smeltsmoke;
			drawer = new DrawMulti(new DrawDefault(), new DrawFlame(Color.valueOf("ffd27a")));
			ambientSound = mindustry.gen.Sounds.loopSmelter;
			ambientSoundVolume = 0.12f;
		}};

		//Pumps: each tile of water gives pumpAmount a tick (the base game's Impulse Pump is 0.22 on 9 tiles, 119 a second).
		turbinePump = new Pump("turbine-pump"){{
			requirements(Category.liquid, with(Items.silicon, 200, Items.graphite, 150, Items.thorium, 120));
			size = 3;
			health = 900;
			envEnabled = Env.any;
			pumpAmount = 0.45f;
			liquidCapacity = 1800f;
			consumePower(2.2f);
		}};
		quantumPump = new Pump("quantum-pump"){{
			requirements(Category.liquid, with(Items.silicon, 400, Items.thorium, 300, Items.surgeAlloy, 150));
			size = 4;
			health = 1800;
			envEnabled = Env.any;
			pumpAmount = 0.5f;
			liquidCapacity = 3600f;
			consumePower(6f);
		}};
		singularityPump = new Pump("singularity-pump"){{
			requirements(Category.liquid, with(Items.silicon, 800, Items.thorium, 600, Items.surgeAlloy, 400, Items.phaseFabric, 300));
			size = 5;
			health = 3200;
			envEnabled = Env.any;
			pumpAmount = 0.95f;
			liquidCapacity = 9000f;
			consumePower(14f);
		}};

		//Tanks.
		armoredContainer = new LiquidRouter("armored-container"){{
			requirements(Category.liquid, with(Items.silicon, 80, Items.graphite, 80, Items.thorium, 60));
			size = 2;
			health = 900;
			envEnabled = Env.any;
			solid = true;
			underBullets = true;
			liquidCapacity = 2500f;
			liquidPadding = 2f;
		}};
		armoredTank = new LiquidRouter("armored-tank"){{
			requirements(Category.liquid, with(Items.silicon, 300, Items.thorium, 200, Items.surgeAlloy, 100));
			size = 3;
			health = 3600;
			armor = 10f;
			envEnabled = Env.any;
			solid = true;
			underBullets = true;
			liquidCapacity = 9000f;
			liquidPadding = 2f;
		}};
		//Enormous storage in three by three tiles.
		singularityTank = new LiquidRouter("singularity-tank"){{
			requirements(Category.liquid, with(Items.silicon, 1200, Items.thorium, 900, Items.surgeAlloy, 600, Items.phaseFabric, 500));
			size = 3;
			health = 9000;
			armor = 20f;
			envEnabled = Env.any;
			solid = true;
			underBullets = true;
			liquidCapacity = 150000f;
			liquidPadding = 2f;
		}};

		//Batteries (the base game's large battery is 3x3 and holds 25,000).
		capacitorBank = new Battery("capacitor-bank"){{
			requirements(Category.power, with(Items.silicon, 100, Items.graphite, 150, Items.thorium, 60));
			size = 2;
			health = 600;
			envEnabled = Env.any;
			consumePowerBuffered(80000f);
			baseExplosiveness = 2f;
		}};
		surgeBattery = new Battery("surge-battery"){{
			requirements(Category.power, with(Items.silicon, 300, Items.thorium, 200, Items.surgeAlloy, 150));
			size = 3;
			health = 1800;
			envEnabled = Env.any;
			consumePowerBuffered(500000f);
			baseExplosiveness = 4f;
		}};
		singularityBattery = new Battery("singularity-battery"){{
			requirements(Category.power, with(Items.silicon, 1000, Items.thorium, 800, Items.surgeAlloy, 500, Items.phaseFabric, 400));
			size = 4;
			health = 4800;
			envEnabled = Env.any;
			consumePowerBuffered(6000000f);
			baseExplosiveness = 8f;
		}};

		//Mass drivers: the base game's reaches 440 and flies at 5.5; these reach 1,200 at 14 (items) and 12 (liquids).
		advancedMassDriver = new MassDriver("advanced-mass-driver"){{
			requirements(Category.distribution, with(Items.silicon, 400, Items.thorium, 250, Items.surgeAlloy, 150, Items.phaseFabric, 100));
			size = 4;
			health = 1800;
			envEnabled = Env.any;
			itemCapacity = 600;
			minDistribute = 60;
			range = 1200f;
			bulletSpeed = 14f;
			bulletLifetime = 160f;
			reload = 60f;
			//Degrees a tick (the base game's is 5).
			rotateSpeed = 8f;
			translation = 10f;
			consumePower(6f);
		}};
		advancedLiquidMassDriver = new LiquidMassDriver("advanced-liquid-mass-driver"){{
			requirements(Category.liquid, with(Items.silicon, 450, Items.thorium, 300, Items.surgeAlloy, 180, Items.phaseFabric, 120));
			size = 4;
			health = 1800;
			envEnabled = Env.any;
			liquidCapacity = 9000f;
			minDistribute = 300f;
			payload = 3000f;
			range = 1200f;
			bulletSpeed = 12f;
			bulletLifetime = 160f;
			reload = 60f;
			rotateSpeed = 0.08f;
			translation = 10f;
			consumePower(7f);
		}};

		//Teleporters: any number on a channel, any distance apart, both ends work both ways.
		itemTeleporter = new ItemTeleporter("item-teleporter"){{
			requirements(Category.distribution, with(Items.silicon, 300, Items.graphite, 200, Items.thorium, 150, Items.phaseFabric, 100));
			size = 2;
			health = 700;
			consumePower(4f);
		}};
		liquidTeleporter = new LiquidTeleporter("liquid-teleporter"){{
			requirements(Category.liquid, with(Items.silicon, 400, Items.thorium, 250, Items.surgeAlloy, 100, Items.phaseFabric, 150));
			size = 3;
			health = 1400;
			consumePower(6f);
		}};

		//Water and power in, hydrogen and ozone out: Erekir's Electrolyzer made from Serpulo's materials, bigger. It feeds the Fusion Reactor.
		electricSeparator = new GenericCrafter("electric-separator"){{
			requirements(Category.crafting, with(Items.silicon, 300, Items.graphite, 250, Items.thorium, 150, Items.surgeAlloy, 60));
			size = 3;
			health = 1200;
			envEnabled = Env.any;
			hasPower = true;
			hasLiquids = true;
			liquidCapacity = 600f;
			consumeLiquid(Liquids.water, 36f / 60f);
			consumePower(3f);
			outputLiquids = LiquidStack.with(Liquids.hydrogen, 18f / 60f, Liquids.ozone, 6f / 60f);
			//Like the Electrolyzer: it can be turned, and each liquid leaves from its own side (relative to the rotation: 1 is the left, 3 the right),
			//with the ports drawn on those sides. Hydrogen out of the left, ozone out of the right.
			rotate = true;
			invertFlip = true;
			liquidOutputDirections = new int[]{1, 3};
			drawer = new DrawMulti(new DrawRegion(), new DrawLiquidOutputs(), new DrawGlowRegion(){{
				color = Color.valueOf("7ff4ff");
				alpha = 0.85f;
				glowScale = 5f;
				glowIntensity = 0.25f;
			}});
		}};

		//Slag and ozone in, one chosen ore out (the rarer the ore, the more of both it takes). The ozone comes from the Electric Separator.
		slagRefinery = new SlagRefinery("slag-refinery"){{
			requirements(Category.crafting, with(Items.silicon, 250, Items.graphite, 200, Items.thorium, 120, Items.surgeAlloy, 40));
			size = 3;
			health = 1100;
			envEnabled = Env.any;
			consumePower(2f);
			speed = 5f;
			recipe(Items.sand, 8f, 0.3f, 20f);
			recipe(Items.scrap, 8f, 0.3f, 20f);
			recipe(Items.copper, 12f, 0.5f, 30f);
			recipe(Items.lead, 14f, 0.6f, 32f);
			recipe(Items.coal, 16f, 0.8f, 36f);
			recipe(Items.titanium, 30f, 1.5f, 50f);
			recipe(Items.beryllium, 36f, 2f, 55f);
			recipe(Items.tungsten, 50f, 3f, 70f);
			recipe(Items.thorium, 80f, 5f, 100f);
		}};

		//Sand and plasma in, a great deal of silicon out: the most of any factory (the Ultimate Silicon Factory makes 40 a second).
		plasmaSmelter = new GenericCrafter("plasma-smelter"){{
			requirements(Category.crafting, with(Items.silicon, 1000, Items.graphite, 700, Items.thorium, 500, Items.surgeAlloy, 250, Items.phaseFabric, 200));
			size = 6;
			health = 4000;
			envEnabled = Env.any;
			hasPower = true;
			hasItems = true;
			hasLiquids = true;
			itemCapacity = 600;
			liquidCapacity = 300f;
			craftTime = 60f;
			outputItem = new ItemStack(Items.silicon, 100);
			consumeItems(with(Items.sand, 160));
			consumeLiquid(IndustryLiquids.plasma, 0.5f);
			consumePower(30f);
			craftEffect = Fx.smeltsmoke;
			drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{
				color = Color.valueOf("ff7bd0");
				alpha = 0.9f;
				glowScale = 6f;
				glowIntensity = 0.3f;
			}});
			ambientSound = mindustry.gen.Sounds.loopSmelter;
			ambientSoundVolume = 0.15f;
		}};

		//A turret that fires plasma bolts: fast, piercing, burning.
		plasmaCannon = new LiquidTurret("plasma-cannon"){{
			requirements(Category.turret, with(Items.silicon, 300, Items.thorium, 200, Items.surgeAlloy, 100, Items.phaseFabric, 60));
			size = 3;
			health = 1800;
			envEnabled = Env.any;
			liquidCapacity = 300f;
			extinguish = false;
			range = 280f;
			reload = 40f;
			rotateSpeed = 5f;
			recoil = 2f;
			targetAir = true;
			targetGround = true;
			shootSound = mindustry.gen.Sounds.shootLaser;
			ammo(IndustryLiquids.plasma, new PlasmaBoltBulletType(9f, 90f){{
				lifetime = 36f;
				width = 14f;
				height = 22f;
				trailLength = 12;
				trailWidth = 3.5f;
				pierce = true;
				pierceCap = 2;
				splashDamage = 60f;
				splashDamageRadius = 26f;
				ammoMultiplier = 1f / 10f;
				puddle = 4f;
				hitEffect = Fx.blastExplosion;
				despawnEffect = Fx.blastExplosion;
				shootEffect = Fx.shootBig;
			}});
		}};

		//A different turret: it lays plasma mines that drift slowly about the field and go off when an enemy touches them.
		plasmaMineCannon = new LiquidTurret("plasma-mine-cannon"){{
			requirements(Category.turret, with(Items.silicon, 350, Items.thorium, 250, Items.surgeAlloy, 150, Items.phaseFabric, 80));
			size = 3;
			health = 1900;
			envEnabled = Env.any;
			liquidCapacity = 300f;
			extinguish = false;
			range = 300f;
			reload = 150f;
			rotateSpeed = 3f;
			recoil = 4f;
			targetAir = true;
			targetGround = true;
			shootSound = mindustry.gen.Sounds.shootLaser;
			ammo(IndustryLiquids.plasma, new PlasmaMineBulletType(){{
				ammoMultiplier = 1f / 20f;
				shootEffect = Fx.shootBig;
			}});
		}};

		//Hydrogen (or less well ozone) in, 200 power a tick out, a little thorium on the side; it takes a great deal of power to light.
		fusionReactor = new FusionReactor("fusion-reactor"){{
			requirements(Category.power, with(Items.silicon, 1500, Items.thorium, 1200, Items.surgeAlloy, 700, Items.phaseFabric, 800));
			size = 5;
			health = 5500;
			armor = 6f;
			envEnabled = Env.any;
			powerProduction = 200f;
			consumePower(200f);
		}};
	}
}
