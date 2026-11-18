package newhorizon.expand.units.ablility;

import arc.scene.style.TextureRegionDrawable;
import mindustry.ai.UnitStance;
import mindustry.type.UnitType;

/**
 * Picks the drone type of a Mothership. One of these appears among the stance buttons of a selected Mothership for each drone type, and only
 * one can be on at a time. The bay reads them, see {@link DroneBayAbility#syncStances}.
 */
public class DroneStance extends UnitStance{
	public final UnitType drone;

	public DroneStance(UnitType drone){
		super("nh-drone-" + drone.name, "", null);
		this.drone = drone;
	}

	@Override
	public String localized(){
		return drone.localizedName;
	}

	@Override
	public TextureRegionDrawable getIcon(){
		return new TextureRegionDrawable(drone.uiIcon);
	}
}
