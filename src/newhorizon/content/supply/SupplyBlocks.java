package newhorizon.content.supply;

import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.content.StatusEffects;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.blocks.distribution.Conveyor;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.blocks.liquid.Conduit;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Angles;
import mindustry.entities.Effect;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Sounds;
import mindustry.type.StatusEffect;
import mindustry.graphics.Layer;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import newhorizon.content.NHStatusEffects;
import newhorizon.content.jade.JadeStatus;
import newhorizon.expand.block.supply.SectorLauncher;
import newhorizon.expand.block.supply.StatusTower;
import newhorizon.expand.block.supply.WindGenerator;
import mindustry.world.blocks.liquid.LiquidBridge;
import mindustry.world.blocks.production.Drill;

import static mindustry.type.ItemStack.with;

/**
 * The supply blocks, all made of base game items and researched in the Serpulo tree: item-themed conveyors, pipes and bridges in four tiers
 * (lead, graphite, silicon, thorium), better drills, a wind turbine and the Status Towers. Everything here uses the base game's own block
 * classes where it can.
 */
public class SupplyBlocks{
	/** The four belt tiers, in order. */
	public static final String[] tiers = {"lead", "graphite", "silicon", "thorium"};
	/** Items a second (displayed) for each tier's conveyor, the speed the base game's titanium conveyor has for 10. */
	public static final float[] conveyorSpeed = {7f, 9f, 13f, 18f};
	public static final int[] conveyorHealth = {55, 75, 100, 140};
	public static final float[] pipeCapacity = {28f, 36f, 48f, 64f};
	public static final float[] pipePressure = {1.01f, 1.02f, 1.03f, 1.05f};
	public static final int[] bridgeRange = {6, 9, 13, 18};
	public static final float[] pipeBridgeCapacity = {120f, 160f, 220f, 320f};

	public static final Block[] conveyors = new Block[4], pipes = new Block[4], bridges = new Block[4], pipeBridges = new Block[4];

	public static Block rotaryDrill, augerDrill, heavyLaserDrill, colossusDrill;
	public static Block windTurbine, improvedWindTurbine, ultimateWindTurbine;
	public static Block statusTower1, statusTower2, statusTower3;
	public static Block sectorLauncher;
	public static Block reverseSprayer;

	public static void load(){
		SupplyStatus.load();
		loadBelts();
		loadDrills();
		loadPower();
		SupplyUnits.load();
		loadTowers();
		loadLauncher();
		loadSprayer();
	}

	/**
	 * Four drills a step above the base game's: two that need no power (2x2) and two powered ones (3x3, 5x5), all with the water boost.
	 * Output of one ore tile a second is 60 / drillTime(item); the base game's mechanical drill is 600, pneumatic 400, laser 280, blast 280.
	 */
	private static void loadDrills(){
		rotaryDrill = new Drill("rotary-drill"){{
			requirements(Category.production, with(Items.copper, 30, Items.lead, 20));
			size = 2;
			tier = 2;
			drillTime = 420;
			consumeLiquid(Liquids.water, 0.06f).boost();
		}};

		augerDrill = new Drill("auger-drill"){{
			requirements(Category.production, with(Items.copper, 40, Items.graphite, 30, Items.titanium, 20));
			size = 2;
			tier = 3;
			drillTime = 260;
			consumeLiquid(Liquids.water, 0.07f).boost();
		}};

		heavyLaserDrill = new Drill("heavy-laser-drill"){{
			requirements(Category.production, with(Items.copper, 60, Items.graphite, 50, Items.silicon, 50, Items.titanium, 40, Items.thorium, 20));
			size = 3;
			tier = 4;
			drillTime = 170;
			hasPower = true;
			drawRim = true;
			updateEffect = Fx.pulverizeMedium;
			updateEffectChance = 0.03f;
			drillEffect = Fx.mineBig;
			consumePower(1.8f);
			consumeLiquid(Liquids.water, 0.10f).boost();
		}};

		colossusDrill = new Drill("colossus-drill"){{
			requirements(Category.production, with(Items.copper, 150, Items.silicon, 120, Items.titanium, 100, Items.thorium, 120, Items.plastanium, 40));
			size = 5;
			tier = 5;
			drillTime = 200;
			hasPower = true;
			drawRim = true;
			updateEffect = Fx.pulverizeRed;
			updateEffectChance = 0.04f;
			drillEffect = Fx.mineHuge;
			rotateSpeed = 3.5f;
			liquidBoostIntensity = 1.8f;
			consumePower(6f);
			consumeLiquid(Liquids.water, 0.15f).boost();
		}};
	}

	private static void loadBelts(){
		for(int i = 0; i < 4; i++){
			int t = i;
			String name = tiers[i];

			conveyors[i] = new Conveyor(name + "-conveyor"){{
				switch(t){
					case 0: requirements(Category.distribution, with(Items.copper, 1, Items.lead, 2)); break;
					case 1: requirements(Category.distribution, with(Items.lead, 1, Items.graphite, 2)); break;
					case 2: requirements(Category.distribution, with(Items.graphite, 1, Items.silicon, 2)); break;
					default: requirements(Category.distribution, with(Items.silicon, 1, Items.thorium, 2)); break;
				}
				health = conveyorHealth[t];
				displayedSpeed = conveyorSpeed[t];
				speed = displayedSpeed / 125f;
			}};

			pipes[i] = new Conduit(name + "-pipe"){{
				switch(t){
					case 0: requirements(Category.liquid, with(Items.metaglass, 1, Items.lead, 1)); break;
					case 1: requirements(Category.liquid, with(Items.metaglass, 1, Items.graphite, 1)); break;
					case 2: requirements(Category.liquid, with(Items.metaglass, 1, Items.silicon, 1)); break;
					default: requirements(Category.liquid, with(Items.metaglass, 1, Items.thorium, 1)); break;
				}
				liquidCapacity = pipeCapacity[t];
				liquidPressure = pipePressure[t];
				health = 55 + 25 * t;
				explosivenessScale = flammabilityScale = 10f / 20f;
			}};

			//No power, unlike the phase conveyor: a plain bridge that moves an item every transportTime ticks, so its rate matches its tier's conveyor.
			bridges[i] = new ItemBridge(name + "-bridge"){{
				switch(t){
					case 0: requirements(Category.distribution, with(Items.copper, 10, Items.lead, 12)); break;
					case 1: requirements(Category.distribution, with(Items.lead, 10, Items.graphite, 14)); break;
					case 2: requirements(Category.distribution, with(Items.lead, 10, Items.graphite, 10, Items.silicon, 16)); break;
					default: requirements(Category.distribution, with(Items.graphite, 10, Items.silicon, 15, Items.thorium, 12)); break;
				}
				range = bridgeRange[t];
				hasPower = false;
				transportTime = 60f / conveyorSpeed[t];
				fadeIn = moveArrows = false;
				arrowSpacing = 6f;
				itemCapacity = 10;
				health = 90 + 40 * t;
				crushFragile = true;
			}};

			pipeBridges[i] = new LiquidBridge(name + "-pipe-bridge"){{
				switch(t){
					case 0: requirements(Category.liquid, with(Items.metaglass, 10, Items.lead, 8)); break;
					case 1: requirements(Category.liquid, with(Items.metaglass, 10, Items.graphite, 12)); break;
					case 2: requirements(Category.liquid, with(Items.metaglass, 12, Items.silicon, 14)); break;
					default: requirements(Category.liquid, with(Items.metaglass, 14, Items.thorium, 12)); break;
				}
				range = bridgeRange[t];
				hasPower = false;
				floating = true;
				fadeIn = moveArrows = false;
				arrowSpacing = 6f;
				liquidCapacity = pipeBridgeCapacity[t];
				explosivenessScale = flammabilityScale = 20f / 100f;
				noAcceptDisabled = true;
				health = 90 + 40 * t;
			}};
		}

		//Auto-bridging: a conveyor that runs into something solid builds its own tier's bridge.
		for(int i = 0; i < 4; i++){
			((Conveyor)conveyors[i]).bridgeReplacement = bridges[i];
			((Conduit)pipes[i]).bridgeReplacement = pipeBridges[i];
		}
	}

	/**
	 * Fuel-free generators in three sizes. Each swings between its minimum and its full output as the wind rises and falls (about once a
	 * minute); the bigger ones are steadier and make more for each tile they cover.
	 */
	private static void loadPower(){
		windTurbine = new WindGenerator("wind-turbine"){{
			requirements(Category.power, with(Items.copper, 40, Items.lead, 35, Items.silicon, 25));
			size = 1;
			health = 100;
			powerProduction = 1f;
		}};
		improvedWindTurbine = new WindGenerator("improved-wind-turbine"){{
			requirements(Category.power, with(Items.copper, 90, Items.lead, 80, Items.silicon, 60, Items.titanium, 40));
			size = 2;
			health = 300;
			powerProduction = 5f;
			minOutput = 0.3f;
			rotorSpeed = 6f;
		}};
		ultimateWindTurbine = new WindGenerator("ultimate-wind-turbine"){{
			requirements(Category.power, with(Items.titanium, 180, Items.silicon, 160, Items.metaglass, 80, Items.thorium, 60));
			size = 3;
			health = 700;
			powerProduction = 12f;
			minOutput = 0.4f;
			rotorSpeed = 5f;
		}};
	}

	/**
	 * Three Status Towers. Each feeds item and liquid "ammo" to status effects: buffs for its own team, debuffs for its enemies. Higher
	 * tiers reach further, run more effects at once, and take more kinds of ammo.
	 */
	private static void loadTowers(){
		statusTower1 = new StatusTower("status-tower-1"){{
			requirements(Category.effect, with(Items.lead, 100, Items.silicon, 60, Items.graphite, 40));
			size = 2;
			health = 320;
			range = 110f;
			maxActive = 2;
			itemTime = 600f;
			liquidUse = 0.05f;
			itemCapacity = 10;
			liquidCapacity = 60f;
			color = Color.valueOf("6ee8d6");
			consumePower(0.8f);
			item(Items.silicon, StatusEffects.overclock, true);
			item(Items.sporePod, JadeStatus.regrowth, true);
			item(Items.pyratite, StatusEffects.burning, false);
			liquid(Liquids.water, StatusEffects.wet, false);
		}};

		statusTower2 = new StatusTower("status-tower-2"){{
			requirements(Category.effect, with(Items.titanium, 150, Items.silicon, 120, Items.metaglass, 80, Items.thorium, 40));
			size = 3;
			health = 700;
			range = 170f;
			maxActive = 4;
			itemTime = 900f;
			liquidUse = 0.08f;
			itemCapacity = 20;
			liquidCapacity = 120f;
			color = Color.valueOf("ffc45a");
			consumePower(2f);
			item(Items.silicon, StatusEffects.overclock, true);
			item(Items.sporePod, JadeStatus.regrowth, true);
			item(Items.phaseFabric, NHStatusEffects.phased, true);
			item(Items.pyratite, StatusEffects.burning, false);
			item(Items.thorium, StatusEffects.sapped, false);
			item(Items.blastCompound, StatusEffects.melting, false);
			//Experimental, with a negative value: their shots heal what they hit.
			item(Items.metaglass, SupplyStatus.mendingShots, false);
			liquid(Liquids.water, StatusEffects.wet, false);
			liquid(Liquids.cryofluid, StatusEffects.freezing, false);
		}};

		statusTower3 = new StatusTower("status-tower-3"){{
			requirements(Category.effect, with(Items.titanium, 300, Items.silicon, 250, Items.plastanium, 100, Items.thorium, 150, Items.phaseFabric, 60));
			size = 4;
			health = 1400;
			range = 230f;
			maxActive = 6;
			itemTime = 1200f;
			liquidUse = 0.12f;
			itemCapacity = 30;
			liquidCapacity = 200f;
			color = Color.valueOf("b084ff");
			consumePower(4f);
			item(Items.silicon, StatusEffects.overclock, true);
			item(Items.sporePod, JadeStatus.regrowth, true);
			item(Items.phaseFabric, NHStatusEffects.phased, true);
			item(Items.plastanium, StatusEffects.overdrive, true);
			item(Items.pyratite, StatusEffects.burning, false);
			item(Items.thorium, StatusEffects.sapped, false);
			item(Items.blastCompound, StatusEffects.melting, false);
			item(Items.surgeAlloy, NHStatusEffects.emp2, false);
			//Experimental, with a negative value, for enemies (metaglass) and for your own units (copper).
			item(Items.metaglass, SupplyStatus.mendingShots, false);
			item(Items.copper, SupplyStatus.mendingShots, true);
			liquid(Liquids.water, StatusEffects.wet, false);
			liquid(Liquids.cryofluid, StatusEffects.freezing, false);
			liquid(Liquids.oil, StatusEffects.tarred, false);
		}};
	}

	/** Sends items straight into the core of another sector of the planet. */
	private static void loadLauncher(){
		sectorLauncher = new SectorLauncher("sector-launcher"){{
			requirements(Category.effect, with(Items.silicon, 600, Items.graphite, 400, Items.thorium, 300, Items.surgeAlloy, 150, Items.phaseFabric, 150));
			size = 4;
			health = 1800;
			consumePower(8f);
		}};
	}

	/**
	 * The Reverse Sprayer: a flamethrower like the Scorch with a longer reach that sprays a pink and violet mist instead. The mist does next to
	 * no damage, but every unit it touches gets the Reverse effect (it moves backwards) for a minute. It burns spore pods.
	 */
	private static void loadSprayer(){
		reverseSprayer = new ItemTurret("reverse-sprayer"){{
			requirements(Category.turret, with(Items.titanium, 120, Items.silicon, 100, Items.graphite, 80, Items.plastanium, 30));
			size = 2;
			health = 700;
			range = 150f;
			reload = 4f;
			inaccuracy = 14f;
			shootCone = 30f;
			rotateSpeed = 8f;
			recoil = 0f;
			targetAir = true;
			targetGround = true;
			ammoUseEffect = Fx.none;
			shootSound = Sounds.shootFlame;
			//Spore pods: a pink and violet mist that makes units move backwards. Coal: an orange mist that makes them three times as fast but slow to turn and to stop.
			ammo(
				Items.sporePod, mistBullet(Color.valueOf("ff8be8"), Color.valueOf("9b5cff"), SupplyStatus.reverse),
				Items.coal, mistBullet(Color.valueOf("ffe08a"), Color.valueOf("ff7a2e"), SupplyStatus.runaway)
			);
		}};
	}

	/** One kind of the sprayer's mist: next to no damage, a cone of puffs in the two colours, and {@code applied} for a minute on whatever it touches. */
	private static BulletType mistBullet(Color from, Color to, StatusEffect applied){
		Effect mist = new Effect(40f, 60f, e -> {
			//Added to what is behind it, so that it glows in its colour over any ground and can never turn dark or gray.
			Draw.blend(Blending.additive);
			Draw.color(from, to, e.fin());
			Draw.alpha(0.4f * e.fout());
			Angles.randLenVectors(e.id, 2, 2f + 9f * e.fin(), (x, y) -> Fill.circle(e.x + x, e.y + y, 2.4f + e.fin() * 5.5f));
			Draw.blend();
		}).layer(Layer.bullet - 1f);
		Effect puff = new Effect(24f, 60f, e -> {
			Draw.blend(Blending.additive);
			Draw.color(from, to, e.fin());
			Draw.alpha(0.5f * e.fout());
			Angles.randLenVectors(e.id, 4, 3f + 14f * e.fin(), e.rotation, 30f, (x, y) -> Fill.circle(e.x + x, e.y + y, 2f + e.fout() * 3.5f));
			Draw.blend();
		}).layer(Layer.bullet - 1f);

		return new BulletType(7f, 1f){{
			ammoMultiplier = 3f;
			hitSize = 9f;
			lifetime = 22f;
			pierce = true;
			keepVelocity = false;
			hittable = false;
			absorbable = false;
			status = applied;
			statusDuration = 60f * 60f;
			shootEffect = puff;
			hitEffect = Fx.none;
			despawnEffect = Fx.none;
			smokeEffect = Fx.none;                 //the default gray muzzle smoke
			trailEffect = mist;
			trailChance = 0.9f;
		}};
	}
}
