package newhorizon.expand.units.nuke;

import arc.math.Angles;
import arc.math.geom.Vec2;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.units.AIController;
import mindustry.world.blocks.payloads.UnitPayload;
import newhorizon.content.nuke.NukeFx;
import newhorizon.content.nuke.NukeUnits;
import newhorizon.expand.block.nuke.NukeLauncher;

/**
 * The AI of an inactive nuke on an AI team. About once a second it looks for a target worth a nuke ({@link NukeBrain}); with none it waits.
 * With one, it flies into a free launcher of its team within reach, if there is one (the launcher then fires it by itself), and otherwise
 * turns to face the target and arms itself, after it has been around for two seconds. A nuke on a team that a player commands never
 * gets this AI.
 */
public class NukeArmAI extends AIController{
	/** Seconds in ticks it waits after appearing, and how closely it has to face the target to arm. */
	public float settle = 120f, tolerance = 10f;

	private float age, decideTimer;
	private final Vec2 target = new Vec2();
	private boolean hasTarget;
	private NukeLauncher.NukeLauncherBuild dock;

	@Override
	public void updateMovement(){
		if(Vars.net.client() || unit == null || unit.dead() || !NukeUnits.isInactive(unit.type)) return;
		age += Time.delta;
		decideTimer -= Time.delta;
		if(decideTimer <= 0f){
			decideTimer = 60f;
			decide();
		}

		if(dock != null){
			if(!dock.isValid() || !dock.acceptUnitPayload(unit)){
				dock = null;
				return;
			}
			float reach = dock.block.size * 8f / 2f;
			if(unit.within(dock, reach)){
				//In: the launcher takes it, as the Enter Payload command does.
				UnitPayload payload = new UnitPayload(unit);
				dock.handlePayload(dock, payload);
				NukeFx.armed.at(unit.x, unit.y);
				unit.remove();
				return;
			}
			moveTo(dock, reach * 0.5f);
			unit.lookAt(dock);
			return;
		}

		if(!hasTarget) return;
		float want = unit.angleTo(target.x, target.y);
		unit.lookAt(want);
		if(age >= settle && Math.abs(Angles.angleDist(unit.rotation, want)) <= tolerance) NukeUnits.arm(unit);
	}

	private void decide(){
		Warhead warhead = NukeUnits.warheadOf(unit.type);
		Vec2 found = warhead == null ? null : NukeBrain.pick(unit.team, warhead, unit);
		hasTarget = found != null;
		if(!hasTarget){
			dock = null;
			return;
		}
		target.set(found);
		dock = NukeBrain.freeLauncher(unit);
	}

	/** No weapons, nothing to aim. */
	@Override
	public void updateTargeting(){
	}
}
