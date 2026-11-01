package newhorizon.content.hiemara;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.abilities.RepairFieldAbility;
import mindustry.entities.abilities.StatusFieldAbility;
import mindustry.entities.abilities.UnitSpawnAbility;
import mindustry.entities.bullet.LaserBoltBulletType;
import mindustry.ai.ControlPathfinder;
import mindustry.ai.Pathfinder;
import mindustry.ai.UnitCommand;
import mindustry.entities.bullet.FlakBulletType;
import mindustry.gen.PathTile;
import newhorizon.expand.units.ability.IceBreakAbility;
import mindustry.graphics.Pal;
import mindustry.type.weapons.RepairBeamWeapon;
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
	public static UnitType skywatch, icebreaker;
	public static UnitType medicSkiff, rescueFrigate, builderSkiff, builderBarge, overdriveTender;
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
				rotate = true;
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
				rotate = true;
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
				rotate = true;
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
				rotate = true;
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
				rotate = true;
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
		
		medicSkiff = new UnitType("medic-skiff"){{
			constructor = UnitWaterMove::create;
			health = 340f;
			hitSize = 11f;
			speed = 1.1f;
			accel = 0.4f;
			drag = 0.13f;
			rotateSpeed = 3.2f;
			faceTarget = false;
			armor = 1f;
			
			weapons.add(new Weapon(){{
				mirror = true;
				rotate = true;
				alternate = true;
				x = 3.5f;
				y = 3f;
				reload = 22f;
				shootSound = Sounds.shoot;
				bullet = new LaserBoltBulletType(4.6f, 9f){{
					lifetime = 34f;
					healPercent = 5f;
					collidesTeam = true;
					backColor = Pal.heal;
					frontColor = Color.white;
				}};
			}});
		}};
		
		rescueFrigate = new UnitType("rescue-frigate"){{
			constructor = UnitWaterMove::create;
			health = 1500f;
			hitSize = 22f;
			speed = 0.85f;
			accel = 0.2f;
			drag = 0.17f;
			rotateSpeed = 1.9f;
			faceTarget = false;
			armor = 6f;
			
			weapons.add(new RepairBeamWeapon("repair-beam-weapon-center"){{
				x = 9f;
				y = -4f;
				shootY = 6f;
				beamWidth = 0.8f;
				repairSpeed = 0.7f;
				targetBuildings = true;
				bullet = new mindustry.entities.bullet.BulletType(){{
					maxRange = 130f;
				}};
			}});
		}};
		
		builderSkiff = new UnitType("builder-skiff"){{
			constructor = UnitWaterMove::create;
			health = 360f;
			hitSize = 12f;
			speed = 1.0f;
			accel = 0.35f;
			drag = 0.14f;
			rotateSpeed = 3f;
			faceTarget = false;
			armor = 1f;
			buildSpeed = 0.6f;
			itemCapacity = 30;
			defaultCommand = UnitCommand.rebuildCommand;
		}};
		
		builderBarge = new UnitType("builder-barge"){{
			constructor = UnitWaterMove::create;
			health = 900f;
			hitSize = 18f;
			speed = 0.9f;
			accel = 0.25f;
			drag = 0.16f;
			rotateSpeed = 2.4f;
			faceTarget = false;
			armor = 4f;
			buildSpeed = 1.2f;
			itemCapacity = 60;
			defaultCommand = UnitCommand.rebuildCommand;
		}};
		
		overdriveTender = new UnitType("overdrive-tender"){{
			constructor = UnitWaterMove::create;
			health = 1300f;
			hitSize = 20f;
			speed = 0.8f;
			accel = 0.2f;
			drag = 0.17f;
			rotateSpeed = 1.8f;
			faceTarget = false;
			armor = 5f;
			
			abilities.add(new StatusFieldAbility(mindustry.content.StatusEffects.overdrive, 60f * 6f, 60f * 5f, 90f));
			abilities.add(new RepairFieldAbility(40f, 60f * 4f, 80f));
			
			weapons.add(new Weapon(){{
				mirror = true;
				alternate = true;
				x = 6f;
				y = 5f;
				reload = 20f;
				rotate = true;
				shootSound = Sounds.shoot;
				bullet = new BasicBulletType(3.4f, 12f){{
					width = 5f;
					height = 8f;
					lifetime = 42f;
					frontColor = Color.valueOf("ffe0a3");
					backColor = Color.valueOf("f29c4a");
				}};
			}});
		}};
		
		skywatch = new UnitType("skywatch"){{
			constructor = UnitWaterMove::create;
			health = 1100f;
			hitSize = 20f;
			speed = 0.85f;
			accel = 0.2f;
			drag = 0.17f;
			rotateSpeed = 1.9f;
			faceTarget = false;
			armor = 5f;
			targetGround = false;
			targetAir = true;
			
			weapons.add(new Weapon(){{
				mirror = true;
				alternate = true;
				rotate = true;
				x = 7f;
				y = 5f;
				reload = 16f;
				inaccuracy = 8f;
				shootSound = Sounds.shootScatter;
				bullet = new FlakBulletType(4.4f, 3){{
					lifetime = 55f;
					width = 6f;
					height = 8f;
					collidesGround = false;
					splashDamage = 45f;
					splashDamageRadius = 24f;
					hitEffect = Fx.flakExplosion;
					backColor = Color.valueOf("ffe0a3");
				}};
			}});
			
			weapons.add(new Weapon(){{
				mirror = false;
				rotate = true;
				x = 0f;
				y = -6f;
				reload = 60f;
				shoot.shots = 2;
				shoot.shotDelay = 8f;
				shootSound = Sounds.shootMissile;
				bullet = new MissileBulletType(4f, 12f){{
					lifetime = 80f;
					homingPower = 0.2f;
					homingRange = 150f;
					splashDamage = 30f;
					splashDamageRadius = 26f;
					collidesGround = false;
					width = 7f;
					height = 9f;
					trailColor = Color.valueOf("bfe6ff");
					trailLength = 8;
					trailWidth = 1.6f;
					hitEffect = despawnEffect = Fx.blastExplosion;
				}};
			}});
		}};
		
		//The ice is thawed ahead of it, so for its path every tile that is not a wall is open; the base game's ships treat ice as a wall.
		Pathfinder.PathCost flowCost = (team, tile) ->
			((PathTile.solid(tile) && (PathTile.team(tile) == team || PathTile.team(tile) == 0)) ? 7000 : 1 + PathTile.health(tile) * 5) +
			(PathTile.nearSolid(tile) ? 14 : 0) + (PathTile.liquid(tile) ? 0 : 2);
		Pathfinder.PathCost controlCost = (team, tile) ->
			(PathTile.solid(tile) && ((PathTile.team(tile) == team && !PathTile.teamPassable(tile)) || PathTile.team(tile) == 0)) ? -1 :
			(PathTile.liquid(tile) ? 1 : 3) +
			((PathTile.team(tile) != team && PathTile.team(tile) != 0) && PathTile.solid(tile) ? 70 : 0) +
			(PathTile.nearSolid(tile) ? 6 : 0);
		int flowType = Pathfinder.costTypes.size;
		Pathfinder.costTypes.add(flowCost);
		ControlPathfinder.costTypes.add(controlCost);
		
		icebreaker = new UnitType("icebreaker"){{
			constructor = UnitWaterMove::create;
			health = 1500f;
			hitSize = 18f;
			speed = 0.7f;
			accel = 0.2f;
			drag = 0.2f;
			rotateSpeed = 1.6f;
			faceTarget = false;
			armor = 8f;
			flowfieldPathType = flowType;
			pathCost = controlCost;
			
			abilities.add(new IceBreakAbility(4f * 8f));
			
			weapons.add(new Weapon(){{
				mirror = true;
				alternate = true;
				rotate = true;
				x = 6f;
				y = 5f;
				reload = 22f;
				shootSound = Sounds.shoot;
				bullet = new BasicBulletType(3.2f, 14f){{
					width = 5f;
					height = 8f;
					lifetime = 38f;
					frontColor = Color.valueOf("d9f2ff");
					backColor = Color.valueOf("6cc0ff");
				}};
			}});
		}};
		
		all.addAll(skywatch, icebreaker);
		all.addAll(medicSkiff, rescueFrigate, builderSkiff, builderBarge, overdriveTender);
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
