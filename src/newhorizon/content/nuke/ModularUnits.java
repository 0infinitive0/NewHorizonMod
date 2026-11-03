package newhorizon.content.nuke;

import arc.func.Cons;
import arc.graphics.Color;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.abilities.Ability;
import mindustry.entities.abilities.ForceFieldAbility;
import mindustry.entities.abilities.StatusFieldAbility;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.bullet.LaserBulletType;
import mindustry.entities.bullet.MissileBulletType;
import mindustry.gen.LegsUnit;
import arc.audio.Sound;
import mindustry.gen.Sounds;
import mindustry.gen.UnitEntity;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.StatusEffect;
import mindustry.type.Weapon;
import newhorizon.NewHorizon;
import newhorizon.expand.units.crab.AreaHealAbility;
import newhorizon.expand.units.modular.ModularAbility;
import newhorizon.expand.units.modular.ModulePart;
import newhorizon.expand.units.modular.ModulePart.Kind;
import newhorizon.expand.units.unitType.NHUnitType;

/**
 * The modular units: the Chimera, a big walker with two arm slots, a core slot and a hull slot, and nine small pods that carry what goes
 * in them. Docking pods into a frame (at a Refit Bay) is how a frame gets its guns, its aura and its stats, and taking a pod out again
 * swaps the part. A pod flies and fights by itself with a small version of what it carries.
 *
 * <ul>
 * <li>Arms: Autocannon (fast piercing shots), Plasma Lance (a long beam through everything), Missile Rack (homing salvo).</li>
 * <li>Core: Shield Dome (a force field), Repair Field (mends allies round it), Overdrive Field (speeds up allies round it).</li>
 * <li>Hull: Plating (tougher, slower), Servos (faster, quicker reload), Overcharge (hits harder, less tough).</li>
 * </ul>
 */
public class ModularUnits{
	public static NHUnitType frame;
	public static ModulePart autocannon, lance, missile, shield, repair, overdrive, plating, servos, overcharge;
	public static StatusEffect platingEffect, servosEffect, overchargeEffect;

	/** How far to each side the arms sit. */
	public static final float armX = 20f;

	public static void load(){
		Color amber = Color.valueOf("ffb347"), cyan = Color.valueOf("5ad7ff"), rose = Color.valueOf("ff6f91");
		Color mint = Color.valueOf("7dffb0"), gold = Color.valueOf("ffe066"), violet = Color.valueOf("b58cff");
		Color steel = Color.valueOf("9fb0c8"), lime = Color.valueOf("c6ff5a"), red = Color.valueOf("ff5d4a");

		platingEffect = new StatusEffect("module-plating"){{
			color = steel;
			permanent = true;
			show = false;
			healthMultiplier = 1.6f;
			speedMultiplier = 0.85f;
		}};
		servosEffect = new StatusEffect("module-servos"){{
			color = lime;
			permanent = true;
			show = false;
			speedMultiplier = 1.4f;
			reloadMultiplier = 1.15f;
		}};
		overchargeEffect = new StatusEffect("module-overcharge"){{
			color = red;
			permanent = true;
			show = false;
			damageMultiplier = 1.4f;
			healthMultiplier = 0.85f;
		}};

		//---------------------------------------------------------------- arms
		BulletType autoBullet = new BasicBulletType(11f, 48f){{
			lifetime = 24f;
			width = 7f;
			height = 18f;
			pierce = true;
			pierceCap = 2;
			frontColor = Color.white;
			backColor = trailColor = hitColor = amber;
			trailWidth = 2f;
			trailLength = 8;
			hitEffect = despawnEffect = Fx.hitBulletColor;
		}};
		BulletType lanceBullet = new LaserBulletType(380f){{
			length = 330f;
			width = 30f;
			colors = new Color[]{amber.cpy().a(0.4f), amber, Color.white};
			hitColor = amber;
			hitEffect = Fx.hitLancer;
			shootEffect = Fx.lancerLaserShoot;
		}};
		BulletType missileBullet = new MissileBulletType(5.2f, 42f){{
			lifetime = 52f;
			width = 8f;
			height = 12f;
			splashDamage = 78f;
			splashDamageRadius = 44f;
			homingPower = 0.12f;
			homingRange = 160f;
			trailColor = backColor = hitColor = rose;
			trailLength = 12;
			hitEffect = despawnEffect = Fx.flakExplosion;
		}};

		autocannon = new ModulePart("autocannon", Kind.arm, amber);
		autocannon.left = arm("chimera-autocannon", true, 7f, Sounds.shootDuo, autoBullet, w -> {
			w.inaccuracy = 2f;
			w.recoil = 2f;
		});
		autocannon.right = arm("chimera-autocannon", false, 7f, Sounds.shootDuo, autoBullet, w -> {
			w.inaccuracy = 2f;
			w.recoil = 2f;
		});
		lance = new ModulePart("lance", Kind.arm, cyan);
		lance.left = arm("chimera-lance", true, 80f, Sounds.shootLancer, lanceBullet, w -> w.recoil = 5f);
		lance.right = arm("chimera-lance", false, 80f, Sounds.shootLancer, lanceBullet, w -> w.recoil = 5f);
		missile = new ModulePart("missile", Kind.arm, rose);
		missile.left = arm("chimera-missile", true, 60f, Sounds.shootMissile, missileBullet, w -> {
			w.shoot.shots = 4;
			w.shoot.shotDelay = 5f;
			w.inaccuracy = 4f;
			w.velocityRnd = 0.1f;
		});
		missile.right = arm("chimera-missile", false, 60f, Sounds.shootMissile, missileBullet, w -> {
			w.shoot.shots = 4;
			w.shoot.shotDelay = 5f;
			w.inaccuracy = 4f;
			w.velocityRnd = 0.1f;
		});

		//---------------------------------------------------------------- core
		shield = new ModulePart("shield", Kind.core, mint);
		shield.ability = () -> new ForceFieldAbility(105f, 3f, 8000f, 60f * 10f, 6, 0f);
		repair = new ModulePart("repair", Kind.core, gold);
		repair.ability = () -> heal(240f, 0.04f);
		overdrive = new ModulePart("overdrive", Kind.core, violet);
		overdrive.ability = () -> new StatusFieldAbility(StatusEffects.overdrive, 60f * 6f, 60f * 5f, 220f);

		//---------------------------------------------------------------- hull
		plating = new ModulePart("plating", Kind.hull, steel);
		plating.effect = platingEffect;
		servos = new ModulePart("servos", Kind.hull, lime);
		servos.effect = servosEffect;
		overcharge = new ModulePart("overcharge", Kind.hull, red);
		overcharge.effect = overchargeEffect;

		//---------------------------------------------------------------- pods
		BulletType podGun = new BasicBulletType(7f, 14f){{
			lifetime = 30f;
			width = 5f;
			height = 10f;
			frontColor = Color.white;
			backColor = trailColor = hitColor = steel;
			hitEffect = despawnEffect = Fx.hitBulletColor;
		}};
		autocannon.pod = pod(autocannon, 480f, small(7f, Sounds.shootDuo, new BasicBulletType(8.5f, 14f){{
			lifetime = 28f;
			width = 5f;
			height = 12f;
			pierce = true;
			frontColor = Color.white;
			backColor = trailColor = hitColor = amber;
			hitEffect = despawnEffect = Fx.hitBulletColor;
		}}), null);
		lance.pod = pod(lance, 520f, small(70f, Sounds.shootLancer, new LaserBulletType(90f){{
			length = 150f;
			width = 14f;
			colors = new Color[]{cyan.cpy().a(0.4f), cyan, Color.white};
			hitColor = cyan;
		}}), null);
		missile.pod = pod(missile, 500f, small(50f, Sounds.shootMissileSmall, new MissileBulletType(4.4f, 20f){{
			lifetime = 50f;
			width = 6f;
			height = 9f;
			splashDamage = 28f;
			splashDamageRadius = 28f;
			homingPower = 0.1f;
			trailColor = backColor = hitColor = rose;
			hitEffect = despawnEffect = Fx.flakExplosion;
		}}), null);
		shield.pod = pod(shield, 560f, small(40f, Sounds.shootDuo, podGun), () -> new ForceFieldAbility(46f, 1.2f, 1800f, 60f * 8f, 6, 0f));
		repair.pod = pod(repair, 520f, small(40f, Sounds.shootDuo, podGun), () -> heal(130f, 0.02f));
		overdrive.pod = pod(overdrive, 520f, small(40f, Sounds.shootDuo, podGun), () -> new StatusFieldAbility(StatusEffects.overdrive, 60f * 4f, 60f * 4f, 110f));
		plating.pod = pod(plating, 900f, small(40f, Sounds.shootDuo, podGun), null);
		servos.pod = pod(servos, 440f, small(24f, Sounds.shootDuo, podGun), null);
		overcharge.pod = pod(overcharge, 460f, small(30f, Sounds.shootDuo, podGun), null);
		servos.pod.speed = 3.6f;
		plating.pod.armor = 8f;

		//---------------------------------------------------------------- the frame
		frame = new NHUnitType(NewHorizon.name("chimera")){{
			constructor = LegsUnit::create;
			outlineColor = Color.valueOf("1b1b22");
			drawCell = false;
			//It enters a Refit Bay as a payload.
			allowedInPayloads = true;
			hitSize = 30f;
			health = 16000f;
			armor = 22f;
			speed = 0.55f;
			drag = 0.12f;
			accel = 0.2f;
			rotateSpeed = 2.2f;
			faceTarget = true;

			//Legged: it walks over land, deep water and blocks alike.
			hovering = true;
			canDrown = false;
			allowLegStep = true;
			legCount = 4;
			legGroupSize = 2;
			legPairOffset = 3f;
			legMoveSpace = 1.1f;
			legLength = 44f;
			legExtension = -14f;
			legBaseOffset = 9f;
			legLengthScl = 0.94f;
			legForwardScl = 0.9f;
			legSpeed = 0.14f;
			legSplashDamage = 60f;
			legSplashRange = 36f;
			lockLegBase = true;
			legContinuousMove = true;
			stepShake = 1.2f;
			rippleScale = 2f;
			shadowElevation = 0.4f;
			groundLayer = Layer.legUnit;

			abilities.add(new ModularAbility());

			//Every arm weapon, left and right, is listed so that its sprite is loaded; the ability mounts only the ones the arms carry.
			for(ModulePart part : new ModulePart[]{autocannon, lance, missile}){
				weapons.add(part.left, part.right);
			}
		}};
	}

	private static AreaHealAbility heal(float range, float percent){
		return new AreaHealAbility(){{
			this.range = range;
			this.reload = 60f;
			this.percent = percent;
			this.color = Pal.heal;
		}};
	}

	/** One arm gun, on the left or the right of the frame. */
	private static Weapon arm(String sprite, boolean left, float reload, Sound sound, BulletType bullet, Cons<Weapon> extra){
		Weapon w = new Weapon(NewHorizon.name(sprite)){{
			x = left ? -armX : armX;
			y = 4f;
			mirror = false;
			alternate = false;
			flipSprite = left;
			rotate = true;
			rotateSpeed = 10f;
			shootY = 14f;
			recoilTime = 14f;
			ejectEffect = Fx.none;
		}};
		w.reload = reload;
		w.shootSound = sound;
		w.bullet = bullet;
		extra.get(w);
		return w;
	}

	/** A pod's gun: a small unnamed weapon. */
	private static Weapon small(float reload, Sound sound, BulletType bullet){
		Weapon w = new Weapon();
		w.reload = reload;
		w.x = 0f;
		w.y = 2f;
		w.mirror = false;
		w.shootSound = sound;
		w.bullet = bullet;
		w.ejectEffect = Fx.none;
		return w;
	}

	/** A pod: a small flier that carries a part. */
	private static NHUnitType pod(ModulePart part, float health, Weapon gun, arc.func.Prov<Ability> ability){
		return new NHUnitType(NewHorizon.name("chimera-pod-" + part.name)){{
			constructor = UnitEntity::create;
			outlineColor = Color.valueOf("1b1b22");
			flying = true;
			lowAltitude = true;
			allowedInPayloads = true;
			hitSize = 10f;
			this.health = health;
			armor = 3f;
			speed = 2.8f;
			accel = 0.1f;
			drag = 0.06f;
			rotateSpeed = 9f;
			engineOffset = 5f;
			engineSize = 2.2f;
			itemCapacity = 0;
			weapons.add(gun);
			if(ability != null) abilities.add(ability.get());
		}};
	}
}
