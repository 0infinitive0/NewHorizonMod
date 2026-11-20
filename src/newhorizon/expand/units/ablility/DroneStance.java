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

	/** The longest side of the icon on the button, in px: the button is 50 px, and the base game's own stance icons are about 32 px. */
	public static final float iconSize = 34f;

	/** The scale that fits an image of this size within {@code max} px on its longer side; never above 1 (a small image is not blown up). */
	public static float fitScale(float width, float height, float max){
		float longest = Math.max(width, height);
		return longest <= max || longest <= 0f ? 1f : max / longest;
	}

	@Override
	public TextureRegionDrawable getIcon(){
		var region = drone.uiIcon;
		return new TextureRegionDrawable(region, fitScale(region.width, region.height, iconSize));
	}
}
