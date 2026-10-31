package newhorizon.content.hiemara;

import arc.func.Prov;
import arc.struct.Seq;
import mindustry.entities.units.AIController;
import mindustry.gen.UnitEntity;
import mindustry.gen.UnitWaterMove;
import mindustry.type.UnitType;
import newhorizon.expand.hiemara.CargoCap;
import newhorizon.expand.units.ai.CargoHaulAI;
import newhorizon.expand.units.ai.TankerHaulAI;
import newhorizon.expand.units.hiemara.CargoUnitType;
import newhorizon.expand.units.hiemara.TankerUnitType;
import newhorizon.expand.units.unitEntity.MultiCargoEntity;
import newhorizon.expand.units.unitEntity.TankerEntity;

/**
 * The cargo units. They have no weapons at all, and each tier is faster and carries more.
 * <ul>
 * <li>Air: three tiers, one kind of item at a time.</li>
 * <li>Naval: six tiers. The first three carry one kind of item, the three big ships carry several at once.</li>
 * <li>Tankers: three tiers of ships that carry one liquid.</li>
 * </ul>
 * All of them count towards the cargo unit cap.
 */
public class HiemaraUnits{
	public static UnitType cargoDrone, cargoHauler, cargoFreighter;
	public static UnitType cargoSkiff, cargoBarge, cargoCarrier, cargoGalleon, cargoLeviathan, cargoTitan;
	public static UnitType tankerCutter, tankerBulk, tankerSuper;
	
	/** Each line, lowest tier first. */
	public static final Seq<UnitType> airLine = new Seq<>(), navalLine = new Seq<>(), tankerLine = new Seq<>();
	
	public static void load(){
		cargoDrone = air("cargo-drone", 90f, 8f, 2.4f, 20, 1);
		cargoHauler = air("cargo-hauler", 170f, 11f, 3f, 50, 2);
		cargoFreighter = air("cargo-freighter", 320f, 15f, 3.8f, 120, 3);
		
		cargoSkiff = naval("cargo-skiff", 220f, 10f, 0.9f, 60, 1);
		cargoBarge = naval("cargo-barge", 420f, 14f, 1.1f, 150, 2);
		cargoCarrier = naval("cargo-carrier", 800f, 20f, 1.4f, 350, 3);
		//The big ships: capacity is for each kind of item they carry.
		cargoGalleon = ship("cargo-galleon", 1800f, 28f, 1.5f, 300, 3, 4, 2.6f);
		cargoLeviathan = ship("cargo-leviathan", 4200f, 40f, 1.6f, 700, 5, 5, 2.2f);
		cargoTitan = ship("cargo-titan", 9500f, 56f, 1.8f, 1500, 9, 6, 1.8f);
		
		tankerCutter = tanker("tanker-cutter", 260f, 11f, 0.9f, 300f, 1);
		tankerBulk = tanker("tanker-bulk", 560f, 17f, 1.1f, 900f, 2);
		tankerSuper = tanker("tanker-super", 1400f, 26f, 1.4f, 2500f, 3);
		
		airLine.addAll(cargoDrone, cargoHauler, cargoFreighter);
		navalLine.addAll(cargoSkiff, cargoBarge, cargoCarrier, cargoGalleon, cargoLeviathan, cargoTitan);
		tankerLine.addAll(tankerCutter, tankerBulk, tankerSuper);
		
		//The first tier of each line is there from the start.
		cargoDrone.alwaysUnlocked = true;
		cargoSkiff.alwaysUnlocked = true;
		tankerCutter.alwaysUnlocked = true;
	}
	
	private static UnitType air(String name, float health, float hitSize, float speed, int capacity, int tier){
		CargoUnitType type = new CargoUnitType(name){{
			constructor = UnitEntity::create;
			flying = true;
			accel = 0.1f;
			drag = 0.05f;
			rotateSpeed = 12f;
			lowAltitude = true;
		}};
		type.cargoTier = tier;
		return cargo(type, health, hitSize, speed, capacity, CargoHaulAI::new);
	}
	
	private static UnitType naval(String name, float health, float hitSize, float speed, int capacity, int tier){
		CargoUnitType type = new CargoUnitType(name){{
			constructor = UnitWaterMove::create;
			accel = 0.4f;
			drag = 0.13f;
			rotateSpeed = 3.3f;
			faceTarget = false;
		}};
		type.cargoTier = tier;
		return cargo(type, health, hitSize, speed, capacity, CargoHaulAI::new);
	}
	
	private static UnitType ship(String name, float health, float hitSize, float speed, int capacityPerType, int types, int tier, float rotateSpeed){
		CargoUnitType type = new CargoUnitType(name){{
			constructor = MultiCargoEntity::new;
			accel = 0.2f;
			drag = 0.17f;
			faceTarget = false;
		}};
		type.rotateSpeed = rotateSpeed;
		type.holdTypes = types;
		type.cargoTier = tier;
		return cargo(type, health, hitSize, speed, capacityPerType, CargoHaulAI::new);
	}
	
	private static UnitType tanker(String name, float health, float hitSize, float speed, float liquidCapacity, int tier){
		TankerUnitType type = new TankerUnitType(name){{
			constructor = TankerEntity::new;
			accel = 0.3f;
			drag = 0.15f;
			rotateSpeed = 2.4f;
			faceTarget = false;
		}};
		type.liquidCapacity = liquidCapacity;
		type.cargoTier = tier;
		return cargo(type, health, hitSize, speed, 0, TankerHaulAI::new);
	}
	
	private static int tierOf(UnitType type){
		if(type instanceof CargoUnitType)return ((CargoUnitType)type).cargoTier;
		if(type instanceof TankerUnitType)return ((TankerUnitType)type).cargoTier;
		return 1;
	}
	
	private static UnitType cargo(UnitType type, float health, float hitSize, float speed, int capacity, Prov<AIController> ai){
		//the higher the tier, the further from the port the ship loads
		float range = 14f + 12f * (Math.max(1, tierOf(type)) - 1);
		if(type instanceof CargoUnitType)((CargoUnitType)type).pickupRange = range;
		if(type instanceof TankerUnitType)((TankerUnitType)type).pickupRange = range;
		
		type.health = health;
		type.hitSize = hitSize;
		type.speed = speed;
		if(capacity > 0)type.itemCapacity = capacity;
		if(type.flying){
			type.engineOffset = hitSize / 2f + 1f;
			type.engineSize = hitSize / 3.5f;
		}
		
		type.aiController = ai;
		type.controller = u -> ai.get();
		
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
