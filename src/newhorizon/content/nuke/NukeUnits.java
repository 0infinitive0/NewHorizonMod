package newhorizon.content.nuke;

import arc.graphics.Color;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.ai.UnitCommand;
import mindustry.entities.units.AIController;
import mindustry.gen.UnitEntity;
import mindustry.type.UnitType;
import newhorizon.expand.units.nuke.DetonateAI;
import newhorizon.expand.units.nuke.NukeAI;
import newhorizon.expand.units.nuke.NukeAbility;
import newhorizon.expand.units.nuke.Warhead;
import newhorizon.expand.units.unitType.NHUnitType;

/**
 * The nukes: four kinds, each in two forms. The inactive form is what the Nuke Builder makes: it turns and flies like any light unit
 * and makes a medium blast if it is destroyed. The Nuke Launcher turns it into the active form just before it launches it: very fast,
 * but it turns slowly and flies only the way it points, and it makes a huge blast of its kind. Both can be flown by a player.
 */
public class NukeUnits{
	public static NHUnitType
		explosive, explosiveActive,
		incendiary, incendiaryActive,
		radiation, radiationActive,
		emp, empActive;

	/** The Strike command (the default of an active nuke) flies it at the target; Detonate sets it off at once. */
	public static UnitCommand strikeCommand, detonateCommand;

	/** Inactive nukes, in the order the builder lists them. */
	public static final Seq<NHUnitType> inactive = new Seq<>();
	private static final ObjectMap<UnitType, UnitType> counterpart = new ObjectMap<>();
	private static final ObjectMap<UnitType, Warhead> warheads = new ObjectMap<>();

	public static void load(){
		inactive.clear();
		counterpart.clear();
		warheads.clear();

		strikeCommand = new UnitCommand("nh-nuke-strike", "commandAttack", u -> new NukeAI());
		strikeCommand.switchToMove = false;
		detonateCommand = new UnitCommand("nh-nuke-detonate", "warning", u -> new DetonateAI());

		explosive = nuke(Warhead.explosive, false);
		explosiveActive = nuke(Warhead.explosive, true);
		incendiary = nuke(Warhead.incendiary, false);
		incendiaryActive = nuke(Warhead.incendiary, true);
		radiation = nuke(Warhead.radiation, false);
		radiationActive = nuke(Warhead.radiation, true);
		emp = nuke(Warhead.emp, false);
		empActive = nuke(Warhead.emp, true);

		pair(explosive, explosiveActive);
		pair(incendiary, incendiaryActive);
		pair(radiation, radiationActive);
		pair(emp, empActive);
	}

	private static void pair(NHUnitType off, NHUnitType on){
		counterpart.put(off, on);
		counterpart.put(on, off);
		inactive.add(off);
	}

	private static NHUnitType nuke(Warhead warhead, boolean active){
		NHUnitType type = new NHUnitType(active ? "nuke-" + warhead.name + "-active" : "nuke-" + warhead.name){{
			constructor = UnitEntity::create;
			outlineColor = Color.valueOf("1b1b22");
			flying = true;
			lowAltitude = true;
			hitSize = 14f;
			engineColor = warhead.color;
			//A nuke is a warhead, not a wreck: it goes off the moment it dies.
			createWreck = false;
			createScorch = false;
			//No weapons: its only attack is its own death.
			targetAir = false;
			targetGround = false;
			isEnemy = true;

			if(active){
				health = 90f;
				speed = 7f;
				accel = 0.12f;
				drag = 0.012f;
				rotateSpeed = 1.1f;
				omniMovement = false;
				engineOffset = 11f;
				engineSize = 4.4f;
				hidden = true;
				aiController = NukeAI::new;
				commands = Seq.with(strikeCommand, detonateCommand, UnitCommand.moveCommand);
				defaultCommand = strikeCommand;
			}else{
				health = 230f;
				speed = 1.9f;
				accel = 0.08f;
				drag = 0.05f;
				rotateSpeed = 10f;
				omniMovement = true;
				engineOffset = 9f;
				engineSize = 2.6f;
				aiController = AIController::new;
				commands = Seq.with(UnitCommand.moveCommand);
				defaultCommand = UnitCommand.moveCommand;
			}

			abilities.add(new NukeAbility(warhead, active));
		}};
		warheads.put(type, warhead);
		return type;
	}

	public static boolean isNuke(UnitType type){
		return warheads.containsKey(type);
	}

	public static boolean isInactive(UnitType type){
		return inactive.contains(t -> t == type);
	}

	public static boolean isActive(UnitType type){
		return isNuke(type) && !isInactive(type);
	}

	/** The other form of the same nuke, or null for anything that is not one. */
	public static UnitType counterpart(UnitType type){
		return counterpart.get(type);
	}

	public static Warhead warheadOf(UnitType type){
		return warheads.get(type);
	}
}
