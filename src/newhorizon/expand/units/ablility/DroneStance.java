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

	/**
	 * The size of the icon cell for an image of this size: its longer side, but no more than {@code max}. (The game sizes a button's icon cell from
	 * {@code Drawable.imageSize()}, not from the drawable's scale.)
	 */
	public static float fitSize(float width, float height, float max){
		return Math.min(Math.max(width, height), max);
	}

	@Override
	public TextureRegionDrawable getIcon(){
		var region = drone.uiIcon;
		return new TextureRegionDrawable(region, fitScale(region.width, region.height, iconSize)){
			/** {@code Table.button(icon, ...)} makes the icon cell this big; the base class says the sprite's own width, which is far more than the button holds. */
			@Override
			public float imageSize(){
				return fitSize(region.width, region.height, iconSize);
			}
		};
	}
}
