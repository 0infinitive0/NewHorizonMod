package newhorizon.content.nuke;

import arc.func.Cons;
import arc.func.Func;
import arc.struct.Seq;
import arc.graphics.Color;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.ai.UnitCommand;
import mindustry.ai.UnitStance;
import mindustry.entities.abilities.Ability;
import mindustry.entities.abilities.ShieldArcAbility;
import mindustry.entities.abilities.ForceFieldAbility;
import mindustry.entities.abilities.StatusFieldAbility;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.ArtilleryBulletType;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.bullet.ContinuousLaserBulletType;
import mindustry.entities.bullet.FlakBulletType;
import mindustry.entities.bullet.LightningBulletType;
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
import mindustry.type.weapons.RepairBeamWeapon;
import newhorizon.expand.bullets.nuke.ConeBulletType;
import newhorizon.expand.bullets.nuke.TractorBulletType;
import newhorizon.NewHorizon;
import newhorizon.expand.units.crab.AreaHealAbility;
import newhorizon.expand.units.modular.AutoBlinkStance;
import newhorizon.expand.units.modular.CloakAbility;
import newhorizon.expand.units.modular.HealSuppressAbility;
import newhorizon.expand.units.modular.HitCapAbility;
import newhorizon.expand.units.modular.LaserBlockAbility;
import newhorizon.expand.units.modular.ModularAbility;
import newhorizon.expand.units.modular.PointDefenseAbility;
import newhorizon.expand.units.modular.TeleportAI;
import newhorizon.expand.units.modular.TeleportAbility;
import newhorizon.expand.units.unitType.ModularUnitType;
import newhorizon.expand.zmodular.ChimeraEntity;
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
	/** The later cores: ids 18 to 26. */
	public static ModulePart blink, jump, cloak, pointdefense, arcshield, hitcap, laserblock, builder, suppress;
	public static UnitCommand blinkCommand, jumpCommand;
	public static UnitStance autoBlink;
	public static StatusEffect noBuildEffect;
	/** The later hulls: ids 27 to 33. */
	public static ModulePart sprint, rapid, emplacement, bulk, glass, comet, bastion, dash;
	public static ModulePart autocannon, lance, missile, shield, repair, overdrive, plating, servos, overcharge;
	/** The later arms: ids 9 to 17. */
	public static ModulePart howitzer, siege, beam, tractor, tesla, pulse, mender, flak, sword;
	public static StatusEffect platingEffect, servosEffect, overchargeEffect;

	/** How far to each side the arms sit. */
	public static final float armX = 20f;

	public static void load(){
		Color amber = Color.valueOf("ffb347"), cyan = Color.valueOf("5ad7ff"), rose = Color.valueOf("ff6f91");
		Color mint = Color.valueOf("7dffb0"), gold = Color.valueOf("ffe066"), violet = Color.valueOf("b58cff");
		Color steel = Color.valueOf("9fb0c8"), lime = Color.valueOf("c6ff5a"), red = Color.valueOf("ff5d4a");
		Color orange = Color.valueOf("ff8a3d"), crimson = Color.valueOf("e8384f"), teal = Color.valueOf("3ee0c1"), sky = Color.valueOf("6cb6ff");
		Color electric = Color.valueOf("e9f26a"), wave = Color.valueOf("a6d8ff"), blade = Color.valueOf("ff5ad1");

		noBuildEffect = new StatusEffect("module-no-build"){{
			color = steel;
			permanent = true;
			show = false;
			buildSpeedMultiplier = 0f;
		}};
		ModularAbility.noBuild = noBuildEffect;
		autoBlink = new AutoBlinkStance();
		blinkCommand = new UnitCommand("nh-blink", "right", u -> new TeleportAI(false));
		jumpCommand = new UnitCommand("nh-jump", "up", u -> new TeleportAI(true));

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

		//The later hulls (the user's numbers): speed, health (hit points: damage taken is divided by it), damage and fire rate here, armor on the part.
		StatusEffect sprintEffect = hullEffect("module-sprint", Color.valueOf("ffd23f"), 5f, 0.2f, 1f, 1f);
		StatusEffect rapidEffect = hullEffect("module-rapid", Color.valueOf("ff9f43"), 0.2f, 1f, 1f, 3f);
		StatusEffect emplacementEffect = hullEffect("module-emplacement", Color.valueOf("c0392b"), 0f, 1f, 1.5f, 7f);
		StatusEffect bulkEffect = hullEffect("module-bulk", Color.valueOf("7f8c8d"), 1f, 2f, 1f, 1f);
		StatusEffect glassEffect = hullEffect("module-glass", Color.valueOf("a29bfe"), 1f, 1f, 3f, 1f);
		StatusEffect cometEffect = hullEffect("module-comet", Color.valueOf("55efc4"), 10f, 0.2f, 1f, 1f);
		StatusEffect bastionEffect = hullEffect("module-bastion", Color.valueOf("0984e3"), 1f, 2f, 0.3f, 1f);
		//15x speed and 5x drag: it gets up to speed at once and stops just as fast.
		StatusEffect dashEffect = hullEffect("module-dash", Color.valueOf("fd79a8"), 15f, 1f, 1f, 1f);
		dashEffect.dragMultiplier = 5f;

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

		//---------------------------------------------------------------- the later arms (their ids come after the hulls', so the first twelve stay as saved)
		howitzer = armPart("howitzer", orange, left -> arm("chimera-howitzer", left, 110f, Sounds.shootArtillery, new ArtilleryBulletType(6f, 0f){{
			lifetime = 94f;
			width = 16f;
			height = 24f;
			splashDamage = 260f;
			splashDamageRadius = 72f;
			hitShake = 3f;
			frontColor = Color.white;
			backColor = hitColor = trailColor = orange;
			hitEffect = despawnEffect = Fx.massiveExplosion;
		}}, w -> {
			w.recoil = 7f;
			w.shootY = 22f;
			w.inaccuracy = 1f;
		}));
		siege = armPart("siege", crimson, left -> arm("chimera-siege", left, 200f, Sounds.shootMissileLarge, new MissileBulletType(4.2f, 70f){{
			lifetime = 152f;
			width = 14f;
			height = 22f;
			splashDamage = 520f;
			splashDamageRadius = 96f;
			homingPower = 0.08f;
			homingRange = 320f;
			hitShake = 5f;
			trailLength = 28;
			trailWidth = 3f;
			trailColor = backColor = hitColor = crimson;
			hitEffect = despawnEffect = Fx.massiveExplosion;
		}}, w -> {
			w.recoil = 6f;
			w.shootY = 18f;
		}));
		beam = armPart("beam", teal, left -> arm("chimera-beam", left, 220f, Sounds.shootMeltdown, new ContinuousLaserBulletType(38f){{
			length = 260f;
			lifetime = 150f;
			damageInterval = 5f;
			width = 9f;
			colors = new Color[]{teal.cpy().a(0.4f), teal, Color.white};
			hitColor = teal;
			shake = 1f;
		}}, w -> {
			w.continuous = true;
			w.shootStatus = StatusEffects.slow;
			w.shootStatusDuration = 150f + 20f;
			w.recoil = 0f;
			w.shootY = 16f;
		}));
		tractor = armPart("tractor", sky, left -> arm("chimera-tractor", left, 70f, Sounds.shootLaser, new TractorBulletType(7f){{
			length = 250f;
			lifetime = 120f;
			color = hitColor = sky;
		}}, w -> {
			w.continuous = true;
			w.recoil = 0f;
			w.shootY = 16f;
		}));
		tesla = armPart("tesla", electric, left -> arm("chimera-tesla", left, 20f, Sounds.shootArc, new LightningBulletType(){{
			damage = 34f;
			lightningLength = 22;
			lightningLengthRand = 8;
			lightningColor = electric;
			status = StatusEffects.shocked;
			statusDuration = 60f;
		}}, w -> {
			w.shoot = new mindustry.entities.pattern.ShootSpread(3, 14f);
			w.recoil = 1f;
			w.shootY = 12f;
		}));
		pulse = armPart("pulse", wave, left -> arm("chimera-pulse", left, 90f, Sounds.shootBreach, new ConeBulletType(){{
			radius = 130f;
			cone = 110f;
			damage = 22f;
			knockback = 18f;
			color = wave;
		}}, w -> {
			w.recoil = 4f;
			w.shootY = 12f;
		}));
		mender = armPart("mender", Color.valueOf("ff9ee8"), left -> new RepairBeamWeapon(NewHorizon.name("chimera-mender")){{
			x = left ? -armX : armX;
			y = 4f;
			mirror = false;
			alternate = false;
			flipSprite = left;
			rotate = true;
			rotateSpeed = 12f;
			shootY = 12f;
			targetBuildings = true;
			targetUnits = true;
			repairSpeed = 1.6f;
			fractionRepairSpeed = 0.0008f;
			beamWidth = 0.8f;
			laserColor = healColor = Color.valueOf("98ffa9");
			bullet = new BulletType(){{
				maxRange = 220f;
			}};
		}});
		flak = armPart("flak", Color.valueOf("e6e6ff"), left -> arm("chimera-flak", left, 14f, Sounds.shootCyclone, new FlakBulletType(4.6f, 3f){{
			lifetime = 62f;
			width = 7f;
			height = 12f;
			splashDamage = 78f;
			splashDamageRadius = 54f;
			explodeRange = 44f;
			collidesGround = false;
			frontColor = Color.white;
			backColor = trailColor = hitColor = gold;
			hitEffect = despawnEffect = Fx.flakExplosion;
		}}, w -> {
			w.inaccuracy = 3f;
			w.recoil = 2f;
			w.shootY = 14f;
		}));
		sword = armPart("sword", blade, left -> arm("chimera-sword", left, 20f, Sounds.shootScathe, new ConeBulletType(){{
			radius = 92f;
			cone = 130f;
			damage = 520f;
			buildingDamageMultiplier = 0.6f;
			color = blade;
		}}, w -> {
			w.recoil = 0f;
			w.rotateSpeed = 22f;
			w.shootCone = 360f;
			w.shootY = 6f;
		}));

		//---------------------------------------------------------------- the later cores (ids 18 to 26, after the arms)
		blink = new ModulePart("blink", Kind.core, Color.valueOf("8ad4ff"));
		blink.ability = () -> new TeleportAbility(){{
			range = 144f;
			cooldown = 60f * 8f;
			color = Color.valueOf("8ad4ff");
		}};
		jump = new ModulePart("jump", Kind.core, Color.valueOf("3f7bff"));
		jump.ability = () -> new TeleportAbility(){{
			longRange = true;
			range = 800f;
			cooldown = 60f * 60f;
			charge = 60f * 2.5f;
			color = Color.valueOf("3f7bff");
		}};
		cloak = new ModulePart("cloak", Kind.core, Color.valueOf("8f9bb3"));
		cloak.ability = CloakAbility::new;
		pointdefense = new ModulePart("pointdefense", Kind.core, Color.valueOf("ffcc66"));
		pointdefense.ability = PointDefenseAbility::new;
		arcshield = new ModulePart("arcshield", Kind.core, Color.valueOf("5ce0d8"));
		arcshield.ability = () -> new ShieldArcAbility(){{
			radius = 90f;
			angle = 130f;
			regen = 4f;
			max = 8000f;
			cooldown = 60f * 6f;
			width = 7f;
			whenShooting = false;
			drawArc = true;
			color = Color.valueOf("5ce0d8");
		}};
		hitcap = new ModulePart("hitcap", Kind.core, Color.valueOf("ff8a8a"));
		hitcap.ability = HitCapAbility::new;
		laserblock = new ModulePart("laserblock", Kind.core, Color.valueOf("e86bff"));
		laserblock.ability = LaserBlockAbility::new;
		builder = new ModulePart("builder", Kind.core, Color.valueOf("ffa94d"));
		builder.builds = true;
		suppress = new ModulePart("suppress", Kind.core, Color.valueOf("ff5f7a"));
		suppress.ability = HealSuppressAbility::new;

		sprint = hullPart("sprint", Color.valueOf("ffd23f"), sprintEffect, 1f);
		rapid = hullPart("rapid", Color.valueOf("ff9f43"), rapidEffect, 1f);
		emplacement = hullPart("emplacement", Color.valueOf("c0392b"), emplacementEffect, 1f);
		bulk = hullPart("bulk", Color.valueOf("7f8c8d"), bulkEffect, 0.5f);
		glass = hullPart("glass", Color.valueOf("a29bfe"), glassEffect, 0f);
		comet = hullPart("comet", Color.valueOf("55efc4"), cometEffect, 0f);
		bastion = hullPart("bastion", Color.valueOf("0984e3"), bastionEffect, 2f);
		dash = hullPart("dash", Color.valueOf("fd79a8"), dashEffect, 1f);

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
		howitzer.pod = pod(howitzer, 520f, small(120f, Sounds.shootArtillery, new ArtilleryBulletType(3.2f, 0f){{
			lifetime = 60f;
			width = 9f;
			height = 13f;
			splashDamage = 56f;
			splashDamageRadius = 38f;
			backColor = hitColor = trailColor = orange;
			hitEffect = despawnEffect = Fx.flakExplosion;
		}}), null);
		siege.pod = pod(siege, 500f, small(160f, Sounds.shootMissileSmall, new MissileBulletType(3.6f, 20f){{
			lifetime = 90f;
			width = 8f;
			height = 12f;
			splashDamage = 120f;
			splashDamageRadius = 52f;
			homingPower = 0.08f;
			trailColor = backColor = hitColor = crimson;
			hitEffect = despawnEffect = Fx.flakExplosion;
		}}), null);
		beam.pod = pod(beam, 500f, contin(small(150f, Sounds.shootMeltdown, new ContinuousLaserBulletType(9f){{
			length = 120f;
			lifetime = 60f;
			damageInterval = 5f;
			width = 4f;
			colors = new Color[]{teal.cpy().a(0.4f), teal, Color.white};
			hitColor = teal;
		}})), null);
		tractor.pod = pod(tractor, 500f, contin(small(70f, Sounds.shootLaser, new TractorBulletType(2f){{
			length = 130f;
			lifetime = 60f;
			width = 8f;
			force = 18f;
			scaledForce = 8f;
			color = hitColor = sky;
		}})), null);
		tesla.pod = pod(tesla, 480f, small(26f, Sounds.shootArc, new LightningBulletType(){{
			damage = 14f;
			lightningLength = 12;
			lightningColor = electric;
		}}), null);
		pulse.pod = pod(pulse, 520f, small(100f, Sounds.shootBreach, new ConeBulletType(){{
			radius = 66f;
			cone = 90f;
			damage = 8f;
			knockback = 7f;
			color = wave;
		}}), null);
		mender.pod = pod(mender, 480f, new RepairBeamWeapon(){{
			x = 0f;
			y = 2f;
			mirror = false;
			rotate = true;
			targetBuildings = true;
			repairSpeed = 0.6f;
			beamWidth = 0.5f;
			laserColor = healColor = Color.valueOf("98ffa9");
			bullet = new BulletType(){{
				maxRange = 110f;
			}};
		}}, null);
		flak.pod = pod(flak, 480f, small(20f, Sounds.shootCyclone, new FlakBulletType(4f, 2f){{
			lifetime = 50f;
			width = 5f;
			height = 9f;
			splashDamage = 26f;
			splashDamageRadius = 30f;
			explodeRange = 28f;
			collidesGround = false;
			backColor = trailColor = hitColor = gold;
			hitEffect = despawnEffect = Fx.flakExplosion;
		}}), null);
		sword.pod = pod(sword, 560f, small(26f, Sounds.shootScathe, new ConeBulletType(){{
			radius = 38f;
			cone = 130f;
			damage = 46f;
			color = blade;
		}}), null);
		blink.pod = pod(blink, 480f, small(40f, Sounds.shootDuo, podGun), null);
		jump.pod = pod(jump, 480f, small(40f, Sounds.shootDuo, podGun), null);
		cloak.pod = pod(cloak, 480f, small(40f, Sounds.shootDuo, podGun), () -> new CloakAbility(){{
			delay = 150f;
		}});
		pointdefense.pod = pod(pointdefense, 480f, small(40f, Sounds.shootDuo, podGun), () -> new PointDefenseAbility(){{
			range = 110f;
			reload = 14f;
			bulletDamage = 30f;
		}});
		arcshield.pod = pod(arcshield, 520f, small(40f, Sounds.shootDuo, podGun), () -> new ShieldArcAbility(){{
			radius = 34f;
			angle = 110f;
			regen = 1.2f;
			max = 1500f;
			cooldown = 60f * 6f;
			width = 4f;
			whenShooting = false;
			drawArc = true;
			color = Color.valueOf("5ce0d8");
		}});
		hitcap.pod = pod(hitcap, 640f, small(40f, Sounds.shootDuo, podGun), null);
		laserblock.pod = pod(laserblock, 520f, small(40f, Sounds.shootDuo, podGun), null);
		builder.pod = pod(builder, 480f, small(40f, Sounds.shootDuo, podGun), null);
		builder.pod.buildSpeed = 0.6f;
		suppress.pod = pod(suppress, 480f, small(40f, Sounds.shootDuo, podGun), () -> new HealSuppressAbility(){{
			range = 100f;
		}});
		sprint.pod = pod(sprint, 420f, small(30f, Sounds.shootDuo, podGun), null);
		sprint.pod.speed = 4.2f;
		rapid.pod = pod(rapid, 480f, small(14f, Sounds.shootDuo, podGun), null);
		emplacement.pod = pod(emplacement, 520f, small(8f, Sounds.shootDuo, podGun), null);
		bulk.pod = pod(bulk, 900f, small(40f, Sounds.shootDuo, podGun), null);
		glass.pod = pod(glass, 380f, small(40f, Sounds.shootDuo, podGun), null);
		comet.pod = pod(comet, 360f, small(30f, Sounds.shootDuo, podGun), null);
		comet.pod.speed = 4.6f;
		bastion.pod = pod(bastion, 1000f, small(60f, Sounds.shootDuo, podGun), null);
		dash.pod = pod(dash, 380f, small(30f, Sounds.shootDuo, podGun), null);
		dash.pod.speed = 4.4f;
		bulk.pod.armor = 2f;
		glass.pod.armor = 0f;
		comet.pod.armor = 0f;
		bastion.pod.armor = 10f;
		servos.pod.speed = 3.6f;
		plating.pod.armor = 8f;

		//---------------------------------------------------------------- the frame
		frame = new ModularUnitType(NewHorizon.name("chimera")){{
			constructor = ChimeraEntity::new;
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
			//One approach distance for whatever the arms carry (a cannon reaches over 500, a sword under 100): the guns each shoot at their own range.
			range = maxRange = 300f;

			//It builds only with the Builder core (a hidden status holds its build speed at nothing otherwise).
			buildSpeed = 1.2f;
			commands = Seq.with(UnitCommand.moveCommand, blinkCommand, jumpCommand);
			//The command panel shows a row of stance buttons only when there is more than one, so the usual ones are kept beside Auto blink.
			stances = Seq.with(UnitStance.stop, UnitStance.holdFire, UnitStance.pursueTarget, UnitStance.patrol, autoBlink);

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
			for(ModulePart part : new ModulePart[]{autocannon, lance, missile, howitzer, siege, beam, tractor, tesla, pulse, mender, flak, sword}){
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

	/** A hull's permanent hidden status: its speed, health (hit points), damage and fire-rate multipliers. */
	private static StatusEffect hullEffect(String name, Color color, float speedMult, float healthMult, float damageMult, float reloadMult){
		return new StatusEffect(name){{
			this.color = color;
			permanent = true;
			show = false;
			//(the parameters have their own names: a status effect has a field called damage, which would shadow one of that name)
			speedMultiplier = speedMult;
			healthMultiplier = healthMult;
			damageMultiplier = damageMult;
			reloadMultiplier = reloadMult;
		}};
	}

	/** A part of the hull kind, with its status and its share of the frame's armor. */
	private static ModulePart hullPart(String name, Color color, StatusEffect effect, float armorScale){
		ModulePart part = new ModulePart(name, Kind.hull, color);
		part.effect = effect;
		part.armorScale = armorScale;
		return part;
	}

	/** A part of the arm kind: a left and a right weapon from the maker. */
	private static ModulePart armPart(String name, Color color, Func<Boolean, Weapon> maker){
		ModulePart part = new ModulePart(name, Kind.arm, color);
		part.left = maker.get(true);
		part.right = maker.get(false);
		return part;
	}

	/** Makes a pod's gun hold its beam. */
	private static Weapon contin(Weapon w){
		w.continuous = true;
		return w;
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
		return new ModularUnitType(NewHorizon.name("chimera-pod-" + part.name)){{
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
