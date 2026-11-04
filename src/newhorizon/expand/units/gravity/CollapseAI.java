package newhorizon.expand.units.gravity;

import arc.math.geom.Vec2;
import mindustry.Vars;
import mindustry.ai.UnitCommand;
import mindustry.ai.types.CommandAI;
import mindustry.entities.units.AIController;

/**
 * The Collapse command of the Gravity Base. Choosing it and then clicking the map drops the black hole where it was clicked (as far as the
 * unit's reach goes, see {@link CollapseAbility#target}) and hands control back to Move at once. A unit that cannot pay, or has no such
 * ability, does nothing and goes straight back to Move. It keeps the unit's own targeting, so the guns still fire while it waits for the click.
 */
public class CollapseAI extends AIController{
	/** A click on the ground. */
	@Override
	public void commandPosition(Vec2 pos){
		if(Vars.net.client() || unit == null) return;
		CollapseAbility a = CollapseAbility.of(unit);
		if(a != null) a.use(unit, pos);
		back();
	}

	@Override
	public void updateMovement(){
		if(Vars.net.client() || unit == null) return;
		if(CollapseAbility.of(unit) == null){
			back();
			return;
		}
		//A click that reached the commanding AI before this controller took over: use it, and keep it out of the Move command.
		if(unit.controller() instanceof CommandAI){
			CommandAI commander = (CommandAI)unit.controller();
			if(commander.targetPos != null){
				CollapseAbility.of(unit).use(unit, commander.targetPos);
				commander.targetPos = null;
				back();
			}
		}
	}

	private void back(){
		if(unit.controller() instanceof CommandAI){
			CommandAI commander = (CommandAI)unit.controller();
			commander.targetPos = null;
			commander.attackTarget = null;
			commander.command(UnitCommand.moveCommand);
		}
	}
}
