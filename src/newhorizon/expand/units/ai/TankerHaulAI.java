package newhorizon.expand.units.ai;

import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.type.Liquid;
import newhorizon.expand.block.cargo.HubBlock;
import newhorizon.expand.block.cargo.LiquidDepot;
import newhorizon.expand.block.cargo.LiquidPort;
import newhorizon.expand.units.hiemara.TankerUnitType;
import newhorizon.expand.units.unitEntity.TankerEntity;

/** Hauls the liquid of the liquid port it is bound to, or of any liquid port with something a depot wants, into the liquid depots. */
public class TankerHaulAI extends HaulAI{
	public LiquidPort.LiquidPortBuild port;
	public LiquidDepot.LiquidDepotBuild depot;
	
	protected float waited;
	
	@Override
	public void updateMovement(){
		if(!(unit instanceof TankerEntity))return;
		TankerEntity tanker = (TankerEntity)unit;
		
		if(port == null || !port.isValid() || timer.get(timerTarget2, 45f)){
			HubBlock.HubBuild hub = HubBlock.boundHub(unit);
			if(hub instanceof LiquidPort.LiquidPortBuild){
				port = (LiquidPort.LiquidPortBuild)hub;
			}else if(port == null || !port.isValid() || tanker.liquidAmount < 1f){
				//Not bound: use any liquid port that has a liquid some depot wants.
				port = closestStockedPort();
			}
		}
		
		if(port == null)return;
		
		if(tanker.liquidAmount < 1f){
			depot = null;
			waited = 0f;
			
			if(!near(port))travel(port);
			if(near(port) && timer.get(timerTarget3, 15f) && !Vars.net.client()){
				Liquid liquid = port.liquids.current();
				float amount = Math.min(((TankerUnitType)unit.type).liquidCapacity, port.liquids.get(liquid));
				if(amount > 1f && closestDepot(liquid) != null){
					port.liquids.remove(liquid, amount);
					tanker.liquid = liquid;
					tanker.liquidAmount = amount;
					depot = closestDepot(liquid);
				}
			}
		}else{
			if(depot == null || !depot.isValid() || !depot.wants(tanker.liquid)){
				depot = timer.get(timerTarget4, 30f) ? closestDepot(tanker.liquid) : null;
			}
			
			if(depot == null){
				if(!near(port))travel(port);
				waited += Time.delta;
				if(waited > 600f && near(port) && !Vars.net.client()){
					port.liquids.add(tanker.liquid, tanker.liquidAmount);
					tanker.liquidAmount = 0f;
					tanker.liquid = null;
				}
				return;
			}
			
			waited = 0f;
			travel(depot);
			if(near(depot) && !Vars.net.client()){
				float room = depot.block.liquidCapacity - depot.liquids.get(tanker.liquid);
				float moved = Math.min(room, tanker.liquidAmount);
				if(moved > 0f){
					depot.liquids.add(tanker.liquid, moved);
					tanker.liquidAmount -= moved;
					if(tanker.liquidAmount < 1f){
						tanker.liquidAmount = 0f;
						tanker.liquid = null;
					}
				}
			}
		}
	}
	
	/** @return the closest liquid port of the team that holds a liquid some depot wants, or null. */
	protected LiquidPort.LiquidPortBuild closestStockedPort(){
		LiquidPort.LiquidPortBuild closest = null;
		float dst = Float.MAX_VALUE;
		for(HubBlock hub : HubBlock.all){
			if(!(hub instanceof LiquidPort))continue;
			for(Building build : unit.team.data().getBuildings(hub)){
				LiquidPort.LiquidPortBuild candidate = (LiquidPort.LiquidPortBuild)build;
				float d = candidate.dst2(unit);
				if(d >= dst || candidate.liquids.currentAmount() <= 1f || closestDepot(candidate.liquids.current()) == null)continue;
				dst = d;
				closest = candidate;
			}
		}
		return closest;
	}
	
	protected LiquidDepot.LiquidDepotBuild closestDepot(Liquid liquid){
		LiquidDepot.LiquidDepotBuild closest = null;
		float dst = Float.MAX_VALUE;
		for(LiquidDepot block : LiquidDepot.all){
			for(Building build : unit.team.data().getBuildings(block)){
				LiquidDepot.LiquidDepotBuild candidate = (LiquidDepot.LiquidDepotBuild)build;
				if(!candidate.wants(liquid))continue;
				float d = candidate.dst2(unit);
				if(d < dst){
					dst = d;
					closest = candidate;
				}
			}
		}
		return closest;
	}
}
