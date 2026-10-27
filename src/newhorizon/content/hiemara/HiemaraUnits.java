package newhorizon.content.hiemara;

import arc.struct.Seq;
import mindustry.gen.UnitEntity;
import mindustry.gen.UnitWaterMove;
import mindustry.type.UnitType;
import newhorizon.expand.hiemara.CargoCap;
import newhorizon.expand.units.ai.CargoHaulAI;

/**
 * The cargo units: two lines of three tiers, one in the air and one on the water. They carry items from a port to the
 * depots and have no weapons at all. Each tier carries more and is faster.
 */
public class HiemaraUnits{
	public static UnitType cargoDrone, cargoHauler, cargoFreighter;
	public static UnitType cargoSkiff, cargoBarge, cargoCarrier;
	
	/** Air line, then naval line, lowest tier first. */
	public static final Seq<UnitType> airLine = new Seq<>(), navalLine = new Seq<>();
	
	public static void load(){
		cargoDrone = air("cargo-drone", 90f, 8f, 2.4f, 20);
		cargoHauler = air("cargo-hauler", 170f, 11f, 3f, 50);
		cargoFreighter = air("cargo-freighter", 320f, 15f, 3.8f, 120);
		
		cargoSkiff = naval("cargo-skiff", 220f, 10f, 0.9f, 60);
		cargoBarge = naval("cargo-barge", 420f, 14f, 1.1f, 150);
		cargoCarrier = naval("cargo-carrier", 800f, 20f, 1.4f, 350);
		
		airLine.addAll(cargoDrone, cargoHauler, cargoFreighter);
		navalLine.addAll(cargoSkiff, cargoBarge, cargoCarrier);
		
		//The first tier of each line is there from the start.
		cargoDrone.alwaysUnlocked = true;
		cargoSkiff.alwaysUnlocked = true;
	}
	
	private static UnitType air(String name, float health, float hitSize, float speed, int capacity){
		return cargo(new UnitType(name){{
			constructor = UnitEntity::create;
			flying = true;
			accel = 0.1f;
			drag = 0.05f;
			rotateSpeed = 12f;
			engineOffset = hitSize / 2f + 1f;
			engineSize = hitSize / 3.5f;
			lowAltitude = true;
		}}, health, hitSize, speed, capacity);
	}
	
	private static UnitType naval(String name, float health, float hitSize, float speed, int capacity){
		return cargo(new UnitType(name){{
			constructor = UnitWaterMove::create;
			accel = 0.4f;
			drag = 0.13f;
			rotateSpeed = 3.3f;
			faceTarget = false;
		}}, health, hitSize, speed, capacity);
	}
	
	private static UnitType cargo(UnitType type, float health, float hitSize, float speed, int capacity){
		type.health = health;
		type.hitSize = hitSize;
		type.speed = speed;
		type.itemCapacity = capacity;
		
		type.aiController = CargoHaulAI::new;
		type.controller = u -> new CargoHaulAI();
		
		//No weapons, and nothing that could be ordered, shot at or carried around.
		type.targetAir = false;
		type.targetGround = false;
		type.isEnemy = false;
		type.playerControllable = false;
		type.logicControllable = false;
		type.allowedInPayloads = false;
		type.useUnitCap = false;
		type.createWreck = false;
		type.drawMinimap = false;
		
		CargoCap.cargoTypes.add(type);
		return type;
	}
}
