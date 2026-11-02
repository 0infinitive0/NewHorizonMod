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
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.LiquidTurret;
import mindustry.entities.bullet.FlakBulletType;
import mindustry.entities.bullet.MissileBulletType;
import mindustry.world.blocks.defense.turrets.PowerTurret;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.blocks.units.UnitFactory.UnitPlan;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.RailBulletType;
import mindustry.entities.pattern.ShootAlternate;
import newhorizon.content.NHItems;
import newhorizon.expand.block.hiemara.IceWall;
import newhorizon.expand.block.hiemara.StormTower;
import newhorizon.expand.block.hiemara.TornadoTower;
import newhorizon.expand.bullets.nuke.TornadoBulletType;
import newhorizon.expand.block.hiemara.NavalMine;
import newhorizon.expand.block.hiemara.VortexGenerator;

import static mindustry.type.ItemStack.with;

/**
 * Defence of Hiemara: turrets with the water theme (two that run on power, two that run on liquid), the vortex generator,
 * the naval mine and the warship yard that makes the warships. All of them can be built on deep water.
 */
public class HiemaraDefense{
	public static PowerTurret arcCoil, pulseLance, skyneedle;
	public static ItemTurret flakCannon, glacierHowitzer, frostshard, squallBattery;
	public static LiquidTurret tideCannon, whirlMortar, maelstrom;
	public static PowerTurret tideripper;
	public static StormTower stormcaller;
	public static TornadoTower tornadoTower;
	public static IceWall iceWall, iceWallLarge, iceWallHuge;
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
		
		flakCannon = new ItemTurret("flak-cannon"){{
			requirements(Category.turret, with(Items.copper, 85, Items.lead, 45, Items.sand, 30));
			size = 2;
			health = 520;
			range = 27f * 8f;
			reload = 18f;
			rotateSpeed = 15f;
			inaccuracy = 17f;
			shootCone = 35f;
			targetGround = false;
			placeableLiquid = true;
			shoot.shotDelay = 5f;
			shoot.shots = 2;
			shootSound = Sounds.shootScatter;
			ammo(
				Items.scrap, new FlakBulletType(4f, 3){{
					lifetime = 60f;
					ammoMultiplier = 5f;
					reloadMultiplier = 0.5f;
					width = 6f;
					height = 8f;
					hitEffect = Fx.flakExplosion;
					splashDamage = 33f;
					splashDamageRadius = 24f;
				}},
				Items.lead, new FlakBulletType(4.2f, 3){{
					lifetime = 60f;
					ammoMultiplier = 4f;
					width = 6f;
					height = 8f;
					hitEffect = Fx.flakExplosion;
					splashDamage = 40f;
					splashDamageRadius = 15f;
				}},
				Items.metaglass, new FlakBulletType(4f, 3){{
					lifetime = 60f;
					ammoMultiplier = 5f;
					width = 6f;
					height = 8f;
					hitEffect = Fx.flakExplosion;
					splashDamage = 45f;
					splashDamageRadius = 20f;
					fragBullets = 6;
					fragBullet = new mindustry.entities.bullet.BasicBulletType(3f, 5){{
						width = 5f;
						height = 12f;
						shrinkY = 1f;
						lifetime = 20f;
						backColor = Color.valueOf("ffe0a3");
						despawnEffect = Fx.none;
						collidesGround = false;
					}};
				}}
			);
		}};
		
		skyneedle = new PowerTurret("skyneedle"){{
			requirements(Category.turret, with(Items.lead, 150, Items.titanium, 80, Items.silicon, 100));
			size = 3;
			health = 1100;
			range = 36f * 8f;
			reload = 70f;
			rotateSpeed = 9f;
			targetGround = false;
			targetAir = true;
			placeableLiquid = true;
			shoot.shots = 3;
			shoot.shotDelay = 6f;
			shootSound = Sounds.shootMissile;
			consumePower(5f);
			shootType = new MissileBulletType(4.2f, 16f){{
				lifetime = 85f;
				homingPower = 0.18f;
				homingRange = 160f;
				splashDamage = 40f;
				splashDamageRadius = 28f;
				collidesGround = false;
				width = 7f;
				height = 10f;
				trailColor = Color.valueOf("bfe6ff");
				trailLength = 9;
				trailWidth = 1.6f;
				hitEffect = despawnEffect = Fx.blastExplosion;
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
		

		//Walls of ice: they only mend in a cold wave.
		iceWall = new IceWall("ice-wall"){{
			requirements(Category.defense, with(NHItems.ice, 6));
			health = 380;
			armor = 3f;
		}};
		iceWallLarge = new IceWall("ice-wall-large"){{
			requirements(Category.defense, with(NHItems.ice, 24));
			size = 2;
			health = 380 * 4;
			armor = 3f;
		}};
		iceWallHuge = new IceWall("ice-wall-huge"){{
			requirements(Category.defense, with(NHItems.ice, 54));
			size = 3;
			health = 380 * 9;
			armor = 3f;
		}};
		
		//Calls lightning down on every enemy in range, a few at a time.
		stormcaller = new StormTower("stormcaller"){{
			requirements(Category.turret, with(Items.lead, 200, Items.titanium, 120, Items.silicon, 150));
			size = 3;
			health = 1100;
			range = 24f;
			consumePower(9f);
		}};
		
		//Raises the tornado of the storm nuke on a spot picked by the player, when an enemy is near it.
		tornadoTower = new TornadoTower("tornado-tower"){{
			requirements(Category.turret, with(Items.lead, 300, Items.titanium, 200, Items.silicon, 250, Items.thorium, 100));
			size = 3;
			health = 1300;
			range = 30f;
			reload = 20f * 60f;
			consumePower(12f);
			tornado = new TornadoBulletType(){{
				speed = 0.3f;
				radius = 100f;
				lifetime = 60f * 8f;
				dps = 40f;
				buildingDps = 60f;
			}};
		}};
		
		tideripper = new PowerTurret("tideripper"){{
			requirements(Category.turret, with(Items.lead, 400, Items.titanium, 300, Items.silicon, 300, Items.thorium, 150, Items.tungsten, 60));
			size = 4;
			health = 2600;
			range = 52f * 8f;
			reload = 150f;
			rotateSpeed = 2.5f;
			recoil = 5f;
			targetAir = true;
			placeableLiquid = true;
			shoot.firstShotDelay = 40f;
			moveWhileCharging = false;
			accurateDelay = true;
			shootSound = Sounds.shootForeshadow;
			consumePower(14f);
			shootType = new RailBulletType(){{
				length = 52f * 8f;
				damage = 1100f;
				pierceDamageFactor = 0.4f;
				buildingDamageMultiplier = 0.4f;
				hitColor = Color.valueOf("9fe3ff");
				pointEffectSpace = 50f;
				shootEffect = Fx.shootBig2;
				hitEffect = Fx.massiveExplosion;
				smokeEffect = Fx.shootBigSmoke;
				ammoMultiplier = 1f;
			}};
		}};
		
		glacierHowitzer = new ItemTurret("glacier-howitzer"){{
			requirements(Category.turret, with(Items.lead, 350, Items.titanium, 250, Items.silicon, 150, Items.thorium, 100));
			size = 4;
			health = 2500;
			range = 46f * 8f;
			reload = 90f;
			rotateSpeed = 3f;
			recoil = 4f;
			inaccuracy = 1f;
			targetAir = false;
			placeableLiquid = true;
			shootSound = Sounds.shootArtillery;
			float life = range / 3f + 4f;
			ammo(
				NHItems.ice, new ArtilleryBulletType(3f, 20f){{
					lifetime = life;
					splashDamage = 160f;
					splashDamageRadius = 64f;
					status = StatusEffects.freezing;
					statusDuration = 60f * 6f;
					hitEffect = Fx.blastExplosion;
					knockback = 1.2f;
					width = height = 14f;
					backColor = trailColor = Color.valueOf("9fe3ff");
					frontColor = Color.white;
					ammoMultiplier = 1f;
				}},
				Items.silicon, new ArtilleryBulletType(3f, 20f){{
					lifetime = life;
					splashDamage = 125f;
					splashDamageRadius = 52f;
					homingPower = 0.08f;
					homingRange = 70f;
					reloadMultiplier = 1.2f;
					hitEffect = Fx.blastExplosion;
					width = height = 12f;
					backColor = trailColor = Color.valueOf("bfc7d9");
					ammoMultiplier = 2f;
				}},
				Items.pyratite, new ArtilleryBulletType(3f, 20f){{
					lifetime = life;
					splashDamage = 140f;
					splashDamageRadius = 60f;
					status = StatusEffects.burning;
					statusDuration = 60f * 6f;
					makeFire = true;
					hitEffect = Fx.blastExplosion;
					width = height = 13f;
					backColor = trailColor = Color.valueOf("ffaa5f");
					ammoMultiplier = 2f;
				}},
				Items.thorium, new ArtilleryBulletType(3f, 20f){{
					lifetime = life;
					splashDamage = 230f;
					splashDamageRadius = 72f;
					knockback = 2f;
					hitEffect = Fx.massiveExplosion;
					width = height = 16f;
					backColor = trailColor = Color.valueOf("f9a3c7");
					ammoMultiplier = 1f;
				}}
			);
		}};
		
		frostshard = new ItemTurret("frostshard-battery"){{
			requirements(Category.turret, with(Items.lead, 200, Items.titanium, 150, Items.silicon, 120, NHItems.ice, 80));
			size = 3;
			health = 1500;
			range = 24f * 8f;
			reload = 7f;
			rotateSpeed = 8f;
			inaccuracy = 3f;
			targetAir = true;
			placeableLiquid = true;
			shoot = new ShootAlternate(10f);
			shootSound = Sounds.shootSalvo;
			ammo(
				NHItems.ice, new BasicBulletType(7f, 22f){{
					width = 7f;
					height = 16f;
					lifetime = 24f * 8f / 7f;
					status = StatusEffects.freezing;
					statusDuration = 60f * 3f;
					backColor = Color.valueOf("8fd8ff");
					frontColor = Color.white;
					ammoMultiplier = 3f;
				}},
				Items.titanium, new BasicBulletType(7.5f, 32f){{
					width = 7f;
					height = 18f;
					lifetime = 24f * 8f / 7.5f;
					pierce = true;
					pierceCap = 3;
					backColor = Color.valueOf("a6c4ff");
					frontColor = Color.white;
					ammoMultiplier = 3f;
				}},
				Items.silicon, new BasicBulletType(7f, 20f){{
					width = 7f;
					height = 16f;
					lifetime = 24f * 8f / 7f;
					homingPower = 0.1f;
					homingRange = 80f;
					reloadMultiplier = 1.3f;
					backColor = Color.valueOf("bfc7d9");
					frontColor = Color.white;
					ammoMultiplier = 4f;
				}}
			);
		}};
		
		maelstrom = new LiquidTurret("maelstrom"){{
			requirements(Category.turret, with(Items.lead, 400, Items.titanium, 300, Items.silicon, 200, Items.metaglass, 150));
			size = 4;
			health = 2800;
			range = 30f * 8f;
			reload = 3f;
			inaccuracy = 3f;
			shootCone = 50f;
			recoil = 0f;
			liquidCapacity = 120f;
			targetAir = true;
			extinguish = true;
			placeableLiquid = true;
			shoot.shots = 2;
			shootEffect = Fx.shootLiquid;
			ammo(
				Liquids.water, new LiquidBulletType(Liquids.water){{
					damage = 16f;
					knockback = 2.4f;
					drag = 0.005f;
					layer = Layer.bullet - 2f;
					lifetime = 56f;
					speed = 4.6f;
					puddleSize = 8f;
					orbSize = 4f;
					collidesAir = true;
				}},
				Liquids.oil, new LiquidBulletType(Liquids.oil){{
					damage = 22f;
					knockback = 1.6f;
					drag = 0.005f;
					layer = Layer.bullet - 2f;
					lifetime = 56f;
					speed = 4.6f;
					puddleSize = 8f;
					orbSize = 4f;
					collidesAir = true;
					status = StatusEffects.tarred;
					statusDuration = 60f * 5f;
				}}
			);
		}};
		
		squallBattery = new ItemTurret("squall-battery"){{
			requirements(Category.turret, with(Items.lead, 400, Items.titanium, 250, Items.silicon, 250, Items.plastanium, 100, Items.thorium, 80));
			size = 4;
			health = 3000;
			range = 40f * 8f;
			reload = 8f;
			rotateSpeed = 9f;
			inaccuracy = 6f;
			shootCone = 30f;
			targetAir = true;
			targetGround = true;
			placeableLiquid = true;
			shoot = new ShootAlternate(16f);
			shootSound = Sounds.shootScatter;
			float life = range / 5f + 4f;
			ammo(
				Items.metaglass, new FlakBulletType(5f, 6f){{
					lifetime = life;
					collidesGround = true;
					width = 8f;
					height = 10f;
					splashDamage = 55f;
					splashDamageRadius = 34f;
					fragBullets = 8;
					fragBullet = new BasicBulletType(3.2f, 8f){{
						width = 5f;
						height = 12f;
						shrinkY = 1f;
						lifetime = 22f;
						backColor = Color.valueOf("ffe0a3");
						despawnEffect = Fx.none;
					}};
					hitEffect = Fx.flakExplosion;
					ammoMultiplier = 3f;
				}},
				Items.silicon, new FlakBulletType(5.2f, 6f){{
					lifetime = life;
					collidesGround = true;
					width = 8f;
					height = 10f;
					splashDamage = 70f;
					splashDamageRadius = 32f;
					homingPower = 0.12f;
					homingRange = 60f;
					reloadMultiplier = 1.2f;
					hitEffect = Fx.flakExplosion;
					ammoMultiplier = 4f;
				}},
				Items.plastanium, new FlakBulletType(5f, 6f){{
					lifetime = life;
					collidesGround = true;
					width = 9f;
					height = 12f;
					splashDamage = 95f;
					splashDamageRadius = 42f;
					fragBullets = 12;
					fragBullet = new BasicBulletType(3.4f, 10f){{
						width = 5f;
						height = 12f;
						shrinkY = 1f;
						lifetime = 24f;
						backColor = Color.valueOf("e8d174");
						despawnEffect = Fx.none;
					}};
					hitEffect = Fx.plasticExplosion;
					ammoMultiplier = 2f;
				}},
				Items.blastCompound, new FlakBulletType(4.6f, 6f){{
					lifetime = life;
					collidesGround = true;
					width = 9f;
					height = 12f;
					splashDamage = 140f;
					splashDamageRadius = 52f;
					hitEffect = Fx.blastExplosion;
					ammoMultiplier = 2f;
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
				new UnitPlan(HiemaraWarships.skywatch, 50f * 60f, with(Items.lead, 200, Items.titanium, 100, Items.silicon, 100, Items.coal, 40)),
				new UnitPlan(HiemaraWarships.icebreaker, 40f * 60f, with(Items.lead, 150, Items.titanium, 120, Items.silicon, 60, Items.coal, 30)),
				new UnitPlan(HiemaraWarships.medicSkiff, 18f * 60f, with(Items.copper, 40, Items.lead, 60, Items.sand, 40)),
				new UnitPlan(HiemaraWarships.builderSkiff, 16f * 60f, with(Items.copper, 60, Items.lead, 40, Items.sand, 20)),
				new UnitPlan(HiemaraWarships.builderBarge, 30f * 60f, with(Items.copper, 80, Items.lead, 150, Items.titanium, 40, Items.silicon, 40)),
				new UnitPlan(HiemaraWarships.rescueFrigate, 60f * 60f, with(Items.lead, 250, Items.titanium, 150, Items.silicon, 150, Items.coal, 40)),
				new UnitPlan(HiemaraWarships.overdriveTender, 70f * 60f, with(Items.lead, 300, Items.titanium, 200, Items.silicon, 200, Items.phaseFabric, 40)),
				new UnitPlan(HiemaraWarships.aircraftCarrier, 200f * 60f, with(Items.lead, 900, Items.titanium, 700, Items.silicon, 600, Items.thorium, 300, Items.tungsten, 120))
			);
		}};
		
		all.addAll(flakCannon, skyneedle, arcCoil, pulseLance, tideCannon, whirlMortar, vortexGenerator, navalMine, warshipYard,
			iceWall, iceWallLarge, iceWallHuge, stormcaller, tornadoTower, tideripper, glacierHowitzer, frostshard, maelstrom, squallBattery);
	}
}
