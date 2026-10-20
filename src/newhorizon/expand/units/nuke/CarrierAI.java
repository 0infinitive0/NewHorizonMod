package newhorizon.expand.units.nuke;

import arc.math.geom.Vec2;
import mindustry.Vars;
import mindustry.ai.types.CommandAI;
import mindustry.entities.units.AIController;
import newhorizon.content.nuke.NukeUnits;

/**
 * The commands of a carrier ship besides moving:
 * <ul>
 * <li><b>Arm</b> starts arming the nuke aboard, and hands the ship over to the Launch command;</li>
 * <li><b>Disarm</b> undoes it and hands the ship back to Move;</li>
 * <li><b>Launch</b> sends the armed nuke to each target the player gives. A target given while it is still arming or reloading is kept until
 * it can go.</li>
 * </ul>
 */
public class CarrierAI extends AIController{
	public enum Mode{
		arm, disarm, launch
	}

	private final Mode mode;
	private Vec2 pending, handled;

	public CarrierAI(Mode mode){
		this.mode = mode;
	}

	public static NukeCarrierAbility abilityOf(mindustry.gen.Unit unit){
		return NukeCarrierAbility.of(unit);
	}

	/** A click on the ground. */
	@Override
	public void commandPosition(Vec2 pos){
		if(mode == Mode.launch) pending = pos == null ? null : pos.cpy();
	}

	@Override
	public void updateMovement(){
		if(Vars.net.client()) return;
		NukeCarrierAbility ability = abilityOf(unit);
		if(ability == null) return;

		if(mode != Mode.launch){
			ability.wantArmed = mode == Mode.arm;
			//Done: on to the next command.
			if(unit.controller() instanceof CommandAI){
				((CommandAI)unit.controller()).command(mode == Mode.arm ? NukeUnits.carrierLaunchCommand : mindustry.ai.UnitCommand.moveCommand);
			}
			return;
		}

		//A target given before this controller existed was kept by the commanding AI, which only forwards later ones.
		if(pending == null && unit.controller() instanceof CommandAI){
			Vec2 held = ((CommandAI)unit.controller()).targetPos;
			if(held != null && (handled == null || !handled.epsilonEquals(held, 1f))) pending = held.cpy();
		}
		if(pending == null) return;

		if(ability.launch(unit, pending)){
			handled = pending;
			pending = null;
		}
	}

	/** No weapons, nothing to aim. */
	@Override
	public void updateTargeting(){
	}
}
