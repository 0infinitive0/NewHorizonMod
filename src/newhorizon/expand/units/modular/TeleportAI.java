package newhorizon.expand.units.modular;

import arc.math.geom.Vec2;
import mindustry.Vars;
import mindustry.ai.UnitCommand;
import mindustry.ai.types.CommandAI;
import mindustry.entities.units.AIController;

/**
 * The Blink and Jump commands of a modular frame. Choosing the command and then clicking the map sends the frame to the clicked place (as
 * far as its drive reaches, see {@link TeleportAbility#target}), and hands control back to Move at once. A frame without the matching
 * drive does nothing and goes straight back to Move. It keeps the frame's own targeting, so the guns still fire while it waits for the click.
 */
public class TeleportAI extends AIController{
	private final boolean longRange;

	public TeleportAI(boolean longRange){
		this.longRange = longRange;
	}

	/** A click on the ground. */
	@Override
	public void commandPosition(Vec2 pos){
		if(Vars.net.client() || unit == null) return;
		TeleportAbility drive = TeleportAbility.of(unit, longRange);
		if(drive != null) drive.start(unit, pos);
		back();
	}

	@Override
	public void updateMovement(){
		if(Vars.net.client() || unit == null) return;
		TeleportAbility drive = TeleportAbility.of(unit, longRange);
		if(drive == null){
			back();
			return;
		}
		//A click that reached the commanding AI before this controller took over: use it, and keep it out of the Move command.
		if(unit.controller() instanceof CommandAI){
			CommandAI commander = (CommandAI)unit.controller();
			if(commander.targetPos != null){
				drive.start(unit, commander.targetPos);
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
