package newhorizon.expand.units.nuke;

import arc.math.geom.Vec2;
import mindustry.Vars;
import mindustry.ai.types.CommandAI;
import mindustry.entities.units.AIController;

/**
 * The Launch command of a carrier ship: it stays where it is, and each target the player gives it sends its nuke there. If the nuke is not
 * armed yet, the order arms it and goes as soon as it is ready; the Armed stance arms it ahead of time.
 */
public class CarrierAI extends AIController{
	private Vec2 pending, handled;

	public static NukeCarrierAbility abilityOf(mindustry.gen.Unit unit){
		return NukeCarrierAbility.of(unit);
	}

	/** A click on the ground. */
	@Override
	public void commandPosition(Vec2 pos){
		pending = pos == null ? null : pos.cpy();
	}

	@Override
	public void updateMovement(){
		if(Vars.net.client()) return;
		NukeCarrierAbility ability = abilityOf(unit);
		if(ability == null) return;

		//A target given before this controller existed was kept by the commanding AI, which only forwards later ones.
		if(pending == null && unit.controller() instanceof CommandAI){
			Vec2 held = ((CommandAI)unit.controller()).targetPos;
			if(held != null && (handled == null || !handled.epsilonEquals(held, 1f))) pending = held.cpy();
		}
		if(pending == null) return;

		//A waiting order arms the nuke if it is not armed yet.
		if(ability.loaded()) ability.order(10f);
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
