package newhorizon.expand.units.nuke;

import arc.math.geom.Vec2;
import mindustry.Vars;
import mindustry.ai.UnitCommand;
import mindustry.ai.types.CommandAI;
import mindustry.entities.units.AIController;
import newhorizon.content.nuke.NukeUnits;

/**
 * The commands of a carrier ship besides moving, which work like a launcher's buttons:
 * <ul>
 * <li><b>Target</b>: a click on the map picks the target of the ship (it is kept until it is cleared);</li>
 * <li><b>Clear</b>: forgets the target, and hands the ship back to Move;</li>
 * <li><b>Launch</b>: sends the nuke at the picked target (straight ahead if there is none), arming it first if it was not armed, and then
 * hands the ship back to Move.</li>
 * </ul>
 */
public class CarrierAI extends AIController{
	public enum Mode{
		target, clear, launch
	}

	private final Mode mode;

	public CarrierAI(Mode mode){
		this.mode = mode;
	}

	public static NukeCarrierAbility abilityOf(mindustry.gen.Unit unit){
		return NukeCarrierAbility.of(unit);
	}

	/** A click on the ground. */
	@Override
	public void commandPosition(Vec2 pos){
		if(mode != Mode.target || Vars.net.client() || unit == null) return;
		NukeCarrierAbility ability = abilityOf(unit);
		if(ability != null) ability.setTarget(unit, pos);
	}

	@Override
	public void updateMovement(){
		if(Vars.net.client()) return;
		NukeCarrierAbility ability = abilityOf(unit);
		if(ability == null) return;

		switch(mode){
			case target -> {
				//A click is the ship's target, not somewhere to sail to: take it from the commanding AI, which keeps it (and only forwards
				//the ones that come after this controller exists), so that it does not leak into the Move command.
				if(unit.controller() instanceof CommandAI){
					CommandAI commander = (CommandAI)unit.controller();
					if(commander.targetPos != null){
						ability.setTarget(unit, commander.targetPos);
						commander.targetPos = null;
					}
				}
			}
			case clear -> {
				ability.clearTarget();
				back();
			}
			case launch -> {
				//Nothing to launch, or it has gone: back to Move. Otherwise arm it if need be and go as soon as it is ready.
				if(!ability.loaded()){
					back();
					return;
				}
				ability.order(10f);
				if(ability.launch(unit, ability.target)) back();
			}
		}
	}

	private void back(){
		if(unit.controller() instanceof CommandAI){
			CommandAI commander = (CommandAI)unit.controller();
			//Nowhere to sail to.
			commander.targetPos = null;
			commander.attackTarget = null;
			commander.command(UnitCommand.moveCommand);
		}
	}

	/** No weapons, nothing to aim. */
	@Override
	public void updateTargeting(){
	}
}
