package newhorizon.expand.units.ai;

import mindustry.entities.Units;
import mindustry.entities.units.AIController;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import newhorizon.expand.units.ability.CarrierBayAbility;

/**
 * A fighter of a carrier. It only goes for enemies that are within the range of its carrier, and flies back to it, to be
 * taken into the hangar and repaired, when there are none left or it is badly hurt.
 */
public class CarrierFighterAI extends AIController{
	public final Unit carrier;
	public final CarrierBayAbility bay;
	/** Once hurt, a fighter goes all the way back to the hangar, even if the repair field heals it on the way. */
	protected boolean hurt;
	
	public CarrierFighterAI(Unit carrier, CarrierBayAbility bay){
		this.carrier = carrier;
		this.bay = bay;
	}
	
	protected boolean carrierValid(){
		return carrier != null && !carrier.dead && carrier.isAdded();
	}
	
	@Override
	public void updateMovement(){
		if(!carrierValid()){
			//The carrier is gone: fight on on its own.
			if(target != null && unit.hasWeapons()){
				if(unit.type.circleTarget)circleAttack(unit.type.circleTargetRadius);
				else{
					moveTo(target, unit.type.range * 0.8f);
					unit.lookAt(target);
				}
			}
			return;
		}
		
		if(unit.health < unit.maxHealth * bay.recallHealth)hurt = true;
		boolean recall = hurt || !bay.engaged;
		if(recall){
			target = null;
			moveTo(carrier, bay.dockRange * 0.5f, 20f);
			unit.lookAt(carrier);
			if(unit.within(carrier, bay.dockRange))bay.dock(unit);
			return;
		}
		
		if(target != null && unit.hasWeapons()){
			if(unit.type.circleTarget)circleAttack(unit.type.circleTargetRadius);
			else{
				moveTo(target, unit.type.range * 0.8f);
				unit.lookAt(target);
			}
		}else{
			//Nothing to shoot at yet: stay with the carrier.
			moveTo(carrier, bay.dockRange * 3f, 30f);
		}
	}
	
	@Override
	public Teamc findMainTarget(float x, float y, float range, boolean air, boolean ground){
		if(!carrierValid())return super.findMainTarget(x, y, range, air, ground);
		
		//the closest enemy to the fighter, among those that are in the range of the carrier
		Teamc best = null;
		float dst = Float.MAX_VALUE;
		Unit enemy = Units.closestEnemy(unit.team, x, y, bay.range + 80f, u -> u.checkTarget(air, ground) && u.within(carrier, bay.range));
		if(enemy != null){
			best = enemy;
			dst = unit.dst2(enemy);
		}
		if(ground){
			Teamc building = Units.closestTarget(unit.team, carrier.x, carrier.y, bay.range, u -> false, b -> true);
			if(building != null && (best == null || unit.dst2(building) < dst))best = building;
		}
		return best;
	}
}
