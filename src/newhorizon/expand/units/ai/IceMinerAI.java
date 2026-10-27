package newhorizon.expand.units.ai;

import mindustry.content.Blocks;
import mindustry.entities.units.AIController;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.world.Tile;
import newhorizon.content.NHItems;
import newhorizon.expand.block.production.ice.IceMiningStation;
import newhorizon.expand.block.env.IceFloor;
import newhorizon.expand.block.production.ice.IceMiningStation.IceMiningStationBuild;

public class IceMinerAI extends AIController{
	public IceMiningStationBuild home;
	public Tile target;
	public boolean mining = true;
	
	@Override
	public void updateMovement(){
		if(!unit.canMine())return;
		
		if(home == null || !home.isValid() || timer.get(timerTarget2, 180f)){
			findHome();
		}
		
		if(home == null){
			releaseTarget();
			return;
		}
		
		if(target != null && !validTarget(target)){
			releaseTarget();
		}
		
		if(mining){
			if(unit.stack.amount >= unit.type.itemCapacity || !unit.acceptsItem(NHItems.ice)){
				mining = false;
				return;
			}
			
			if(target == null && timer.get(timerTarget3, 30f)){
				target = home.claimIce(unit);
			}
			
			if(target != null){
				moveTo(target, unit.type.mineRange / 2f, 20f);
				
				if(unit.within(target, unit.type.mineRange)){
					unit.mineTile = target;
				}
			}else{
				unit.mineTile = null;
				
				//Nothing left to mine, bring what has been collected.
				if(unit.stack.amount > 0){
					mining = false;
				}else{
					circle(home, home.block.size * 8f * 2f);
				}
			}
		}else{
			unit.mineTile = null;
			releaseTarget();
			
			if(unit.stack.amount == 0){
				mining = true;
				return;
			}
			
			if(unit.within(home, unit.type.range)){
				int accepted = home.acceptStack(unit.stack.item, unit.stack.amount, unit);
				if(accepted > 0){
					Call.transferItemTo(unit, unit.stack.item, accepted, unit.x, unit.y, home);
				}
				
				if(unit.stack.amount <= 0)mining = true;
			}
			
			circle(home, unit.type.range / 1.8f);
		}
	}
	
	protected boolean validTarget(Tile tile){
		return tile.floor() instanceof IceFloor && tile.block() == Blocks.air;
	}
	
	protected void releaseTarget(){
		if(target != null && home != null)home.release(target);
		target = null;
		unit.mineTile = null;
	}
	
	protected void findHome(){
		IceMiningStationBuild closest = null;
		float dst = Float.MAX_VALUE;
		
		for(IceMiningStation block : IceMiningStation.all){
			for(Building build : unit.team.data().getBuildings(block)){
				float d = unit.dst2(build);
				if(build instanceof IceMiningStationBuild && d < dst){
					dst = d;
					closest = (IceMiningStationBuild)build;
				}
			}
		}
		
		if(closest != home){
			releaseTarget();
			home = closest;
		}
	}
}
