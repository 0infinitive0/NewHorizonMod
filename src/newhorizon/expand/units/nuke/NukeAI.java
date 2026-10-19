package newhorizon.expand.units.nuke;

import arc.math.geom.Vec2;
import mindustry.ai.types.CommandAI;
import mindustry.entities.Units;
import mindustry.entities.units.AIController;
import mindustry.gen.Building;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;

/**
 * Flies an active nuke at its target and detonates it. The target is a point or a unit the player gave (through the launcher, or a
 * click with the strike command); with none it heads for the nearest enemy core, or straight ahead. The nuke explodes when it
 * reaches the target, when it has passed it, or as soon as it comes close to anything of the enemy's on the way.
 *
 * <p>An active nuke turns very slowly: it cannot swing round for a second try, so it has to be aimed.
 */
public class NukeAI extends AIController{
	/** The fuse: how close an enemy has to be for the nuke to go off, besides its own size. */
	public float proximity = 26f;
	/** Reaching this close to the target sets it off, and so does moving away from the target while this close. */
	public float arrival = 40f, passedRange = 170f;

	private final Vec2 tmp = new Vec2(), aimPoint = new Vec2();
	private Vec2 targetPos;
	private Unit targetUnit;
	private float lastDst = Float.MAX_VALUE;

	@Override
	public void updateMovement(){
		NukeAbility ability = abilityOf(unit);
		if(ability == null || !ability.active) return;

		if(fused(ability)) return;

		if(aim(aimPoint)){
			float dst = unit.dst(aimPoint);
			if(dst < arrival || (dst < passedRange && dst > lastDst + 0.01f)){
				ability.detonate(unit);
				return;
			}
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
		Building core = unit.closestEnemyCore();
		if(core != null){
			out.set(core);
			return true;
		}
		return false;
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
	}

	/** A click on an enemy unit: fly at that one. */
	@Override
	public void commandTarget(Teamc target){
		if(target instanceof Unit){
			targetUnit = (Unit)target;
			targetPos = null;
			lastDst = Float.MAX_VALUE;
		}
	}

	/** No weapons, nothing to aim. */
	@Override
	public void updateTargeting(){
	}
}
