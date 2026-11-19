package newhorizon.content.fleet;

import arc.graphics.Color;
import arc.func.Prov;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import mindustry.ai.UnitStance;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.content.StatusEffects;
import mindustry.entities.abilities.ForceFieldAbility;
import mindustry.entities.abilities.StatusFieldAbility;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.BombBulletType;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.bullet.ContinuousLaserBulletType;
import mindustry.entities.bullet.FlakBulletType;
import mindustry.entities.bullet.LaserBulletType;
import mindustry.entities.bullet.MissileBulletType;
import mindustry.entities.bullet.LightningBulletType;
import mindustry.gen.Sounds;
import mindustry.gen.UnitEntity;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.type.weapons.RepairBeamWeapon;
import mindustry.world.blocks.units.UnitFactory;
import newhorizon.content.ricochet.RicochetBlocks;
import newhorizon.expand.bullets.ricochet.RicochetBulletType;
import newhorizon.expand.units.ablility.KeepStatusAbility;
import newhorizon.expand.units.ablility.RamAuraAbility;
import newhorizon.expand.units.ablility.DroneBayAbility;
import newhorizon.expand.units.ablility.DroneStance;
import newhorizon.expand.units.ablility.StandDownStance;
import newhorizon.expand.units.ai.DroneAI;
import newhorizon.expand.units.ai.RammerAI;
import newhorizon.expand.units.ai.ShooterDroneAI;
import newhorizon.expand.units.unitType.NHUnitType;

import static mindustry.type.ItemStack.with;

/**
 * The Mothership and its Rammers. The Mothership (built in the Air Factory) is a big, tough flyer with a repair beam, a shield projector and a speed
 * projector; it carries four Rammers ({@link RammerBayAbility}) that attack what it attacks and come back to it to be healed. A Rammer is a very
 * fast, very tough flyer that is slow to speed up but turns sharply ({@link RammerAI}); the faster it moves the harder it hits everything it
 * touches ({@link RamAuraAbility}), and every hit hurts it too.
 */
public class FleetUnits{
	public static NHUnitType rammer, gunnerDrone, laserDrone, teslaDrone, bomberDrone, missileDrone, napalmDrone, continuousLaserDrone, mothership, mothershipII, mothershipIII;
	/** The Stand down button of every Mothership. */
	public static UnitStance standDownStance;
	/** The stance buttons that pick the Mothership's drone type, in the order of the drone types. */
	public static final Seq<UnitStance> droneStances = new Seq<>();
	public static final Color outline = Color.valueOf("2c2a33"), glow = Color.valueOf("ff9a4d");

	public static void load(){
		FleetStatus.load();
		FleetLiquids.load();

		rammer = new NHUnitType("rammer"){{
			constructor = UnitEntity::create;
			aiController = RammerAI::new;
			outlineColor = outline;
			drawCell = false;
			playerControllable = false;
			allowChangeCommands = false;
			useUnitCap = false;

			flying = true;
			lowAltitude = true;
			health = 3000f;
			armor = 75f;
			speed = 6.5f;
			accel = 0.05f;
			drag = 0.003f;
			rotateSpeed = 20f;
			hitSize = 18f;
			range = maxRange = 60f;
			engineOffset = 9f;
			engineSize = 4f;
			engineColor = glow;
			immunities = ObjectSet.with(StatusEffects.burning, StatusEffects.melting, StatusEffects.corroded, StatusEffects.sapped,
				StatusEffects.shocked, StatusEffects.electrified, StatusEffects.freezing);

			abilities.add(new KeepStatusAbility(FleetStatus.bulwark));
			abilities.add(new RamAuraAbility());
		}};

		gunnerDrone = drone("gunner-drone", 600f, 6f, 3.2f, 10f, () -> shooter(0.7f, 7f, 0.08f, 0.2f), new Color[]{Color.valueOf("ffd36b")});
		gunnerDrone.weapons.add(new Weapon(){{
			x = 4f;
			y = 2f;
			mirror = true;
			alternate = true;
			rotate = true;
			reload = 7f;
			ejectEffect = Fx.none;
			shootSound = Sounds.shoot;
			bullet = new BasicBulletType(7f, 12f){{
				lifetime = 24f;
				width = 6f;
				height = 14f;
				backColor = trailColor = lightColor = Color.valueOf("ffb347");
				frontColor = Color.valueOf("fff1c9");
				hitEffect = Fx.hitBulletColor;
				hitColor = Color.valueOf("ffb347");
			}};
		}});

		laserDrone = drone("laser-drone", 700f, 8f, 2.6f, 12f, () -> shooter(0.8f, 6f, 0.07f, 0.18f), new Color[]{Color.valueOf("ff6b6b")});
		laserDrone.weapons.add(new Weapon(){{
			x = 0f;
			y = 4f;
			mirror = false;
			rotate = true;
			reload = 55f;
			ejectEffect = Fx.none;
			shootSound = Sounds.shootLaser;
			bullet = new LaserBulletType(110f){{
				length = 200f;
				width = 14f;
				colors = new Color[]{Color.valueOf("ff6b6b").a(0.4f), Color.valueOf("ff6b6b"), Color.white};
				hitColor = Color.valueOf("ff6b6b");
				lightColor = Color.valueOf("ff6b6b");
			}};
		}});

		teslaDrone = drone("tesla-drone", 650f, 6f, 3.0f, 10f, () -> shooter(0.6f, 7f, 0.08f, 0.2f), new Color[]{Color.valueOf("6fe8ff")});
		teslaDrone.weapons.add(new Weapon(){{
			x = 3f;
			y = 2f;
			mirror = true;
			alternate = true;
			rotate = true;
			reload = 14f;
			ejectEffect = Fx.none;
			shootSound = Sounds.shootArc;
			bullet = tesla();
		}});

		bomberDrone = drone("bomber-drone", 900f, 10f, 3.0f, 14f, () -> {
			RammerAI ai = new RammerAI();
			ai.turnRate = 5f;
			ai.accelRate = 0.07f;
			ai.brakeRate = 0.18f;
			ai.runOut = 90f;
			return ai;
		}, new Color[]{Color.valueOf("b58cff")});
		bomberDrone.targetAir = false;
		bomberDrone.weapons.add(new Weapon(){{
			x = 0f;
			y = 0f;
			mirror = false;
			rotate = false;
			ignoreRotation = true;
			shootCone = 180f;
			minShootVelocity = 0.75f;
			reload = 12f;
			inaccuracy = 10f;
			ejectEffect = Fx.none;
			shootSound = Sounds.none;
			bullet = new BombBulletType(40f, 36f){{
				width = 10f;
				height = 14f;
				hitEffect = Fx.flakExplosion;
				shootEffect = Fx.none;
				smokeEffect = Fx.none;
				status = StatusEffects.blasted;
				statusDuration = 60f;
			}};
		}});

		missileDrone = drone("missile-drone", 700f, 6f, 2.8f, 12f, () -> shooter(0.75f, 6f, 0.07f, 0.18f), new Color[]{Color.valueOf("ff8a6b")});
		missileDrone.weapons.add(new Weapon(){{
			x = 0f;
			y = 3f;
			mirror = false;
			rotate = true;
			reload = 100f;
			shoot.shots = 6;
			shoot.shotDelay = 4f;
			inaccuracy = 8f;
			velocityRnd = 0.15f;
			ejectEffect = Fx.none;
			shootSound = Sounds.shootMissileSmall;
			bullet = new MissileBulletType(4.2f, 10f){{
				lifetime = 55f;
				width = 7f;
				height = 10f;
				homingPower = 0.08f;
				homingRange = 90f;
				splashDamage = 22f;
				splashDamageRadius = 28f;
				backColor = trailColor = lightColor = Color.valueOf("ff8a6b");
				frontColor = Color.valueOf("fff1ea");
				hitEffect = Fx.blastExplosion;
				despawnEffect = Fx.blastExplosion;
			}};
		}});

		napalmDrone = drone("napalm-drone", 850f, 8f, 2.9f, 14f, () -> {
			RammerAI ai = new RammerAI();
			ai.turnRate = 5f;
			ai.accelRate = 0.07f;
			ai.brakeRate = 0.18f;
			ai.runOut = 90f;
			return ai;
		}, new Color[]{Color.valueOf("ff5a36")});
		napalmDrone.targetAir = false;
		napalmDrone.weapons.add(new Weapon(){{
			x = 0f;
			y = 0f;
			mirror = false;
			rotate = false;
			ignoreRotation = true;
			shootCone = 180f;
			minShootVelocity = 0.75f;
			reload = 10f;
			inaccuracy = 10f;
			ejectEffect = Fx.none;
			shootSound = Sounds.none;
			//a small blast; what it does is the fire and the burning oil it leaves
			bullet = new BombBulletType(10f, 40f){{
				width = 10f;
				height = 14f;
				splashDamage = 14f;
				hitEffect = Fx.fireballsmoke;
				shootEffect = Fx.none;
				smokeEffect = Fx.none;
				backColor = lightColor = Color.valueOf("ff7a3a");
				frontColor = Color.valueOf("ffd36b");
				status = StatusEffects.burning;
				statusDuration = 300f;
				makeFire = true;
				incendAmount = 3;
				incendSpread = 20f;
				incendChance = 1f;
				puddleLiquid = FleetLiquids.napalm;
				puddles = 3;
				puddleRange = 24f;
				puddleAmount = 20f;
			}};
		}});

		continuousLaserDrone = drone("continuous-laser-drone", 800f, 8f, 2.4f, 13f, () -> shooter(0.75f, 6f, 0.07f, 0.18f), new Color[]{Color.valueOf("8fc8ff")});
		continuousLaserDrone.weapons.add(new Weapon(){{
			x = 0f;
			y = 4f;
			mirror = false;
			rotate = true;
			reload = 90f;
			shoot.firstShotDelay = 25f;
			continuous = true;
			ejectEffect = Fx.none;
			shootSound = Sounds.shootBeamPlasma;
			bullet = new ContinuousLaserBulletType(){{
				damage = 14f;
				length = 190f;
				lifetime = 100f;
				width = 5f;
				drawSize = 420f;
				hitEffect = Fx.hitMeltdown;
				hitColor = Color.valueOf("8fc8ff");
				lightColor = Color.valueOf("8fc8ff");
				status = StatusEffects.burning;
				incendChance = 0.1f;
				incendSpread = 5f;
				incendAmount = 1;
				colors = new Color[]{Color.valueOf("8fc8ff").a(0.35f), Color.valueOf("8fc8ff").a(0.7f), Color.valueOf("d8ecff"), Color.white};
			}};
		}});

		//the buttons that pick the type, in the order of the types: only one on at a time
		Seq<UnitType> types = Seq.with(rammer, gunnerDrone, laserDrone, teslaDrone, bomberDrone, missileDrone, napalmDrone, continuousLaserDrone);
		droneStances.clear();
		for(UnitType type : types) droneStances.add(new DroneStance(type));
		for(UnitStance a : droneStances) for(UnitStance b : droneStances) if(a != b) a.incompatibleStances.add(b);

		standDownStance = new StandDownStance();

		//The three tiers, from one builder: drones, health, armour, speed, hit size, range, shield (radius, strength, recharge), repair beam pairs and
		//strength, flak pairs, speed projector range, seconds to replace a lost drone, and the scale of the weapon positions.
		mothership = carrier("mothership", types, 4, 24000f, 25f, 0.9f, 1.2f, 44f, 650f, 100f, 6000f, 2.4f, 1, 2.5f, 1, 260f, 40f, 1f);
		mothershipII = carrier("mothership-ii", types, 8, 48000f, 35f, 0.8f, 1.0f, 58f, 720f, 130f, 12000f, 4.8f, 2, 3.5f, 2, 340f, 30f, 1.3f);
		mothershipIII = carrier("mothership-iii", types, 12, 90000f, 45f, 0.7f, 0.8f, 74f, 800f, 160f, 24000f, 9.6f, 2, 5f, 3, 420f, 20f, 1.65f);
		//Tiers II and III are made by upgrading in the Fleet Reconstructor, not in a factory: what they cost to research is set here.
		mothershipII.setRequirements(with(Items.silicon, 1500, Items.titanium, 1000, Items.thorium, 700, Items.plastanium, 500, Items.surgeAlloy, 250));
		mothershipIII.setRequirements(with(Items.silicon, 3000, Items.titanium, 2000, Items.thorium, 1400, Items.plastanium, 1000, Items.surgeAlloy, 600));

		//Built in the Air Factory, after what it already makes (so that the numbers saved in factories stay right).
		((UnitFactory)Blocks.airFactory).plans.add(new UnitFactory.UnitPlan(mothership, 60f * 120f,
			with(Items.silicon, 800, Items.titanium, 600, Items.thorium, 400, Items.plastanium, 300, Items.surgeAlloy, 100)));
	}

	/**
	 * A Mothership of one tier. Its own weapons are short range (repair beams that mend the drones and allied units, flak that shoots ground and air by
	 * itself), it fights with its drones: the game's AIs close in to 80% of its range from a target, so it keeps far off. Besides the drone bay it has a
	 * shield projector and a speed projector. The sizes of the weapon positions follow {@code scale}.
	 */
	protected static NHUnitType carrier(String name, Seq<UnitType> types, int drones, float hpV, float armorV, float speedV, float turnV, float hitV, float rangeV,
		float shieldRadius, float shieldMax, float shieldRegen, int beamPairs, float beamRepair, int flakPairs, float rallyRange, float replaceSeconds, float scale){
		return new NHUnitType(name){{
			constructor = UnitEntity::create;
			outlineColor = outline;
			drawCell = false;
			stances = droneStances.copy();
			stances.add(standDownStance);

			flying = true;
			lowAltitude = false;
			health = hpV;
			armor = armorV;
			speed = speedV;
			accel = 0.02f;
			drag = 0.04f;
			rotateSpeed = turnV;
			hitSize = hitV;
			range = maxRange = rangeV;
			engineOffset = 30f * scale;
			engineSize = 10f * scale;
			engineColor = glow;
			immunities = ObjectSet.with(StatusEffects.burning, StatusEffects.melting, StatusEffects.corroded, StatusEffects.sapped);

			//the healing turrets: the drones hover near them to be mended
			for(int i = 0; i < beamPairs; i++){
				final int k = i;
				weapons.add(new RepairBeamWeapon("repair-beam-weapon-center"){{
					x = (18f + 4f * k) * scale;
					y = (-4f - 12f * k) * scale;
					shootY = 6f;
					mirror = true;
					beamWidth = 0.9f;
					repairSpeed = beamRepair;
					fractionRepairSpeed = 0.05f;
					targetUnits = true;
					targetBuildings = true;
					bullet = new BulletType(){{
						maxRange = 200f;
					}};
				}});
			}

			//the flak turrets: short range, ground and air, they pick their own targets whatever the Mothership is attacking
			for(int i = 0; i < flakPairs; i++){
				final int k = i;
				weapons.add(new Weapon(){{
					x = 24f * scale;
					y = (14f - 14f * k) * scale;
					mirror = true;
					alternate = true;
					rotate = true;
					controllable = false;
					autoTarget = true;
					reload = 16f;
					ejectEffect = Fx.none;
					shootSound = Sounds.shootScatter;
					bullet = new FlakBulletType(4.2f, 3f){{
						lifetime = 36f;
						width = 7f;
						height = 9f;
						explodeRange = 26f;
						collidesGround = true;
						splashDamage = 30f;
						splashDamageRadius = 36f;
						backColor = trailColor = lightColor = Color.valueOf("ffb347");
						frontColor = Color.valueOf("fff1c9");
						hitEffect = Fx.flakExplosion;
						despawnEffect = Fx.flakExplosion;
					}};
				}});
			}

			DroneBayAbility bay = new DroneBayAbility(drones, types, droneStances);
			bay.standDownStance = standDownStance;
			bay.rebuildTime = 60f * replaceSeconds;
			abilities.add(bay);
			//the shield projector
			abilities.add(new ForceFieldAbility(shieldRadius, shieldRegen, shieldMax, 400f));
			//the speed projector
			abilities.add(new StatusFieldAbility(FleetStatus.rally, 120f, 36f, rallyRange));
		}};
	}

	/** A drone: a small flyer launched only by the Mothership (no factory, no unit cap, no commands of its own), whose weapons all aim at the one target its AI chose. */
	protected static NHUnitType drone(String name, float hpValue, float armorValue, float speedValue, float sizeValue, Prov<? extends DroneAI> aiValue, Color[] accent){
		return new NHUnitType(name){{
			constructor = UnitEntity::create;
			aiController = aiValue;
			outlineColor = outline;
			drawCell = false;
			playerControllable = false;
			allowChangeCommands = false;
			useUnitCap = false;
			singleTarget = true;

			flying = true;
			lowAltitude = true;
			health = hpValue;
			armor = armorValue;
			speed = speedValue;
			accel = 0.08f;
			drag = 0.003f;
			rotateSpeed = 20f;
			hitSize = sizeValue;
			engineOffset = sizeValue * 0.5f;
			engineSize = 3f;
			engineColor = accent[0];
		}};
	}

	/** A shooting drone's brain: how far it stands off (share of its range) and how fast it speeds up and brakes. */
	protected static ShooterDroneAI shooter(float standOff, float turn, float accel, float brake){
		ShooterDroneAI ai = new ShooterDroneAI();
		ai.standOff = standOff;
		ai.turnRate = turn;
		ai.accelRate = accel;
		ai.brakeRate = brake;
		return ai;
	}

	/** The Tesla drone's bolt: the Ricochet turret's. It turns into arcs that jump from enemy to enemy, 8 times, at full damage, and electrify what they hit. */
	protected static BulletType tesla(){
		RicochetBulletType b = RicochetBlocks.bolt(8, Color.valueOf("6fe8ff"), 22f, StatusEffects.electrified);
		b.lifetime = 22f;   // 8 px a tick: reaches about 176 px
		return b;
	}
}
