package newhorizon.content.jade;

import arc.graphics.Color;
import mindustry.content.Fx;
import mindustry.entities.bullet.*;
import mindustry.gen.Sounds;

import static newhorizon.content.jade.JadeColor.*;

public class JadeBullets{
	public static BulletType
		needle, thornShard, thornShardSilk, thornSpread,
		seed, lifeBolt, bloomMissile,
		lanceBeam, canopyShell, canopyShellCore, briarMortar;

	private static final float bloomTime = 60f * 3f;

	public static void load(){
		//Unit and turret light shots
		needle = new BasicBulletType(3.4f, 9f){{
			width = 4f;
			height = 11f;
			lifetime = 40f;
			status = JadeStatus.bloom;
			statusDuration = bloomTime;
			backColor = trailColor = lightColor = hitColor = jade;
			frontColor = jadeLight;
			trailLength = 6;
			trailWidth = 1.2f;
			shootEffect = JadeFx.thornShoot;
			hitEffect = despawnEffect = JadeFx.leafBurst;
			smokeEffect = Fx.none;
		}};

		thornShard = new BasicBulletType(4.6f, 18f){{
			width = 5f;
			height = 14f;
			lifetime = 38f;
			pierce = true;
			pierceCap = 2;
			knockback = 0.4f;
			status = JadeStatus.bloom;
			statusDuration = bloomTime;
			backColor = trailColor = lightColor = hitColor = jade;
			frontColor = jadeLight;
			trailLength = 8;
			trailWidth = 1.5f;
			ammoMultiplier = 3f;
			shootEffect = JadeFx.thornShoot;
			hitEffect = despawnEffect = JadeFx.leafBurst;
			smokeEffect = Fx.none;
		}};

		thornShardSilk = new BasicBulletType(5.2f, 30f){{
			width = 6f;
			height = 17f;
			lifetime = 36f;
			pierce = true;
			pierceCap = 4;
			knockback = 0.6f;
			homingPower = 0.06f;
			homingRange = 70f;
			reloadMultiplier = 1.2f;
			status = JadeStatus.bloom;
			statusDuration = bloomTime * 1.5f;
			backColor = trailColor = lightColor = hitColor = jadeLight;
			frontColor = Color.white;
			trailLength = 10;
			trailWidth = 1.8f;
			ammoMultiplier = 4f;
			shootEffect = JadeFx.thornShoot;
			hitEffect = despawnEffect = JadeFx.leafBurst;
			smokeEffect = Fx.none;
		}};

		thornSpread = new BasicBulletType(4.2f, 14f){{
			width = 5f;
			height = 12f;
			lifetime = 34f;
			status = JadeStatus.bloom;
			statusDuration = bloomTime;
			backColor = trailColor = lightColor = hitColor = jade;
			frontColor = jadeLight;
			trailLength = 7;
			trailWidth = 1.3f;
			shootEffect = JadeFx.thornShoot;
			hitEffect = despawnEffect = JadeFx.leafBurst;
			smokeEffect = Fx.none;
		}};

		//Air unit weapons
		seed = new BasicBulletType(3.2f, 10f){{
			sprite = "circle-bullet";
			width = height = 7f;
			lifetime = 48f;
			homingPower = 0.05f;
			homingRange = 60f;
			status = JadeStatus.bloom;
			statusDuration = bloomTime;
			backColor = trailColor = lightColor = hitColor = pollen;
			frontColor = Color.white;
			trailLength = 8;
			trailWidth = 1.5f;
			shootEffect = JadeFx.pollenShoot;
			hitEffect = despawnEffect = JadeFx.pollenBurst;
			smokeEffect = Fx.none;
		}};

		//Heals friendly buildings, harms enemies.
		lifeBolt = new BasicBulletType(3.8f, 16f){{
			sprite = "circle-bullet";
			width = height = 8f;
			lifetime = 52f;
			healPercent = 5f;
			collidesTeam = true;
			status = JadeStatus.bloom;
			statusDuration = bloomTime;
			backColor = trailColor = lightColor = hitColor = jadeLight;
			frontColor = Color.white;
			trailLength = 10;
			trailWidth = 2f;
			shootEffect = JadeFx.thornShoot;
			hitEffect = despawnEffect = JadeFx.leafBurst;
			smokeEffect = Fx.none;
		}};

		bloomMissile = new MissileBulletType(3.4f, 22f){{
			width = 8f;
			height = 12f;
			lifetime = 62f;
			homingPower = 0.09f;
			homingRange = 110f;
			splashDamage = 46f;
			splashDamageRadius = 38f;
			status = JadeStatus.bloom;
			statusDuration = bloomTime * 1.5f;
			backColor = trailColor = lightColor = hitColor = jade;
			frontColor = jadeLight;
			trailLength = 14;
			trailWidth = 2f;
			shootEffect = Fx.shootBig;
			hitEffect = despawnEffect = new mindustry.entities.effect.MultiEffect(JadeFx.leafBurst, JadeFx.bloomRing);
		}};

		//Power turret beam
		lanceBeam = new LaserBulletType(95f){{
			length = 200f;
			width = 22f;
			lifetime = 20f;
			colors = new Color[]{jadeDark.cpy().a(0.4f), jade, Color.white};
			status = JadeStatus.bloom;
			statusDuration = bloomTime * 2f;
			lightningSpacing = 30f;
			lightningLength = 3;
			lightningDelay = 1.1f;
			lightningDamage = 20f;
			lightningAngleRand = 40f;
			lightningColor = jadeLight;
			healPercent = 4f;
			collidesTeam = true;
			hitEffect = JadeFx.leafBurstBig;
			shootEffect = JadeFx.thornShoot;
			largeHit = true;
		}};

		//Artillery
		canopyShell = new ArtilleryBulletType(3f, 20f){{
			sprite = "circle-bullet";
			width = height = 15f;
			lifetime = 110f;
			knockback = 0.8f;
			collidesTiles = false;
			collides = false;
			collidesAir = false;
			scaleLife = true;
			splashDamage = 130f;
			splashDamageRadius = 62f;
			status = JadeStatus.bloom;
			statusDuration = bloomTime * 2f;
			backColor = trailColor = lightColor = hitColor = jade;
			frontColor = jadeLight;
			trailLength = 12;
			trailWidth = 2.4f;
			ammoMultiplier = 2f;
			hitShake = 2f;
			hitSound = Sounds.explosion;
			shootEffect = Fx.shootBig;
			hitEffect = despawnEffect = new mindustry.entities.effect.MultiEffect(JadeFx.leafBurstBig, JadeFx.bloomRingBig);
			fragBullets = 5;
			fragBullet = thornSpread;
		}};

		canopyShellCore = new ArtilleryBulletType(3.2f, 20f){{
			sprite = "circle-bullet";
			width = height = 18f;
			lifetime = 115f;
			knockback = 1f;
			collidesTiles = false;
			collides = false;
			collidesAir = false;
			scaleLife = true;
			splashDamage = 230f;
			splashDamageRadius = 85f;
			reloadMultiplier = 0.9f;
			status = JadeStatus.bloom;
			statusDuration = bloomTime * 3f;
			backColor = trailColor = lightColor = hitColor = pollen;
			frontColor = Color.white;
			trailLength = 16;
			trailWidth = 3f;
			ammoMultiplier = 1f;
			hitShake = 3.5f;
			hitSound = Sounds.largeExplosion;
			shootEffect = Fx.shootBig2;
			hitEffect = despawnEffect = new mindustry.entities.effect.MultiEffect(JadeFx.pollenBurst, JadeFx.bloomRingBig);
			fragBullets = 8;
			fragBullet = thornShard;
			lightning = 4;
			lightningLength = 6;
			lightningDamage = 28f;
			lightningColor = pollen;
		}};

		//Unit artillery
		briarMortar = new ArtilleryBulletType(2.8f, 20f){{
			sprite = "circle-bullet";
			width = height = 12f;
			lifetime = 95f;
			collidesTiles = false;
			collides = false;
			collidesAir = false;
			scaleLife = true;
			splashDamage = 90f;
			splashDamageRadius = 50f;
			status = JadeStatus.bloom;
			statusDuration = bloomTime * 2f;
			backColor = trailColor = lightColor = hitColor = jade;
			frontColor = jadeLight;
			trailLength = 10;
			trailWidth = 2f;
			hitShake = 1.5f;
			shootEffect = Fx.shootBig;
			hitEffect = despawnEffect = new mindustry.entities.effect.MultiEffect(JadeFx.leafBurstBig, JadeFx.bloomRing);
			fragBullets = 3;
			fragBullet = thornSpread;
		}};
	}
}
