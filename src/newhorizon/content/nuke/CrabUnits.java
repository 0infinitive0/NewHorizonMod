package newhorizon.content.nuke;

import arc.graphics.Color;
import mindustry.content.Fx;
import mindustry.entities.abilities.ForceFieldAbility;
import mindustry.entities.abilities.ShieldRegenFieldAbility;
import mindustry.entities.bullet.ShrapnelBulletType;
import mindustry.gen.LegsUnit;
import mindustry.gen.Sounds;
import mindustry.graphics.Layer;
import mindustry.type.Weapon;
import newhorizon.NewHorizon;
import newhorizon.expand.units.crab.AreaHealAbility;
import newhorizon.expand.units.crab.NuclearDeathAbility;
import newhorizon.expand.units.crab.OnePerTeamAbility;
import newhorizon.expand.units.crab.ReflectAbility;
import newhorizon.expand.units.crab.StoredDamageAbility;
import newhorizon.expand.units.unitType.NHUnitType;

/**
 * The King Crab: one to a team, an armoured legged unit that walks over land and deep water, tears at what is beside it with two claws
 * without a pause, turns bullets back, shields and heals what is round it, stores the damage it takes and lets it out in a blast, and
 * goes nuclear when it dies. It is made in the Crab Dock.
 */
public class CrabUnits{
	public static NHUnitType kingCrab;

	public static void load(){
		Color crimson = Color.valueOf("d8362b"), hot = Color.valueOf("ff8c5a");

		kingCrab = new NHUnitType("king-crab"){{
			constructor = LegsUnit::create;
			outlineColor = Color.valueOf("1b1b22");
			drawCell = false;
			allowedInPayloads = false;
			hitSize = 64f;
			health = 120000f;
			armor = 60f;
			speed = 0.42f;
			drag = 0.12f;
			accel = 0.2f;
			rotateSpeed = 1.6f;
			faceTarget = true;
			//Nothing pushes it.
			knockbackMultiplier = 0f;

			//Legged: it walks over land, deep water and blocks alike, and what its feet come down on is hurt.
			hovering = true;
			canDrown = false;
			allowLegStep = true;
			legCount = 8;
			legGroupSize = 2;
			legPairOffset = 3f;
			legMoveSpace = 1.1f;
			legLength = 70f;
			//The lower piece of a leg is drawn from this far behind the knee (the sign is ignored): a few units, so that it overlaps the knee and not so much that the knee sits in the middle of the bar.
			legExtension = -4f;
			legBaseOffset = 16f;
			legLengthScl = 0.94f;
			legForwardScl = 0.9f;
			legSpeed = 0.14f;
			legSplashDamage = 200f;
			legSplashRange = 60f;
			lockLegBase = true;
			legContinuousMove = true;
			stepShake = 2.2f;
			rippleScale = 3f;
			shadowElevation = 0.5f;
			groundLayer = Layer.legUnit;

			//The reflection comes first, so that the shield below it only has to deal with what cannot be turned back.
			abilities.add(new ReflectAbility(){{
				radius = 200f;
				color = hot;
			}});
			abilities.add(new ForceFieldAbility(200f, 12f, 40000f, 60f * 8f, 6, 0f));
			abilities.add(new ShieldRegenFieldAbility(80f, 4000f, 90f, 260f));
			abilities.add(new AreaHealAbility(){{
				range = 300f;
				reload = 60f;
				percent = 0.06f;
			}});
			abilities.add(new StoredDamageAbility(){{
				maxCharge = 30000f;
				threshold = 1000f;
				triggerRange = 12f * 8f;
				blastRadius = 20f * 8f;
				blastCooldown = 30f;
				multiplier = 2.5f;
				color = hot;
			}});
			abilities.add(new OnePerTeamAbility());
			abilities.add(new NuclearDeathAbility(){{
				radius = 600f;
				damage = 9000f;
				fire = 90;
				color = hot;
			}});

			//Two claws that keep snapping at whatever of the enemy's is in reach: a short, wide slash that goes through everything in it.
			weapons.add(new Weapon(NewHorizon.name("king-crab-claw")){{
				//The claw turns about its shoulder, which is where its sprite is centred and where the shell has a socket for it; the arm lies under the
				//edge of the shell and the jaws are out in front.
				x = 24f;
				y = 7f;
				layerOffset = -0.001f;
				mirror = true;
				alternate = false;
				rotate = true;
				rotateSpeed = 14f;
				rotationLimit = 45f;
				baseRotation = 0f;
				reload = 8f;
				shootCone = 360f;
				recoil = 4f;
				recoilTime = 10f;
				shootY = 36f;
				ejectEffect = Fx.none;
				shootSound = Sounds.shootScathe;
				bullet = new ShrapnelBulletType(){{
					length = 96f;
					width = 28f;
					damage = 700f;
					buildingDamageMultiplier = 1.5f;
					lifetime = 16f;
					serrations = 7;
					serrationLenScl = 6f;
					serrationWidth = 6f;
					fromColor = Color.white;
					toColor = crimson;
					hitColor = hot;
					collidesAir = true;
					collidesGround = true;
					shootEffect = smokeEffect = Fx.none;
				}};
			}});
		}};
	}
}
