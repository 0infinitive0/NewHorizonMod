package newhorizon.expand.units.nuke;

import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.entities.units.AIController;
import mindustry.gen.Payloadc;
import mindustry.gen.Unit;
import newhorizon.content.nuke.NukeFx;
import newhorizon.content.nuke.NukeUnits;

/** The Dock command: an inactive nuke flies to the nearest carrier of its team that has room and is taken on board. */
public class DockAI extends AIController{
	@Override
	public void updateMovement(){
		if(!NukeUnits.isInactive(unit.type)) return;
		//Passengers cannot be taken along.
		if(unit instanceof Payloadc && !((Payloadc)unit).payloads().isEmpty()) return;

		Unit carrier = Units.closest(unit.team, unit.x, unit.y, 8000f, u -> {
			NukeCarrierAbility a = CarrierAI.abilityOf(u);
			return a != null && a.free();
		});
		if(carrier == null) return;

		moveTo(carrier, carrier.hitSize / 2f + 6f);
		if(Vars.net.client() || !unit.within(carrier, carrier.hitSize / 2f + 16f)) return;
		if(CarrierAI.abilityOf(carrier).load(unit.type)){
			NukeFx.armed.at(unit.x, unit.y);
			unit.remove();
		}
	}

	/** No weapons, nothing to aim. */
	@Override
	public void updateTargeting(){
	}
}
