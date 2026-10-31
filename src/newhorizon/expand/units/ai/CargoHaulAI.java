package newhorizon.expand.units.ai;

import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.type.Item;
import newhorizon.expand.block.cargo.Depot;
import newhorizon.expand.block.cargo.HubBlock;
import newhorizon.expand.block.cargo.Port;
import newhorizon.expand.hiemara.Hold;

/**
 * Hauls items from the port the unit is bound to into the depots. A unit that is not bound to a port takes items from
 * the port that has the most of what the depots want.
 * Ships that carry several kinds of item take as many kinds as there are depots for, and visit depots until they are empty.
 */
public class CargoHaulAI extends HaulAI{
	public Port.PortBuild port;
	public Depot.DepotBuild depot;
	protected float waited;
	
	@Override
	public void updateMovement(){
		if(port == null || !port.isValid() || timer.get(timerTarget2, 45f)){
			HubBlock.HubBuild hub = HubBlock.boundHub(unit);
			if(hub instanceof Port.PortBuild && !(ignored(hub) && Hold.empty(unit))){
				port = (Port.PortBuild)hub;
			}else if(hub instanceof Port.PortBuild){
				//Its port can not be got to for now: use another one until the ban is over. With nothing else it keeps waiting.
				Port.PortBuild other = fullestPort();
				port = other != null ? other : (Port.PortBuild)hub;
			}else if(port == null || !port.isValid() || ignored(port) || Hold.empty(unit)){
				//Not bound: take items from any port that has something a depot wants. With cargo on board the unit
				//keeps its port, so it can put the cargo back.
				port = fullestPort();
			}
		}
		
		if(port == null){
			unit.mineTile = null;
			return;
		}
		
		if(Hold.empty(unit)){
			depot = null;
			waited = 0f;
			
			if(!near(port))travel(port);
			if(near(port) && timer.get(timerTarget3, 15f)){
				load();
			}
		}else{
			if(depot == null || !depot.isValid() || ignored(depot) || !wantsAny(depot)){
				depot = timer.get(timerTarget4, 30f) ? closestDepotFor(Hold.items(unit)) : null;
			}
			
			if(depot == null){
				//Nowhere to go with it: wait at the port, and put it back if that takes too long.
				if(!near(port))travel(port);
				waited += Time.delta;
				if(waited > 600f && near(port) && !Vars.net.client()){
					for(Item item : Hold.items(unit)){
						port.handleStack(item, Hold.get(unit, item), unit);
					}
					Hold.clear(unit);
				}
				return;
			}
			
			waited = 0f;
			travel(depot);
			if(near(depot))unload();
		}
	}
	
	/** @return the port of the team that holds the most of what the depots want, the closest one if equal, or null. */
	protected Port.PortBuild fullestPort(){
		Port.PortBuild best = null;
		int most = 0;
		float dst = Float.MAX_VALUE;
		for(HubBlock hub : HubBlock.all){
			if(!(hub instanceof Port))continue;
			for(Building build : unit.team.data().getBuildings(hub)){
				Port.PortBuild candidate = (Port.PortBuild)build;
				int stock = wantedStock(candidate);
				float d = candidate.dst2(unit);
				if(stock <= 0 || stock < most || (stock == most && d >= dst) || ignored(candidate))continue;
				most = stock;
				dst = d;
				best = candidate;
			}
		}
		return best;
	}
	
	/** @return how many items the port holds that some depot wants. */
	protected int wantedStock(Port.PortBuild candidate){
		int stock = 0;
		for(Item item : Vars.content.items()){
			int amount = candidate.items.get(item);
			if(amount > 0 && closestDepotFor(Seq.with(item)) != null)stock += amount;
		}
		return stock;
	}
	
	protected boolean wantsAny(Depot.DepotBuild depot){
		for(Item item : Hold.items(unit)){
			if(depot.wants(item))return true;
		}
		return false;
	}
	
	/** Takes what the depots want from the port. */
	protected void load(){
		if(Vars.net.client())return;
		
		//Most stocked first.
		Seq<Item> candidates = new Seq<>();
		for(Item item : Vars.content.items()){
			if(port.items.get(item) > 0 && closestDepotFor(Seq.with(item)) != null)candidates.add(item);
		}
		candidates.sort(item -> -port.items.get(item));
		
		for(Item item : candidates){
			int room = Hold.room(unit, item);
			int amount = Math.min(room, port.items.get(item));
			if(amount <= 0)continue;
			
			if(Hold.multi(unit)){
				port.items.remove(item, amount);
				Hold.add(unit, item, amount);
			}else{
				//A plain unit carries one kind of item, and the call also shows the item flying in.
				Call.takeItems(port, item, amount, unit);
				break;
			}
		}
		
		if(!Hold.empty(unit))depot = closestDepotFor(Hold.items(unit));
	}
	
	/** Puts what the depot wants into it. */
	protected void unload(){
		for(Item item : Hold.items(unit)){
			int accepted = depot.acceptStack(item, Hold.get(unit, item), unit);
			if(accepted <= 0)continue;
			
			if(Hold.multi(unit)){
				Hold.remove(unit, item, accepted);
				depot.handleStack(item, accepted, unit);
			}else{
				Call.transferItemTo(unit, item, accepted, unit.x, unit.y, depot);
			}
		}
	}
	
	protected Depot.DepotBuild closestDepotFor(Seq<Item> items){
		Depot.DepotBuild closest = null;
		float dst = Float.MAX_VALUE;
		for(Depot block : Depot.all){
			for(Building build : unit.team.data().getBuildings(block)){
				Depot.DepotBuild candidate = (Depot.DepotBuild)build;
				if(ignored(candidate))continue;
				boolean wants = false;
				for(Item item : items){
					if(candidate.wants(item)){
						wants = true;
						break;
					}
				}
				if(!wants)continue;
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
