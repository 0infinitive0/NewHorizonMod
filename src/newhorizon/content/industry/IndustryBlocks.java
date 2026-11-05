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
import mindustry.world.blocks.power.Battery;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.production.Pump;
import mindustry.world.draw.DrawDefault;
import mindustry.world.draw.DrawFlame;
import mindustry.world.draw.DrawGlowRegion;
import mindustry.world.draw.DrawMulti;
import mindustry.world.meta.Env;
import newhorizon.expand.block.distribution.LiquidMassDriver;
import newhorizon.expand.block.industry.FusionReactor;
import newhorizon.expand.block.industry.ItemTeleporter;
import newhorizon.expand.block.industry.LiquidTeleporter;

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
	public static Block electricSeparator, fusionReactor;
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
			drawer = new DrawMulti(new DrawDefault(), new DrawGlowRegion(){{
				color = Color.valueOf("7ff4ff");
				alpha = 0.85f;
				glowScale = 5f;
				glowIntensity = 0.25f;
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
