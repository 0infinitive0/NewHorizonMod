package newhorizon.content.nuke;

import arc.graphics.Color;
import newhorizon.expand.bullets.nuke.ForceZoneBulletType;
import newhorizon.expand.bullets.nuke.ZoneBulletType;
import newhorizon.expand.units.nuke.Warhead;

/** What the blasts of the special nukes leave behind. */
public class NukeBullets{
	public static ZoneBulletType radiationZone, incendiaryZone;
	/** The pull of a gravity nuke (its blast comes when the pull ends), and the push of a shockwave nuke. */
	public static ForceZoneBulletType gravityZone, shockZone;

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

		gravityZone = new ForceZoneBulletType(){{
			radius = 250f;
			lifetime = 60f * 3f;
			edge = 0.12f;
			center = 0.6f;
			color = Warhead.gravity.color;
			onEnd = b -> Warhead.gravity.finish(b.x, b.y);
		}};

		shockZone = new ForceZoneBulletType(){{
			radius = 280f;
			lifetime = 50f;
			edge = -2.4f;
			center = -3.6f;
			color = Warhead.shockwave.color;
		}};
	}
}
