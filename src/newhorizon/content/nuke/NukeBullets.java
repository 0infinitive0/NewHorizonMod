package newhorizon.content.nuke;

import arc.graphics.Color;
import newhorizon.expand.bullets.nuke.ZoneBulletType;

/** What the blasts of the special nukes leave behind. */
public class NukeBullets{
	public static ZoneBulletType radiationZone, incendiaryZone;

	public static void load(){
		radiationZone = new ZoneBulletType(){{
			radius = 200f;
			lifetime = 60f * 25f;
			unitDps = 36f;
			buildingDps = 9f;
			zoneStatus = NukeStatus.irradiated;
			color = Color.valueOf("9bff4a");
		}};

		incendiaryZone = new ZoneBulletType(){{
			radius = 165f;
			lifetime = 60f * 10f;
			unitDps = 24f;
			buildingDps = 12f;
			zoneStatus = mindustry.content.StatusEffects.burning;
			statusTime = 60f * 3f;
			color = Color.valueOf("ff6a3d");
		}};
	}
}
