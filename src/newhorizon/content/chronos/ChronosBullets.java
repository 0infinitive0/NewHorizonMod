package newhorizon.content.chronos;

import arc.graphics.Color;
import mindustry.content.Fx;
import mindustry.entities.bullet.ArtilleryBulletType;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.BombBulletType;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Sounds;
import newhorizon.expand.bullets.chronos.BlackHoleBulletType;
import newhorizon.expand.bullets.chronos.ChainFreezeBulletType;
import newhorizon.expand.bullets.chronos.DisruptorBulletType;

import static newhorizon.content.chronos.ChronosColor.*;

public class ChronosBullets{
	public static BulletType
		siphonPulse, arcBolt, dissonanceShot, dissonanceShotSmall,
		nanoBlast,
		turretHole, bombHole, blackHoleShell, blackHoleBomb;

	public static void load(){
		siphonPulse = new BasicBulletType(4.2f, 11f){{
			width = 5f;
			height = 9f;
			lifetime = 40f;
			backColor = trailColor = lightColor = hitColor = chrono;
			frontColor = Color.white;
			trailLength = 5;
			trailWidth = 1.1f;
			status = ChronosStatus.temporalFreeze;
			statusDuration = 60f;
			shootEffect = ChronosFx.sparkBurst;
			hitEffect = despawnEffect = ChronosFx.sparkBurst;
			smokeEffect = Fx.none;
		}};

		//Hits the first enemy unit and arcs on from it to the next ones, freezing every one it touches.
		arcBolt = new ChainFreezeBulletType(9f, 24f){{
			width = 6f;
			height = 15f;
			lifetime = 24f;
			jumps = 4;
			jumpRange = 88f;
			falloff = 0.82f;
			chainColor = chronoLight;
			freeze = ChronosStatus.temporalFreeze;
			status = ChronosStatus.temporalFreeze;
			statusDuration = 60f * 4f;
			freezeTime = 60f * 4f;
			backColor = trailColor = lightColor = hitColor = chrono;
			frontColor = Color.white;
			trailLength = 8;
			trailWidth = 1.6f;
			shootEffect = ChronosFx.sparkBurst;
			hitEffect = despawnEffect = ChronosFx.freezeBurst;
			smokeEffect = Fx.none;
			hitSound = Sounds.shootArc;
		}};

		dissonanceShot = new DisruptorBulletType(6.5f, 20f){{
			width = 7f;
			height = 16f;
			lifetime = 50f;
			disableTime = 60f * 6f;
			status = ChronosStatus.dissonance;
			statusDuration = 60f * 2f;
			backColor = trailColor = lightColor = hitColor = violet;
			frontColor = Color.white;
			trailLength = 9;
			trailWidth = 1.8f;
			shootEffect = ChronosFx.disabled;
			hitEffect = despawnEffect = ChronosFx.sparkBurst;
			smokeEffect = Fx.none;
			hitSound = Sounds.shockBullet;
		}};

		dissonanceShotSmall = new DisruptorBulletType(6f, 9f){{
			width = 5f;
			height = 11f;
			lifetime = 45f;
			disableTime = 60f * 3f;
			status = ChronosStatus.dissonance;
			statusDuration = 60f;
			backColor = trailColor = lightColor = hitColor = violet;
			frontColor = Color.white;
			trailLength = 6;
			trailWidth = 1.2f;
			shootEffect = ChronosFx.disabled;
			hitEffect = despawnEffect = ChronosFx.disabled;
			smokeEffect = Fx.none;
		}};

		//A Nanobot's last act: the burst of cold freezes every enemy around it. It is harmless to structures.
		nanoBlast = new BulletType(){{
			speed = 0f;
			rangeOverride = 30f;
			instantDisappear = true;
			hittable = false;
			collides = false;
			collidesTiles = false;
			collidesAir = true;
			splashDamageRadius = 58f;
			splashDamage = 26f;
			buildingDamageMultiplier = 0f;
			status = ChronosStatus.temporalFreeze;
			statusDuration = 60f * 6f;
			lightColor = hitColor = chrono;
			hitSound = Sounds.explosion;
			hitShake = 0.5f;
			hitEffect = new mindustry.entities.effect.MultiEffect(ChronosFx.sparkBurstBig, ChronosFx.freezeBurstBig);
			despawnEffect = Fx.none;
		}};

		turretHole = new BlackHoleBulletType(){{
			radius = 125f;
			lifetime = 60f * 6f;
			coreDps = 340f;
			outerDps = 32f;
			pullCenter = 0.5f;
		}};

		bombHole = new BlackHoleBulletType(){{
			radius = 95f;
			lifetime = 60f * 4.5f;
			coreDps = 230f;
			outerDps = 22f;
		}};

		//Lobbed by the Event Horizon: the shell flies to the target and the hole opens where it lands.
		blackHoleShell = new ArtilleryBulletType(3.4f, 0f){{
			width = 14f;
			height = 14f;
			lifetime = 120f;
			backColor = trailColor = lightColor = hitColor = violet;
			frontColor = hole;
			despawnHit = true;
			fragBullet = turretHole;
			fragBullets = 1;
			fragRandomSpread = 0f;
			fragVelocityMin = fragVelocityMax = 1f;
			hitSound = Sounds.explosion;
			hitEffect = ChronosFx.freezeBurstBig;
			trailEffect = ChronosFx.sparkBurst;
			trailChance = 0.4f;
		}};

		//Dropped by the Singularity.
		blackHoleBomb = new BombBulletType(0f, 0f){{
			width = 16f;
			height = 20f;
			lifetime = 40f;
			backColor = lightColor = hitColor = violet;
			frontColor = hole;
			despawnHit = true;
			fragBullet = bombHole;
			fragBullets = 1;
			fragRandomSpread = 0f;
			fragVelocityMin = fragVelocityMax = 1f;
			hitSound = Sounds.explosion;
			hitEffect = ChronosFx.freezeBurstBig;
			collidesAir = false;
			collidesGround = true;
		}};
	}
}
