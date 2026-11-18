package newhorizon.content.fleet;

import arc.graphics.Color;
import arc.func.Prov;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import mindustry.ai.UnitStance;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.StatusEffects;
import mindustry.entities.abilities.ForceFieldAbility;
import mindustry.entities.abilities.StatusFieldAbility;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.BombBulletType;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.bullet.LaserBulletType;
import mindustry.entities.bullet.LightningBulletType;
import mindustry.gen.Sounds;
import mindustry.gen.UnitEntity;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.type.weapons.RepairBeamWeapon;
import mindustry.world.blocks.units.UnitFactory;
import newhorizon.expand.units.ablility.KeepStatusAbility;
import newhorizon.expand.units.ablility.RamAuraAbility;
import newhorizon.expand.units.ablility.DroneBayAbility;
import newhorizon.expand.units.ablility.DroneStance;
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
	public static NHUnitType rammer, gunnerDrone, laserDrone, teslaDrone, bomberDrone, mothership;
	/** The stance buttons that pick the Mothership's drone type, in the order of the drone types. */
	public static final Seq<UnitStance> droneStances = new Seq<>();
	public static final Color outline = Color.valueOf("2c2a33"), glow = Color.valueOf("ff9a4d");

	public static void load(){
		FleetStatus.load();

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
			reload = 16f;
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

		//the buttons that pick the type, in the order of the types: only one on at a time
		Seq<UnitType> types = Seq.with(rammer, gunnerDrone, laserDrone, teslaDrone, bomberDrone);
		droneStances.clear();
		for(UnitType type : types) droneStances.add(new DroneStance(type));
		for(UnitStance a : droneStances) for(UnitStance b : droneStances) if(a != b) a.incompatibleStances.add(b);

		mothership = new NHUnitType("mothership"){{
			constructor = UnitEntity::create;
			outlineColor = outline;
			drawCell = false;
			stances = droneStances.copy();

			flying = true;
			lowAltitude = false;
			health = 24000f;
			armor = 25f;
			speed = 0.9f;
			accel = 0.02f;
			drag = 0.04f;
			rotateSpeed = 1.2f;
			hitSize = 56f;
			engineOffset = 30f;
			engineSize = 10f;
			engineColor = glow;
			immunities = ObjectSet.with(StatusEffects.burning, StatusEffects.melting, StatusEffects.corroded, StatusEffects.sapped);

			//the healing turret: the Rammers hover near it to be mended
			weapons.add(new RepairBeamWeapon("repair-beam-weapon-center"){{
				x = 18f;
				y = -4f;
				shootY = 6f;
				mirror = true;
				beamWidth = 0.9f;
				repairSpeed = 2.5f;
				fractionRepairSpeed = 0.05f;
				targetUnits = true;
				targetBuildings = true;
				bullet = new BulletType(){{
					maxRange = 200f;
				}};
			}});

			abilities.add(new DroneBayAbility(4, types, droneStances));
			//the shield projector
			abilities.add(new ForceFieldAbility(190f, 2.4f, 6000f, 400f));
			//the speed projector
			abilities.add(new StatusFieldAbility(FleetStatus.rally, 120f, 36f, 260f));
		}};

		//Built in the Air Factory, after what it already makes (so that the numbers saved in factories stay right).
		((UnitFactory)Blocks.airFactory).plans.add(new UnitFactory.UnitPlan(mothership, 60f * 120f,
			with(Items.silicon, 800, Items.titanium, 600, Items.thorium, 400, Items.plastanium, 300, Items.surgeAlloy, 100)));
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

	/** Electric arcs that electrify what they hit (units and buildings alike). */
	protected static BulletType tesla(){
		Color arc = Color.valueOf("6fe8ff");
		return new LightningBulletType(){{
			damage = 20f;
			lightningLength = 14;
			lightningColor = arc;
			collidesAir = true;
			collidesGround = true;
			rangeChange = 35f;   // the arcs reach 80 px by themselves
			ammoMultiplier = 1f;
			lightningType = new BulletType(0.0001f, 0f){{
				lifetime = Fx.lightning.lifetime;
				hitEffect = Fx.hitLancer;
				despawnEffect = Fx.none;
				status = StatusEffects.electrified;
				statusDuration = 120f;
				hittable = false;
				lightColor = Color.white;
				collidesAir = true;
			}};
		}};
	}
}
