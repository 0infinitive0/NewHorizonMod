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

/** Hauls the liquid of the liquid port it is bound to, or of the liquid port that has the most of what the depots want, into the liquid depots. */
public class TankerHaulAI extends HaulAI{
	public LiquidPort.LiquidPortBuild port;
	public LiquidDepot.LiquidDepotBuild depot;
	
	protected float waited, nextSearch;
	
	@Override
	public void updateMovement(){
		if(!(unit instanceof TankerEntity))return;
		TankerEntity tanker = (TankerEntity)unit;
		
		if(port == null || !port.isValid() || timer.get(timerTarget2, 45f)){
			HubBlock.HubBuild hub = HubBlock.boundHub(unit);
			if(hub instanceof LiquidPort.LiquidPortBuild && !(ignored(hub) && tanker.liquidAmount < 1f)){
				port = (LiquidPort.LiquidPortBuild)hub;
			}else if(hub instanceof LiquidPort.LiquidPortBuild){
				//Its port can not be got to for now: use another one until the ban is over.
				LiquidPort.LiquidPortBuild other = fullestPort();
				port = other != null ? other : (LiquidPort.LiquidPortBuild)hub;
			}else if(port == null || !port.isValid() || ignored(port) || tanker.liquidAmount < 1f){
				//Not bound: use any liquid port that has a liquid some depot wants.
				port = fullestPort();
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
			if(depot == null || !depot.isValid() || ignored(depot) || !depot.wants(tanker.liquid)){
				//The depot is full or gone: look for another one at once, but not every tick when there is none.
				depot = null;
				if(Time.time >= nextSearch){
					depot = closestDepot(tanker.liquid, tanker.liquidAmount);
					if(depot == null)nextSearch = Time.time + 30f;
				}
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
	
	/** @return the liquid port of the team with the most liquid that some depot wants, the closest one if equal, or null. */
	protected LiquidPort.LiquidPortBuild fullestPort(){
		LiquidPort.LiquidPortBuild best = null;
		float most = 1f, dst = Float.MAX_VALUE;
		for(HubBlock hub : HubBlock.all){
			if(!(hub instanceof LiquidPort))continue;
			for(Building build : unit.team.data().getBuildings(hub)){
				LiquidPort.LiquidPortBuild candidate = (LiquidPort.LiquidPortBuild)build;
				float stock = candidate.liquids.currentAmount();
				float d = candidate.dst2(unit);
				if(stock <= 1f || stock < most || (stock == most && d >= dst) || ignored(candidate) || closestDepot(candidate.liquids.current()) == null)continue;
				most = stock;
				dst = d;
				best = candidate;
			}
		}
		return best;
	}
	
	protected LiquidDepot.LiquidDepotBuild closestDepot(Liquid liquid){
		return closestDepot(liquid, 0f);
	}
	
	/** The closest depot that wants the liquid and has room for all of it; if none has, the closest with any room. */
	protected LiquidDepot.LiquidDepotBuild closestDepot(Liquid liquid, float needed){
		LiquidDepot.LiquidDepotBuild closest = null, fits = null;
		float dst = Float.MAX_VALUE, fitDst = Float.MAX_VALUE;
		for(LiquidDepot block : LiquidDepot.all){
			for(Building build : unit.team.data().getBuildings(block)){
				LiquidDepot.LiquidDepotBuild candidate = (LiquidDepot.LiquidDepotBuild)build;
				if(ignored(candidate) || !candidate.wants(liquid))continue;
				float d = candidate.dst2(unit);
				if(d < dst){
					dst = d;
					closest = candidate;
				}
				if(d < fitDst && candidate.block.liquidCapacity - candidate.liquids.get(liquid) >= needed){
					fitDst = d;
					fits = candidate;
				}
			}
		}
		return fits != null ? fits : closest;
	}
}
