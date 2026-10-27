package newhorizon.content.hiemara;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.content.StatusEffects;
import mindustry.entities.bullet.ArtilleryBulletType;
import mindustry.entities.bullet.LaserBulletType;
import mindustry.entities.bullet.LightningBulletType;
import mindustry.entities.bullet.LiquidBulletType;
import mindustry.gen.Sounds;
import mindustry.graphics.Layer;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.blocks.defense.turrets.LiquidTurret;
import mindustry.world.blocks.defense.turrets.PowerTurret;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.blocks.units.UnitFactory.UnitPlan;
import newhorizon.expand.block.hiemara.NavalMine;
import newhorizon.expand.block.hiemara.VortexGenerator;

import static mindustry.type.ItemStack.with;

/**
 * Defence of Hiemara: turrets with the water theme (two that run on power, two that run on liquid), the vortex generator,
 * the naval mine and the warship yard that makes the warships. All of them can be built on deep water.
 */
public class HiemaraDefense{
	public static PowerTurret arcCoil, pulseLance;
	public static LiquidTurret tideCannon, whirlMortar;
	public static VortexGenerator vortexGenerator;
	public static NavalMine navalMine;
	public static UnitFactory warshipYard;
	
	public static final Seq<Block> all = new Seq<>();
	
	public static void load(){
		arcCoil = new PowerTurret("arc-coil"){{
			requirements(Category.turret, with(Items.copper, 70, Items.lead, 60, Items.sand, 30));
			size = 2;
			health = 520;
			range = 14f * 8f;
			reload = 38f;
			rotateSpeed = 12f;
			targetAir = true;
			placeableLiquid = true;
			shootSound = Sounds.shootArc;
			consumePower(3.3f);
			shootType = new LightningBulletType(){{
				damage = 22f;
				lightningLength = 20;
				collidesAir = true;
				ammoMultiplier = 1f;
				lightningColor = Color.valueOf("9fe3ff");
			}};
		}};
		
		pulseLance = new PowerTurret("pulse-lance"){{
			requirements(Category.turret, with(Items.lead, 160, Items.titanium, 90, Items.silicon, 60));
			size = 3;
			health = 1250;
			range = 26f * 8f;
			reload = 80f;
			rotateSpeed = 6f;
			targetAir = true;
			placeableLiquid = true;
			shootSound = Sounds.shootLancer;
			shoot.firstShotDelay = 28f;
			moveWhileCharging = false;
			accurateDelay = true;
			consumePower(6.5f);
			shootType = new LaserBulletType(120f){{
				colors = new Color[]{Color.valueOf("5ec2ff55"), Color.valueOf("8fd8ff"), Color.white};
				chargeEffect = new mindustry.entities.effect.MultiEffect(Fx.lancerLaserCharge, Fx.lancerLaserChargeBegin);
				buildingDamageMultiplier = 0.25f;
				hitEffect = Fx.hitLancer;
				hitSize = 4f;
				lifetime = 16f;
				drawSize = 440f;
				collidesAir = true;
				length = 26f * 8f;
				ammoMultiplier = 1f;
				pierceCap = 4;
			}};
		}};
		
		tideCannon = new LiquidTurret("tide-cannon"){{
			requirements(Category.turret, with(Items.copper, 90, Items.lead, 70, Items.sand, 50));
			size = 2;
			health = 600;
			range = 17f * 8f;
			reload = 3f;
			inaccuracy = 4f;
			shootCone = 50f;
			recoil = 0f;
			liquidCapacity = 20f;
			targetAir = true;
			extinguish = true;
			placeableLiquid = true;
			shootEffect = Fx.shootLiquid;
			ammo(
				Liquids.water, new LiquidBulletType(Liquids.water){{
					damage = 5f;
					knockback = 1.1f;
					drag = 0.01f;
					layer = Layer.bullet - 2f;
					lifetime = 52f;
					speed = 4f;
					collidesAir = true;
				}},
				Liquids.oil, new LiquidBulletType(Liquids.oil){{
					damage = 7f;
					knockback = 0.6f;
					drag = 0.01f;
					layer = Layer.bullet - 2f;
					lifetime = 52f;
					speed = 4f;
					collidesAir = true;
					status = StatusEffects.tarred;
					statusDuration = 60f * 4f;
				}}
			);
		}};
		
		whirlMortar = new LiquidTurret("whirl-mortar"){{
			requirements(Category.turret, with(Items.lead, 180, Items.titanium, 100, Items.silicon, 80, Items.coal, 60));
			size = 3;
			health = 1300;
			range = 30f * 8f;
			reload = 64f;
			inaccuracy = 2f;
			recoil = 2f;
			liquidCapacity = 40f;
			targetAir = false;
			extinguish = false;
			placeableLiquid = true;
			shootSound = Sounds.shootArtillery;
			ammo(
				Liquids.water, new ArtilleryBulletType(2.8f, 20f){{
					lifetime = 86f;
					splashDamage = 80f;
					splashDamageRadius = 42f;
					collidesAir = false;
					hitEffect = Fx.blastExplosion;
					knockback = 1.4f;
					width = height = 11f;
					backColor = trailColor = Color.valueOf("6cc0ff");
					status = StatusEffects.wet;
					statusDuration = 60f * 4f;
					ammoMultiplier = 1f;
				}},
				Liquids.oil, new ArtilleryBulletType(2.8f, 20f){{
					lifetime = 86f;
					splashDamage = 115f;
					splashDamageRadius = 46f;
					collidesAir = false;
					hitEffect = Fx.blastExplosion;
					knockback = 0.8f;
					width = height = 11f;
					backColor = trailColor = Color.valueOf("3a3a40");
					status = StatusEffects.tarred;
					statusDuration = 60f * 6f;
					ammoMultiplier = 1f;
				}}
			);
		}};
		
		vortexGenerator = new VortexGenerator("vortex-generator"){{
			requirements(Category.turret, with(Items.lead, 150, Items.titanium, 80, Items.silicon, 60, Items.sand, 80));
			size = 3;
			health = 900;
		}};
		
		navalMine = new NavalMine("naval-mine"){{
			requirements(Category.effect, with(Items.copper, 10, Items.lead, 6, Items.scrap, 4));
		}};
		
		warshipYard = new UnitFactory("warship-yard"){{
			requirements(Category.units, with(Items.copper, 200, Items.lead, 150, Items.sand, 80));
			size = 4;
			health = 900;
			placeableLiquid = true;
			consumePower(2.4f);
			plans.addAll(
				new UnitPlan(HiemaraWarships.patrolBoat, 14f * 60f, with(Items.copper, 40, Items.lead, 30, Items.scrap, 10)),
				new UnitPlan(HiemaraWarships.corvette, 24f * 60f, with(Items.copper, 70, Items.lead, 70, Items.sand, 40)),
				new UnitPlan(HiemaraWarships.torpedoBoat, 24f * 60f, with(Items.copper, 60, Items.lead, 70, Items.sand, 30, Items.coal, 20)),
				new UnitPlan(HiemaraWarships.mortarSkiff, 26f * 60f, with(Items.copper, 80, Items.lead, 60, Items.sand, 40, Items.coal, 20)),
				new UnitPlan(HiemaraWarships.missileFrigate, 70f * 60f, with(Items.lead, 220, Items.titanium, 140, Items.silicon, 100, Items.coal, 60)),
				new UnitPlan(HiemaraWarships.abyss, 55f * 60f, with(Items.lead, 160, Items.titanium, 110, Items.silicon, 80, Items.sand, 60)),
				new UnitPlan(HiemaraWarships.longbow, 110f * 60f, with(Items.lead, 400, Items.titanium, 300, Items.silicon, 220, Items.thorium, 80)),
				new UnitPlan(HiemaraWarships.aircraftCarrier, 200f * 60f, with(Items.lead, 900, Items.titanium, 700, Items.silicon, 600, Items.thorium, 300, Items.tungsten, 120))
			);
		}};
		
		all.addAll(arcCoil, pulseLance, tideCannon, whirlMortar, vortexGenerator, navalMine, warshipYard);
	}
}
