package newhorizon.content.nuke;

import arc.graphics.Color;
import mindustry.content.Fx;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.bullet.MissileBulletType;
import newhorizon.expand.bullets.nuke.ArcOrbBulletType;
import newhorizon.expand.bullets.nuke.ForceZoneBulletType;
import newhorizon.expand.bullets.nuke.QuakeBulletType;
import newhorizon.expand.bullets.nuke.SpinLaserBulletType;
import newhorizon.expand.bullets.nuke.SwarmSpawnerBulletType;
import newhorizon.expand.bullets.nuke.TornadoBulletType;
import newhorizon.expand.bullets.nuke.ZoneBulletType;
import newhorizon.expand.units.nuke.Warhead;

/** What the blasts of the special nukes leave behind. */
public class NukeBullets{
	public static ZoneBulletType radiationZone, incendiaryZone;
	/** The pull of a gravity nuke (its blast comes when the pull ends), and the push of a shockwave nuke. */
	public static ForceZoneBulletType gravityZone, shockZone;
	/** The laser nuke's beam, and the swarm nuke's missile and the thing that fires them in waves. */
	public static SpinLaserBulletType spinLaser;
	public static BulletType swarmMissile;
	public static SwarmSpawnerBulletType swarmSpawner;
	/** The storm nuke's tornado. */
	public static TornadoBulletType tornado;
	/** The lightning nuke's ball of light, and the earthquake nuke's quake. */
	public static ArcOrbBulletType arcOrb, siphonOrb;
	public static QuakeBulletType quake;

	public static void load(){
		radiationZone = new ZoneBulletType(){{
			radius = 200f;
			lifetime = 60f * 25f;
			//A share of what each target has, not a fixed amount: the bigger it is the more it loses.
			unitDps = 0f;
			buildingDps = 0f;
			unitPercent = 0.03f;
			buildingPercent = 0.025f;
			zoneStatus = NukeStatus.irradiated;
			color = Color.valueOf("9bff4a");
			particle = NukeFx.fallout;
			particleDensity = 0.000028f;
		}};

		incendiaryZone = new ZoneBulletType(){{
			radius = 165f;
			lifetime = 60f * 10f;
			unitDps = 24f;
			buildingDps = 12f;
			zoneStatus = mindustry.content.StatusEffects.burning;
			statusTime = 60f * 3f;
			color = Color.valueOf("ff6a3d");
			particle = NukeFx.ember;
			particleDensity = 0.00003f;
		}};

		gravityZone = new ForceZoneBulletType(){{
			radius = 250f;
			lifetime = 60f * 3f;
			edge = 0.12f;
			center = 0.6f;
			color = Warhead.gravity.color;
			onEnd = b -> Warhead.gravity.finish(b.x, b.y);
		}};

		//Five pulses, each a short hard shove out from the middle.
		shockZone = new ForceZoneBulletType(){{
			radius = 280f;
			pulses = 5;
			pulseInterval = 24f;
			pulseOn = 6f;
			lifetime = 5 * 24f - 2f;
			edge = -4f;
			center = -6f;
			color = Warhead.shockwave.color;
		}};

		//A meltdown's beam, left to burn for eight seconds while it turns once round.
		spinLaser = new SpinLaserBulletType(10.5f){{
			length = 520f;
			width = 20f;
			lifetime = 60f * 8f;
			turns = 1f;
			//Checked every tick: the beam moves several pixels a tick at its far end and must not skip over what it passes.
			damageInterval = 1f;
			largeHit = true;
			fadeTime = 24f;
			drawSize = 1200f;
			hitEffect = Fx.hitMeltdown;
			hitColor = Color.valueOf("ff9c5a");
			colors = new Color[]{Color.valueOf("ec7458aa"), Color.valueOf("ff9c5a"), Color.white};
			incendChance = 0.4f;
			incendSpread = 5f;
			incendAmount = 1;
			status = mindustry.content.StatusEffects.melting;
			statusDuration = 60f * 4f;
		}};

		swarmMissile = new MissileBulletType(3.4f, 22f){{
			sprite = newhorizon.NewHorizon.name("swarm-missile");
			width = 8f;
			height = 15f;
			trailLength = 12;
			trailWidth = 1.3f;
			trailColor = Color.valueOf("ffd84d");
			lifetime = 140f;
			splashDamage = 28f;
			splashDamageRadius = 26f;
			homingPower = 0.09f;
			homingRange = 280f;
			collidesTiles = true;
			trailLength = 0;
			backColor = lightColor = hitColor = Color.valueOf("ffd84d");
			frontColor = Color.white;
			hitEffect = despawnEffect = Fx.blastExplosion;
			shootEffect = smokeEffect = Fx.none;
			weaveScale = 5f;
			weaveMag = 1f;
		}};

		swarmSpawner = new SwarmSpawnerBulletType(){{
			missile = swarmMissile;
			waves = 3;
			count = 120;
			interval = 60f;
		}};

		arcOrb = new ArcOrbBulletType(){{
			radius = 210f;
			lifetime = 60f * 10f;
			interval = 15f;
			maxTargets = 20;
			arcDamage = 50f;
			color = Color.valueOf("ffe066");
			arcColor = Color.valueOf("8fe3ff");
		}};

		//One quick arc at a time that keeps the orb alive for as long as it strikes something; after thirty seconds it starts to pull.
		//Point defence can shoot it down: its damage is what it can take.
		siphonOrb = new ArcOrbBulletType(){{
			radius = 210f;
			lifetime = 60f * 10f;
			interval = 4f;
			maxTargets = 1;
			arcDamage = 20f;
			statusTime = 60f;
			refresh = true;
			pullStart = 60f * 30f;
			pullGrowth = 0.0005f;
			pullMax = 1.2f;
			hittable = true;
			damage = 700f;
			color = Color.valueOf("9c3fe8");
			arcColor = Color.valueOf("c885ff");
		}};

		quake = new QuakeBulletType(){{
			radius = 290f;
			lifetime = 60f * 4f;
			interval = 15f;
			color = Color.valueOf("b08968");
		}};

		tornado = new TornadoBulletType(){{
			radius = 170f;
			lifetime = 60f * 10f;
			color = Color.valueOf("9bb7d4");
		}};
	}
}
