package newhorizon.content.ricochet;

import arc.graphics.Color;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.StatusEffects;
import mindustry.entities.part.RegionPart;
import mindustry.entities.pattern.ShootAlternate;
import mindustry.gen.Sounds;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.type.Item;
import mindustry.type.StatusEffect;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.draw.DrawTurret;
import mindustry.entities.part.DrawPart.PartProgress;
import newhorizon.content.NHStatusEffects;
import newhorizon.expand.bullets.ricochet.AntiHeal;
import newhorizon.expand.bullets.ricochet.RicochetBulletType;

import static mindustry.type.ItemStack.with;

/**
 * The Ricochet: a turret whose bolt turns into lightning arcs on its first hit. The arcs jump from enemy to enemy (units and buildings,
 * and back to ones they already hit when nothing fresh is left), and how many times they jump, and what they do to everything they touch,
 * depends on the ammo.
 */
public class RicochetBlocks{
	public static final Color teal = Color.valueOf("4de3d0"), magenta = Color.valueOf("e04fb5");

	public static StatusEffect antiHeal;
	public static ItemTurret ricochet;
	public static RicochetBulletType copperBolt, graphiteBolt, blastBolt, thoriumBolt, pyratiteBolt, plastaniumBolt, siliconBolt, surgeBolt;

	/** Ticks the Plastanium ammo keeps a target from healing. */
	public static final float antiHealTime = 60f * 8f;

	public static void load(){
		antiHeal = new StatusEffect("anti-heal"){{
			color = Color.valueOf("a577ff");
			effect = Fx.sapped;
			effectChance = 0.08f;
		}};
		AntiHeal.status = antiHeal;
		AntiHeal.load();

		copperBolt = bolt(10, Color.valueOf("f5a45d"), 12f, StatusEffects.slow);

		graphiteBolt = bolt(12, Color.valueOf("c8d0e0"), 17f, StatusEffects.sapped);

		blastBolt = bolt(11, Color.valueOf("ff6b52"), 13f, NHStatusEffects.weak);
		blastBolt.splashDamage = 16f;
		blastBolt.splashDamageRadius = 30f;

		thoriumBolt = bolt(13, Color.valueOf("f9a3c7"), 23f, StatusEffects.melting);
		thoriumBolt.pierceArmor = true;

		pyratiteBolt = bolt(14, Color.valueOf("ffaa5f"), 12f, StatusEffects.burning);
		pyratiteBolt.splashDamage = 8f;
		pyratiteBolt.splashDamageRadius = 22f;
		pyratiteBolt.incendChance = 0.5f;
		pyratiteBolt.incendAmount = 1;
		pyratiteBolt.incendSpread = 14f;

		plastaniumBolt = bolt(15, Color.valueOf("a577ff"), 14f, null);
		plastaniumBolt.antiHeal = antiHealTime;

		siliconBolt = bolt(16, Color.valueOf("86f0b4"), 13f, StatusEffects.electrified);
		siliconBolt.homingPower = 0.08f;
		siliconBolt.homingRange = 70f;

		surgeBolt = bolt(24, Color.valueOf("ffe66a"), 18f, NHStatusEffects.emp2);
		surgeBolt.lightning = 2;
		surgeBolt.lightningLength = 6;
		surgeBolt.lightningDamage = 12f;
		surgeBolt.lightningColor = Color.valueOf("ffe66a");

		ricochet = new ItemTurret("ricochet"){{
			requirements(Category.turret, with(Items.titanium, 120, Items.silicon, 100, Items.graphite, 80, Items.plastanium, 40));
			size = 3;
			health = 1500;
			range = 220f;
			reload = 26f;
			rotateSpeed = 6f;
			recoil = 2.5f;
			inaccuracy = 1.5f;
			maxAmmo = 40;
			ammoPerShot = 1;
			targetAir = true;
			targetGround = true;
			shootY = 10f;
			shootSound = Sounds.shootArc;
			coolant = consumeCoolant(0.2f);
			outlineColor = Pal.darkOutline;

			shoot = new ShootAlternate(){{
				spread = 7.5f;
			}};

			drawer = new DrawTurret(){{
				parts.add(new RegionPart("-barrel"){{
					mirror = true;
					under = true;
					moveY = -3f;
					progress = PartProgress.recoil;
					heatColor = magenta.cpy().a(0.8f);
				}});
			}};

			ammo(
				Items.copper, copperBolt,
				Items.graphite, graphiteBolt,
				Items.blastCompound, blastBolt,
				Items.thorium, thoriumBolt,
				Items.pyratite, pyratiteBolt,
				Items.plastanium, plastaniumBolt,
				Items.silicon, siliconBolt,
				Items.surgeAlloy, surgeBolt
			);
		}};
	}

	/** One ammo: a fast bolt that starts {@code n} arcs on its first hit. */
	private static RicochetBulletType bolt(int n, Color c, float dmg, StatusEffect st){
		return new RicochetBulletType(8f, dmg){{
			maxBounces = n;
			bounceRange = 90f;
			hopDelay = 3f;
			arcColor = c;
			arcStatus = st;
			arcStatusDuration = 60f * 3f;
			status = st == null ? StatusEffects.none : st;
			statusDuration = 60f * 3f;
			lifetime = 31f;
			width = 7f;
			height = 11f;
			backColor = trailColor = lightColor = hitColor = c;
			frontColor = Color.white;
			trailLength = 8;
			trailWidth = 1.6f;
			hitEffect = Fx.hitBulletColor;
			shootEffect = Fx.shootSmall;
			smokeEffect = Fx.shootSmallSmoke;
			ammoMultiplier = 4f;
		}};
	}
}
