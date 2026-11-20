package newhorizon;

import arc.Core;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.gen.Unit;
import newhorizon.expand.packets.DroneStandDownPacket;
import newhorizon.expand.packets.DroneTypePacket;
import newhorizon.expand.units.EnergyUnit;
import newhorizon.expand.units.ablility.DroneBayAbility;
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
		updateDroneKey();
		updateStandDownKey();

		if(currentUnit instanceof EnergyUnit){
			EnergyUnit u = (EnergyUnit)currentUnit;
			if(u.teleportValid() && Core.input.keyTap(NHKeyBinding.teleport)){
				Tmp.v1.set(Core.camera.unproject(Core.input.mouse())).sub(u).clampLength(0, u.teleportRange).add(u);
				
				u.teleport(Tmp.v1.x, Tmp.v1.y);
			}
		}
	}

	/** The pilot's key: a Mothership flown by the player takes the next drone type. */
	protected void updateDroneKey(){
		if(currentUnit == null || !Vars.state.isGame() || Core.scene.hasKeyboard() || !Core.input.keyTap(NHKeyBinding.droneType)) return;
		DroneBayAbility bay = DroneBayAbility.of(currentUnit);
		if(bay == null) return;
		int next = (bay.index() + 1) % bay.types.size;
		if(Vars.net.client()) Vars.net.send(new DroneTypePacket(next), true);
		else if(!bay.select(next)){
			Vars.ui.showInfoToast(Core.bundle.format("ui.nh-drone-refused", bay.type().localizedName), 2f);
			return;
		}
		Vars.ui.showInfoToast(Core.bundle.format("ui.nh-drone-type", bay.types.get(next).localizedName), 2f);
	}

	/** The pilot's Stand down key: a Mothership flown by the player holds its drones back, or lets them attack again. */
	protected void updateStandDownKey(){
		if(currentUnit == null || !Vars.state.isGame() || Core.scene.hasKeyboard() || !Core.input.keyTap(NHKeyBinding.standDown)) return;
		DroneBayAbility bay = DroneBayAbility.of(currentUnit);
		if(bay == null) return;
		boolean on = !bay.standDown;
		if(Vars.net.client()) Vars.net.send(new DroneStandDownPacket(on), true);
		else bay.setStandDown(on);
		Vars.ui.showInfoToast(Core.bundle.get(on ? "ui.nh-stand-down-on" : "ui.nh-stand-down-off"), 2f);
	}
}
