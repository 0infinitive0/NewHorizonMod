package newhorizon.expand.units.nuke;

import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Time;
import mindustry.ai.types.CommandAI;
import mindustry.entities.Units;
import mindustry.entities.units.AIController;
import mindustry.gen.Building;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;

/**
 * Flies an active nuke at its target and detonates it. The target is a point or a unit the player gave (through the launcher, or a
 * click with the strike command); with none it heads for the nearest enemy core, or straight ahead. The nuke explodes when it
 * reaches the target or when it has passed it. Only a nuke with no target of its own also explodes as soon as it comes close to anything
 * of the enemy's: one that was given a target flies to it, past everything else.
 *
 * <p>An active nuke turns very slowly: it cannot swing round for a second try, so it has to be aimed.
 */
public class NukeAI extends AIController{
	/** The fuse: how close an enemy has to be for the nuke to go off, besides its own size. */
	public float proximity = 26f;
	/** Reaching this close to the target sets it off. */
	public float arrival = 40f;

	private final Vec2 tmp = new Vec2(), aimPoint = new Vec2();
	/** The enemy beacon that has taken hold of this nuke. */
	private Building lureTarget;
	/** Whether a jammer or a cloak has made it lose its target for good: it flies on without one, and does not go off on what it meets. */
	private boolean lost, jammedNow;
	private float defenseTimer, jamTimer, jamTurn, brainTimer;
	private Vec2 targetPos;
	private Unit targetUnit;
	private float lastDst = Float.MAX_VALUE;
	/** Whether it has been getting closer to the target, and whether the target was given to it rather than guessed. */
	protected boolean closing, explicitAim;

	@Override
	public void updateMovement(){
		NukeAbility ability = abilityOf(unit);
		if(ability == null || !ability.active) return;

		//The defence blocks: a jammer takes the target away, a beacon gives it a new one.
		defenseTimer -= Time.delta;
		if(defenseTimer <= 0f){
			defenseTimer = 8f;
			jammedNow = NukeDefense.jammed(unit);
			if(!jammedNow){
				Building lure = NukeDefense.lure(unit);
				if(lure != null){
					lureTarget = lure;
					lost = false;
				}
			}
		}
		if(jammedNow) lose();
		if(lost){
			drift();
			return;
		}

		//On an AI team with nothing to fly at, it chooses a target worth the warhead, and flies at it as it would at an ordered one.
		if(brainTimer > 0f) brainTimer -= Time.delta;
		if(brainTimer <= 0f && unit.team.isAI() && !(this instanceof MiniNukeAI) && targetPos == null && targetUnit == null && lureTarget == null
			&& !(unit.controller() instanceof CommandAI)){
			brainTimer = 120f;
			Vec2 chosen = NukeBrain.pick(unit.team, ability.warhead, unit);
			if(chosen != null) commandPosition(chosen);
		}

		boolean aimed = aim(aimPoint);
		//A target hidden by an enemy cloak is gone.
		if(aimed && explicitAim && lureTarget == null && NukeDefense.cloaked(unit.team, aimPoint.x, aimPoint.y)){
			lose();
			drift();
			return;
		}

		//A nuke that was given a target flies to it, past whatever else is in the way. Only one with nothing to aim at (launched by hand, or
		//heading for the nearest core on its own) goes off on the first enemy it meets.
		if(!(aimed && explicitAim) && fused(ability)) return;

		if(aimed){
			float dst = unit.dst(aimPoint);
			//Going off as it flies past only makes sense if the target is inside the blast.
			float passed = Math.max(arrival, ability.warhead.radius * 0.9f);
			if(dst < arrival || (closing && dst < passed && dst > lastDst + 0.01f)){
				ability.detonate(unit);
				return;
			}
			//It only counts as having passed the target after it has been getting closer: one that starts out pointing away turns first.
			if(lastDst != Float.MAX_VALUE && dst < lastDst - 0.01f) closing = true;
			lastDst = dst;
			unit.movePref(tmp.set(aimPoint).sub(unit).setLength(unit.speed()));
		}else{
			//Nothing to aim at: keep flying the way it points.
			unit.movePref(tmp.trns(unit.rotation, unit.speed()));
		}
	}

	/**
	 * Where the nuke is headed, written into {@code out}. What this AI was told directly comes first; otherwise what the commanding AI
	 * holds (a command controller is only created on the commander's first update, so a target given before that never reaches this
	 * one), and last the nearest enemy core.
	 */
	protected boolean aim(Vec2 out){
		explicitAim = true;
		if(lureTarget != null){
			if(lureTarget.isValid()){
				out.set(lureTarget);
				return true;
			}
			lureTarget = null;
		}
		if(targetUnit != null && targetUnit.isValid() && targetUnit.team != unit.team){
			out.set(targetUnit);
			return true;
		}
		if(targetPos != null){
			out.set(targetPos);
			return true;
		}
		if(unit.controller() instanceof CommandAI){
			CommandAI commander = (CommandAI)unit.controller();
			if(commander.attackTarget instanceof Unit && ((Unit)commander.attackTarget).isValid() && commander.attackTarget.team() != unit.team){
				out.set(commander.attackTarget);
				return true;
			}
			if(commander.targetPos != null){
				out.set(commander.targetPos);
				return true;
			}
		}
		//Nobody gave it anything: the nearest enemy core is only a heading.
		explicitAim = false;
		Building core = unit.closestEnemyCore();
		if(core != null){
			out.set(core);
			return true;
		}
		return false;
	}

	/** Forgets every target: from here the nuke only flies straight on. */
	protected void lose(){
		lost = true;
		targetPos = null;
		targetUnit = null;
		lureTarget = null;
	}

	/** Flies on without a target. Inside a jammer's field it swings about at random. */
	protected void drift(){
		float offset = 0f;
		if(jammedNow){
			jamTimer -= Time.delta;
			if(jamTimer <= 0f){
				jamTimer = Mathf.random(12f, 40f);
				jamTurn = Mathf.range(90f);
			}
			offset = jamTurn;
		}
		unit.movePref(tmp.trns(unit.rotation + offset, unit.speed()));
	}

	/** Goes off if an enemy unit or structure is right ahead. */
	protected boolean fused(NukeAbility ability){
		float range = proximity + unit.hitSize / 2f + unit.speed();
		Teamc hit = Units.closestTarget(unit.team, unit.x, unit.y, range, u -> u.checkTarget(true, true), b -> true);
		if(hit != null){
			ability.detonate(unit);
			return true;
		}
		return false;
	}

	/**
	 * Where this nuke has been told to go, written into {@code out}: a point, a unit or a beacon. False if it has only a heading (nothing was
	 * given to it, or it has lost its target to a jammer or a cloak).
	 */
	public boolean knownTarget(Vec2 out){
		if(lost) return false;
		if(lureTarget != null && lureTarget.isValid()){
			out.set(lureTarget);
			return true;
		}
		if(targetUnit != null && targetUnit.isValid() && targetUnit.team != unit.team){
			out.set(targetUnit);
			return true;
		}
		if(targetPos != null){
			out.set(targetPos);
			return true;
		}
		if(unit.controller() instanceof CommandAI){
			CommandAI commander = (CommandAI)unit.controller();
			if(commander.attackTarget instanceof Unit && ((Unit)commander.attackTarget).isValid() && commander.attackTarget.team() != unit.team){
				out.set(commander.attackTarget);
				return true;
			}
			if(commander.targetPos != null){
				out.set(commander.targetPos);
				return true;
			}
		}
		return false;
	}

	/** Where the nuke is going, whichever controller it has (flying itself, or commanded on the Strike command). */
	public static boolean targetOf(Unit unit, Vec2 out){
		mindustry.entities.units.UnitController controller = unit.controller();
		if(controller instanceof NukeAI) return ((NukeAI)controller).knownTarget(out);
		if(controller instanceof CommandAI){
			CommandAI commander = (CommandAI)controller;
			try{
				AIController inner = arc.util.Reflect.get(CommandAI.class, commander, "commandController");
				if(inner instanceof NukeAI) return ((NukeAI)inner).knownTarget(out);
			}catch(Throwable ignored){
			}
			if(commander.attackTarget instanceof Unit && ((Unit)commander.attackTarget).isValid()){
				out.set(commander.attackTarget);
				return true;
			}
			if(commander.targetPos != null){
				out.set(commander.targetPos);
				return true;
			}
		}
		return false;
	}

	public static NukeAbility abilityOf(Unit unit){
		for(mindustry.entities.abilities.Ability a : unit.abilities){
			if(a instanceof NukeAbility) return (NukeAbility)a;
		}
		return null;
	}

	/** A click on the ground: fly there. */
	@Override
	public void commandPosition(Vec2 pos){
		targetPos = pos == null ? null : pos.cpy();
		targetUnit = null;
		lastDst = Float.MAX_VALUE;
		closing = false;
	}

	/** A click on an enemy unit: fly at that one. */
	@Override
	public void commandTarget(Teamc target){
		if(target instanceof Unit){
			targetUnit = (Unit)target;
			targetPos = null;
			lastDst = Float.MAX_VALUE;
			closing = false;
		}
	}

	/** No weapons, nothing to aim. */
	@Override
	public void updateTargeting(){
	}
}
