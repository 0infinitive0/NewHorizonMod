package newhorizon.content.nuke;

import arc.graphics.Color;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.ai.UnitCommand;
import mindustry.entities.units.AIController;
import mindustry.gen.PayloadUnit;
import mindustry.gen.Payloadc;
import mindustry.entities.units.UnitController;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.ai.types.CommandAI;
import mindustry.gen.UnitWaterMove;
import mindustry.ai.UnitStance;
import newhorizon.expand.units.nuke.ArmAI;
import newhorizon.expand.units.nuke.CarrierAI;
import newhorizon.expand.units.nuke.NukeCarrierAbility;
import newhorizon.expand.units.nuke.DetonateAI;
import newhorizon.expand.units.nuke.MiniNukeAI;
import newhorizon.expand.units.nuke.NukeAI;
import newhorizon.expand.units.nuke.NukeAbility;
import newhorizon.expand.units.nuke.WallStance;
import newhorizon.expand.units.nuke.Warhead;
import newhorizon.expand.units.unitType.NHUnitType;
import newhorizon.expand.units.unitType.NukeUnitType;

import static mindustry.Vars.tilesize;

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
		emp, empActive,
		psychic, psychicActive,
		gravity, gravityActive,
		spatial, spatialActive,
		shockwave, shockwaveActive,
		cluster, clusterActive,
		wall, wallActive,
		temporal, temporalActive,
		infantry, infantryActive,
		overload, overloadActive,
		laser, laserActive,
		swarm, swarmActive,
		storm, stormActive,
		decoy, decoyActive,
		lightning, lightningActive,
		siphon, siphonActive,
		quake, quakeActive,
		scatter, scatterActive,
		magnet, magnetActive,
		hijack, hijackActive,
		scramble, scrambleActive,
		sabotage, sabotageActive,
		breach, breachActive,
		devolve, devolveActive;
	/** What a decoy breaks into. Never built or launched, and not a part of any unit cap. */
	public static NHUnitType mini;
	/** The ship that carries nukes and launches them. */
	public static NHUnitType carrier;

	/** The Strike command (the default of an active nuke) flies it at the target; Detonate sets it off at once. */
	public static UnitCommand strikeCommand, detonateCommand, armCommand, carrierTargetCommand, carrierClearCommand, carrierLaunchCommand;
	/** The toggle among a carrier's buttons that keeps its nuke armed. */
	public static UnitStance carrierArmedStance;

	/** One stance per wall a wall nuke can be set to. */
	public static final Seq<WallStance> wallStances = new Seq<>();

	/** Inactive nukes, in the order the builder lists them. */
	public static final Seq<NHUnitType> inactive = new Seq<>();
	private static final ObjectMap<UnitType, UnitType> counterpart = new ObjectMap<>();
	private static final ObjectMap<UnitType, Warhead> warheads = new ObjectMap<>();

	public static void load(){
		inactive.clear();
		counterpart.clear();
		warheads.clear();
		wallStances.clear();
		for(Block block : Vars.content.blocks()){
			if(NukeAbility.choosable(block)) wallStances.add(new WallStance(block));
		}
		//Only one wall can be picked at a time.
		for(WallStance a : wallStances) for(WallStance b : wallStances) if(a != b) a.incompatibleStances.add(b);

		strikeCommand = new UnitCommand("nh-nuke-strike", "commandAttack", u -> new NukeAI());
		strikeCommand.switchToMove = false;
		detonateCommand = new UnitCommand("nh-nuke-detonate", "warning", u -> new DetonateAI());
		//An inactive nuke can arm itself: it turns active where it is, and the player then sends it off with the Strike command.
		armCommand = new UnitCommand("nh-nuke-arm", "power", u -> new ArmAI());
		armCommand.switchToMove = false;
		//A carrier ship works like a launcher: its Launch command sends the nuke aboard at each target (arming it first if need be), and the
		//Armed button keeps it armed ahead of time.
		carrierTargetCommand = new UnitCommand("nh-carrier-target", "commandAttack", u -> new CarrierAI(CarrierAI.Mode.target));
		carrierTargetCommand.switchToMove = false;
		carrierTargetCommand.resetTarget = false;
		carrierTargetCommand.drawTarget = true;
		carrierClearCommand = new UnitCommand("nh-carrier-clear", "cancel", u -> new CarrierAI(CarrierAI.Mode.clear));
		carrierClearCommand.switchToMove = false;
		carrierLaunchCommand = new UnitCommand("nh-carrier-launch", "up", u -> new CarrierAI(CarrierAI.Mode.launch));
		carrierArmedStance = new newhorizon.expand.units.nuke.CarrierArmedStance();
		carrierLaunchCommand.switchToMove = false;

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

		psychic = nuke(Warhead.psychic, false);
		psychicActive = nuke(Warhead.psychic, true);
		gravity = nuke(Warhead.gravity, false);
		gravityActive = nuke(Warhead.gravity, true);
		spatial = nuke(Warhead.spatial, false);
		spatialActive = nuke(Warhead.spatial, true);
		shockwave = nuke(Warhead.shockwave, false);
		shockwaveActive = nuke(Warhead.shockwave, true);
		cluster = nuke(Warhead.cluster, false);
		clusterActive = nuke(Warhead.cluster, true);
		wall = nuke(Warhead.wall, false);
		wallActive = nuke(Warhead.wall, true);
		temporal = nuke(Warhead.temporal, false);
		temporalActive = nuke(Warhead.temporal, true);
		infantry = nuke(Warhead.infantry, false);
		infantryActive = nuke(Warhead.infantry, true);

		pair(psychic, psychicActive);
		pair(gravity, gravityActive);
		pair(spatial, spatialActive);
		pair(shockwave, shockwaveActive);
		pair(cluster, clusterActive);
		pair(wall, wallActive);
		pair(temporal, temporalActive);
		overload = nuke(Warhead.overload, false);
		overloadActive = nuke(Warhead.overload, true);
		laser = nuke(Warhead.laser, false);
		laserActive = nuke(Warhead.laser, true);
		swarm = nuke(Warhead.swarm, false);
		swarmActive = nuke(Warhead.swarm, true);
		pair(infantry, infantryActive);
		pair(overload, overloadActive);
		storm = nuke(Warhead.storm, false);
		stormActive = nuke(Warhead.storm, true);
		pair(laser, laserActive);
		pair(swarm, swarmActive);
		pair(storm, stormActive);

		decoy = nuke(Warhead.decoy, false);
		decoyActive = nuke(Warhead.decoy, true);
		pair(decoy, decoyActive);
		mini = nuke(Warhead.mini, true);

		lightning = nuke(Warhead.lightning, false);
		lightningActive = nuke(Warhead.lightning, true);
		pair(lightning, lightningActive);
		siphon = nuke(Warhead.siphon, false);
		siphonActive = nuke(Warhead.siphon, true);
		pair(siphon, siphonActive);
		quake = nuke(Warhead.quake, false);
		quakeActive = nuke(Warhead.quake, true);
		pair(quake, quakeActive);
		scatter = nuke(Warhead.scatter, false);
		scatterActive = nuke(Warhead.scatter, true);
		pair(scatter, scatterActive);
		magnet = nuke(Warhead.magnet, false);
		magnetActive = nuke(Warhead.magnet, true);
		pair(magnet, magnetActive);
		hijack = nuke(Warhead.hijack, false);
		hijackActive = nuke(Warhead.hijack, true);
		pair(hijack, hijackActive);
		scramble = nuke(Warhead.scramble, false);
		scrambleActive = nuke(Warhead.scramble, true);
		pair(scramble, scrambleActive);
		sabotage = nuke(Warhead.sabotage, false);
		sabotageActive = nuke(Warhead.sabotage, true);
		pair(sabotage, sabotageActive);
		breach = nuke(Warhead.breach, false);
		breachActive = nuke(Warhead.breach, true);
		pair(breach, breachActive);
		carrier = new NukeUnitType("nuke-carrier"){{
			constructor = UnitWaterMove::create;
			naval = true;
			outlineColor = Color.valueOf("1b1b22");
			speed = 0.9f;
			accel = 0.08f;
			drag = 0.14f;
			rotateSpeed = 1.8f;
			hitSize = 34f;
			health = 2600f;
			armor = 8f;
			trailLength = 22;
			waveTrailX = 9f;
			waveTrailY = -22f;
			trailScl = 1.8f;
			commands = Seq.with(UnitCommand.moveCommand, carrierTargetCommand, carrierClearCommand, carrierLaunchCommand);
			stances = Seq.with(carrierArmedStance);
			defaultCommand = UnitCommand.moveCommand;
			abilities.add(new NukeCarrierAbility());
		}};

		devolve = nuke(Warhead.devolve, false);
		devolveActive = nuke(Warhead.devolve, true);
		pair(devolve, devolveActive);
	}

	private static void pair(NHUnitType off, NHUnitType on){
		counterpart.put(off, on);
		counterpart.put(on, off);
		inactive.add(off);
	}

	private static NHUnitType nuke(Warhead warhead, boolean active){
		NHUnitType type = new NukeUnitType(active ? "nuke-" + warhead.name + "-active" : "nuke-" + warhead.name){{
			//The infantry nuke carries units: it is a payload unit, so units can be loaded into it while it is inactive.
			constructor = warhead == Warhead.infantry ? PayloadUnit::create : UnitEntity::create;
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
				//It can also fly into a launcher (or any payload block that takes it), and arm itself.
				commands = Seq.with(UnitCommand.moveCommand, UnitCommand.enterPayloadCommand, armCommand);
				defaultCommand = UnitCommand.moveCommand;
			}

			if(warhead == Warhead.infantry){
				payloadCapacity = tilesize * tilesize * 60f;
				//Passengers are loaded before it is armed and cannot be changed in flight.
				if(!active){
					commands = Seq.with(UnitCommand.moveCommand, UnitCommand.enterPayloadCommand, armCommand, UnitCommand.loadUnitsCommand, UnitCommand.unloadPayloadCommand);
				}
			}

			//The wall is chosen with the unit's stance buttons, until the nuke is armed.
			if(warhead == Warhead.wall && !active){
				stances = new Seq<>(wallStances);
			}

			//A decoy is bait: far tougher than any other nuke, in both forms.
			if(warhead == Warhead.decoy){
				health = 1400f;
				armor = 15f;
				hitSize = 16f;
			}

			//A mini nuke: small, quick and short-lived, it finds its own target and nobody commands it.
			if(warhead == Warhead.mini){
				health = 30f;
				speed = 5f;
				rotateSpeed = 4f;
				hitSize = 7f;
				engineOffset = 5f;
				engineSize = 2.2f;
				playerControllable = false;
				useUnitCap = false;
				allowedInPayloads = false;
				aiController = MiniNukeAI::new;
				commands = new Seq<>();
				defaultCommand = null;
			}

			NukeAbility ability = new NukeAbility(warhead, active);
			if(warhead == Warhead.mini) ability.fuel = 60f * 5f;
			abilities.add(ability);
		}};
		warheads.put(type, warhead);
		return type;
	}

	/**
	 * Arms an inactive nuke where it is: the same unit becomes its active form (so a pilot, or whoever has it selected, keeps it). What it
	 * was set up with (a wall type, passengers) stays. Server side. Returns false for anything that is not an inactive nuke.
	 */
	public static boolean arm(Unit unit){
		if(!isInactive(unit.type) || unit.dead()) return false;

		UnitType active = counterpart(unit.type);
		NukeAbility old = NukeAI.abilityOf(unit);
		float data = old == null ? 0f : old.data;

		//Changing the type may hand the unit to a new AI: whoever had it (a player, a commander) keeps it.
		UnitController keep = unit.controller();
		unit.setType(active);
		if(unit.controller() != keep) unit.controller(keep);
		//The ability list is only rebuilt when its length changes, and both forms have one ability: swap it by hand.
		for(int i = 0; i < unit.abilities.length && i < active.abilities.size; i++) unit.abilities[i] = active.abilities.get(i).copy();
		NukeAbility now = NukeAI.abilityOf(unit);
		if(now != null){
			now.data = data;
			//A pilot who armed it by firing must let go of the button before the next press detonates it.
			now.latch = unit.isPlayer();
		}
		unit.health = Math.min(unit.health, unit.maxHealth);

		if(unit.controller() instanceof CommandAI){
			((CommandAI)unit.controller()).command(strikeCommand);
		}else if(!unit.isPlayer()){
			unit.controller(active.createController(unit));
		}
		NukeFx.armed.at(unit.x, unit.y);
		return true;
	}

	/**
	 * The way a launcher sends a nuke off: a team the player commands gets the unit as a commandable one on the Strike command, everything
	 * else gets the nuke's own flight AI; the target, if any, is given to it.
	 */
	public static void fly(Unit nuke, arc.math.geom.Vec2 aim){
		AIController controller;
		if(!nuke.team.isAI() && nuke.type.commands.contains(strikeCommand)){
			CommandAI command = new CommandAI();
			nuke.controller(command);
			command.command(strikeCommand);
			controller = command;
		}else{
			UnitController current = nuke.controller();
			if(!(current instanceof AIController)){
				current = nuke.type.createController(nuke);
				nuke.controller(current);
			}
			controller = (AIController)current;
		}
		if(aim != null) controller.commandPosition(aim);
	}

	/**
	 * Disarms an active nuke where it is: the same unit becomes its inactive form. Server side. Returns false for anything that is not an
	 * active nuke with an inactive form.
	 */
	public static boolean disarm(Unit unit){
		if(!isActive(unit.type) || unit.dead()) return false;
		UnitType inactive = counterpart(unit.type);
		if(inactive == null) return false;

		NukeAbility old = NukeAI.abilityOf(unit);
		float data = old == null ? 0f : old.data;
		UnitController keep = unit.controller();
		unit.setType(inactive);
		if(unit.controller() != keep) unit.controller(keep);
		for(int i = 0; i < unit.abilities.length && i < inactive.abilities.size; i++) unit.abilities[i] = inactive.abilities.get(i).copy();
		NukeAbility now = NukeAI.abilityOf(unit);
		if(now != null) now.data = data;
		unit.health = Math.min(unit.maxHealth, inactive.health);
		return true;
	}

	/**
	 * What an interceptor does to a nuke it hits: an active nuke is switched off, becoming its inactive form, and goes over to the
	 * interceptor's team (a mini nuke has no inactive form and is simply neutralised). Server side. Returns whether it worked.
	 */
	public static boolean capture(Unit unit, mindustry.game.Team team){
		if(!isActive(unit.type) || unit.dead() || !unit.isAdded()) return false;
		if(counterpart(unit.type) == null){
			NukeFx.armed.at(unit.x, unit.y);
			unit.remove();
			return true;
		}
		if(!disarm(unit)) return false;
		unit.unapply(NukeStatus.highAltitude);
		unit.team(team);
		unit.controller(unit.type.createController(unit));
		unit.vel.scl(0.3f);
		NukeFx.armed.at(unit.x, unit.y);
		return true;
	}

	/** Carries what an inactive nuke has been set up with over to the active unit that replaces it, or the other way round: a wall type, passengers. */
	public static void transfer(Unit from, Unit to){
		NukeAbility a = NukeAI.abilityOf(from), b = NukeAI.abilityOf(to);
		if(a != null && b != null) b.data = a.data;
		if(from instanceof Payloadc && to instanceof Payloadc) ((Payloadc)to).payloads(((Payloadc)from).payloads());
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
