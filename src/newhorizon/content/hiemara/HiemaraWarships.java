package newhorizon.content.hiemara;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.abilities.UnitSpawnAbility;
import mindustry.entities.bullet.ArtilleryBulletType;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.MissileBulletType;
import mindustry.gen.Sounds;
import mindustry.gen.UnitEntity;
import mindustry.gen.UnitWaterMove;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import newhorizon.expand.units.hiemara.SubmarineUnitType;

/**
 * The warships of Hiemara, built in the warship yard. Light boats first, then missile ships, a submarine, long range gun
 * ships and an aircraft carrier. All of them are ordinary units, with the ordinary unit cap and commands.
 */
public class HiemaraWarships{
	public static UnitType patrolBoat, corvette, torpedoBoat, mortarSkiff, missileFrigate, abyss, longbow, aircraftCarrier, interceptor;
	
	public static final Seq<UnitType> all = new Seq<>();
	
	public static void load(){
		interceptor = new UnitType("interceptor"){{
			constructor = UnitEntity::create;
			flying = true;
			health = 120f;
			hitSize = 8f;
			speed = 3.6f;
			accel = 0.08f;
			drag = 0.04f;
			rotateSpeed = 12f;
			lowAltitude = true;
			circleTarget = true;
			faceTarget = true;
			engineOffset = 5f;
			engineSize = 2.6f;
			targetGround = true;
			
			weapons.add(new Weapon(){{
				mirror = true;
				x = 3f;
				y = 1f;
				reload = 38f;
				alternate = true;
				shootSound = Sounds.shootMissile;
				bullet = missile(3.6f, 14f, 0.1f, 14f, 22f, 36);
			}});
		}};
		
		patrolBoat = new UnitType("patrol-boat"){{
			constructor = UnitWaterMove::create;
			health = 300f;
			hitSize = 11f;
			speed = 1.15f;
			accel = 0.4f;
			drag = 0.13f;
			rotateSpeed = 3.3f;
			faceTarget = false;
			armor = 2f;
			
			weapons.add(new Weapon(){{
				mirror = true;
				alternate = true;
				x = 3.5f;
				y = 3f;
				reload = 13f;
				shootSound = Sounds.shoot;
				bullet = new BasicBulletType(3.2f, 11f){{
					width = 5f;
					height = 7f;
					lifetime = 42f;
					frontColor = Color.valueOf("ffe0a3");
					backColor = Color.valueOf("f29c4a");
				}};
			}});
		}};
		
		corvette = new UnitType("corvette"){{
			constructor = UnitWaterMove::create;
			health = 560f;
			hitSize = 14f;
			speed = 1.0f;
			accel = 0.3f;
			drag = 0.14f;
			rotateSpeed = 2.8f;
			faceTarget = false;
			armor = 4f;
			
			weapons.add(new Weapon(){{
				mirror = true;
				alternate = true;
				x = 4.5f;
				y = 1f;
				reload = 50f;
				shootSound = Sounds.shootMissile;
				bullet = missile(3f, 14f, 0.12f, 18f, 28f, 50);
			}});
		}};
		
		torpedoBoat = new UnitType("torpedo-boat"){{
			constructor = UnitWaterMove::create;
			health = 420f;
			hitSize = 11f;
			speed = 1.35f;
			accel = 0.35f;
			drag = 0.12f;
			rotateSpeed = 3.6f;
			faceTarget = false;
			armor = 2f;
			targetAir = false;
			
			weapons.add(new Weapon(){{
				mirror = false;
				x = 0f;
				y = 5f;
				reload = 100f;
				rotate = false;
				shootSound = Sounds.shootMissile;
				bullet = torpedo(2.4f, 100f, 44f, 62);
			}});
		}};
		
		mortarSkiff = new UnitType("mortar-skiff"){{
			constructor = UnitWaterMove::create;
			health = 480f;
			hitSize = 12f;
			speed = 0.95f;
			accel = 0.3f;
			drag = 0.14f;
			rotateSpeed = 2.8f;
			faceTarget = false;
			armor = 3f;
			targetAir = false;
			
			weapons.add(new Weapon(){{
				mirror = false;
				x = 0f;
				y = -1f;
				reload = 70f;
				rotate = true;
				shootSound = Sounds.shootArtillery;
				bullet = shell(3f, 20f, 64f, 38f, 60);
			}});
		}};
		
		missileFrigate = new UnitType("missile-frigate"){{
			constructor = UnitWaterMove::create;
			health = 2100f;
			hitSize = 24f;
			speed = 0.78f;
			accel = 0.2f;
			drag = 0.17f;
			rotateSpeed = 1.7f;
			faceTarget = false;
			armor = 8f;
			
			for(float py : new float[]{7f, -7f}){
				weapons.add(new Weapon(){{
					mirror = true;
					alternate = true;
					x = 8f;
					y = py;
					reload = 80f;
					shoot.shots = 3;
					shoot.shotDelay = 6f;
					shootSound = Sounds.shootMissile;
					bullet = missile(3.2f, 20f, 0.13f, 34f, 34f, 80);
				}});
			}
		}};
		
		abyss = new SubmarineUnitType("abyss"){{
			constructor = UnitWaterMove::create;
			health = 1250f;
			hitSize = 17f;
			speed = 0.95f;
			accel = 0.25f;
			drag = 0.15f;
			rotateSpeed = 2.4f;
			faceTarget = false;
			armor = 5f;
			targetAir = false;
			
			weapons.add(new Weapon(){{
				mirror = true;
				alternate = true;
				x = 4.5f;
				y = 8f;
				reload = 110f;
				rotate = false;
				shootSound = Sounds.shootMissile;
				bullet = torpedo(2.6f, 130f, 50f, 70);
			}});
		}};
		
		longbow = new UnitType("longbow"){{
			constructor = UnitWaterMove::create;
			health = 4200f;
			hitSize = 34f;
			speed = 0.6f;
			accel = 0.15f;
			drag = 0.2f;
			rotateSpeed = 1.2f;
			faceTarget = false;
			armor = 11f;
			targetAir = false;
			
			weapons.add(new Weapon(){{
				mirror = true;
				alternate = true;
				x = 9f;
				y = 10f;
				reload = 140f;
				rotate = true;
				shootSound = Sounds.shootArtillery;
				bullet = shell(3.6f, 30f, 150f, 52f, 100);
			}});
		}};
		
		aircraftCarrier = new UnitType("aircraft-carrier"){{
			constructor = UnitWaterMove::create;
			health = 8200f;
			hitSize = 48f;
			speed = 0.55f;
			accel = 0.12f;
			drag = 0.22f;
			rotateSpeed = 1f;
			faceTarget = false;
			armor = 14f;
			
			abilities.add(new UnitSpawnAbility(interceptor, 60f * 7f, 0f, -14f));
			
			weapons.add(new Weapon(){{
				mirror = true;
				alternate = true;
				x = 14f;
				y = 12f;
				reload = 12f;
				rotate = true;
				shootSound = Sounds.shoot;
				bullet = new BasicBulletType(3.6f, 14f){{
					width = 5f;
					height = 8f;
					lifetime = 42f;
					frontColor = Color.valueOf("d9f2ff");
					backColor = Color.valueOf("6cc0ff");
				}};
			}});
		}};
		
		all.addAll(patrolBoat, corvette, torpedoBoat, mortarSkiff, missileFrigate, abyss, longbow, aircraftCarrier, interceptor);
		
		//The first boat comes with the yard.
		patrolBoat.alwaysUnlocked = true;
	}
	
	/** A homing missile. */
	static MissileBulletType missile(float speed, float damage, float homing, float splash, float radius, int life){
		return new MissileBulletType(speed, damage){{
			homingPower = homing;
			splashDamage = splash;
			splashDamageRadius = radius;
			lifetime = life;
			width = 7f;
			height = 9f;
			trailColor = Color.valueOf("bfe6ff");
			trailLength = 8;
			trailWidth = 1.6f;
			hitEffect = despawnEffect = Fx.blastExplosion;
		}};
	}
	
	/** A slow torpedo that only hits what is on the water. */
	static BasicBulletType torpedo(float speed, float splash, float radius, int life){
		return new BasicBulletType(speed, 0f){{
			sprite = "missile";
			width = 7f;
			height = 15f;
			splashDamage = splash;
			splashDamageRadius = radius;
			lifetime = life;
			collidesAir = false;
			trailColor = Color.valueOf("9fd8ff");
			trailLength = 18;
			trailWidth = 2f;
			frontColor = Color.white;
			backColor = Color.valueOf("5ba9e6");
			status = StatusEffects.wet;
			hitEffect = despawnEffect = Fx.blastExplosion;
		}};
	}
	
	/** An artillery shell that only hits what is on the water. */
	static ArtilleryBulletType shell(float speed, float damage, float splash, float radius, int life){
		return new ArtilleryBulletType(speed, damage){{
			lifetime = life;
			splashDamage = splash;
			splashDamageRadius = radius;
			collidesAir = false;
			hitEffect = Fx.blastExplosion;
			knockback = 0.8f;
			width = height = 11f;
			trailColor = backColor = Color.valueOf("cfe8ff");
		}};
	}
}
