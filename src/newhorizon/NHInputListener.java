package newhorizon;

import arc.Core;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.gen.Unit;
import newhorizon.expand.units.EnergyUnit;
import newhorizon.util.annotation.HeadlessDisabled;

@HeadlessDisabled
public class NHInputListener{
	public Unit currentUnit;

	public NHInputListener(){}

	public static void registerModBinding(){
		NHKeyBinding.load();
	}
	public void update(){
		if(Vars.player != null)updatePlayerStatus();
		
		updateActiveAbility();
	}
	
	protected void updatePlayerStatus(){
		currentUnit = Vars.player.unit();
	}
	
	protected void updateActiveAbility(){
		if(currentUnit instanceof EnergyUnit){
			EnergyUnit u = (EnergyUnit)currentUnit;
			if(u.teleportValid() && Core.input.keyTap(NHKeyBinding.teleport)){
				Tmp.v1.set(Core.camera.unproject(Core.input.mouse())).sub(u).clampLength(0, u.teleportRange).add(u);
				
				u.teleport(Tmp.v1.x, Tmp.v1.y);
			}
		}
	}
}
