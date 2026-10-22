package newhorizon.content.nuke;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.ai.UnitCommand;
import mindustry.content.Fx;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.content.StatusEffects;
import mindustry.entities.Effect;
import mindustry.entities.bullet.ContinuousLaserBulletType;
import mindustry.entities.pattern.ShootAlternate;
import mindustry.gen.Sounds;
import mindustry.gen.UnitEntity;
import newhorizon.NewHorizon;
import newhorizon.expand.units.BoostAbility;
import newhorizon.expand.units.serpent.OmniArmorAbility;
import newhorizon.expand.units.serpent.PassiveHealAbility;
import newhorizon.expand.units.serpent.SerpentChain;
import newhorizon.expand.units.serpent.SerpentCommandAI;
import newhorizon.expand.units.serpent.SerpentFlyingAI;
import newhorizon.expand.units.serpent.SerpentGrowAI;
import newhorizon.expand.units.serpent.SerpentGuardAbility;
import newhorizon.expand.units.serpent.SerpentHeadType;
import newhorizon.expand.units.serpent.SerpentSegmentAI;
import newhorizon.expand.units.serpent.SideWeapon;
import newhorizon.expand.units.unitType.NHUnitType;

/**
 * The Serpent: a flying head with a laser cannon and a body of sections that grow, a hundred omni-matter at a time, from its tail. Head
 * and sections are units of their own. A team can have only one; the head is made in the Serpent Nest, and while it has any sections
 * nothing can hurt it or aim at it.
 */
public class SerpentUnits{
	public static SerpentHeadType head;
	public static NHUnitType body;
	public static UnitCommand growCommand;
	public static mindustry.ai.UnitStance spreadStance;

	public static void load(){
		growCommand = new UnitCommand("nh-serpent-grow", "add", u -> new SerpentGrowAI());
		growCommand.switchToMove = false;
		spreadStance = new newhorizon.expand.units.serpent.SerpentSpreadStance();
		newhorizon.expand.units.serpent.SerpentSpreadSystem.hook();

		Color purple = Color.valueOf("9c3fe8"), light = Color.valueOf("c885ff");

		body = new NHUnitType("serpent-body"){{
			constructor = UnitEntity::create;
			outlineColor = Color.valueOf("1b1b22");
			flying = true;
			lowAltitude = true;
			drawCell = false;
			//A section is driven by the section in front of it, and cannot be commanded or carried.
			playerControllable = false;
			allowedInPayloads = false;
			physics = false;
			aiController = SerpentSegmentAI::new;
			commands = new Seq<>();
			defaultCommand = null;
			hitSize = SerpentChain.bodyWidth;
			//The health of an omni wall, which it has the other properties of too (see OmniArmorAbility).
			health = 5000f;
			armor = 8f;
			speed = 1.15f;
			accel = 0.3f;
			drag = 0.12f;
			rotateSpeed = 20f;
			engineOffset = 0f;
			engineSize = 0f;

			abilities.add(new PassiveHealAbility(0.015f));
			abilities.add(new OmniArmorAbility());

			//One twin-barrel cannon on each side, on the pad at the end of the oval. It turns a little but can only fire to its own side;
			//its two barrels fire in turn.
			weapons.add(new SideWeapon(NewHorizon.name("serpent-cannon")){{
				x = 14.25f;
				y = 0f;
				shootY = 5f;
				mirror = true;
				alternate = false;
				rotate = true;
				rotateSpeed = 8f;
				baseRotation = -90f;
				rotationLimit = 80f;
				shootCone = 12f;
				reload = 9f;
				shoot = new ShootAlternate(3f);
				autoTarget = true;
				controllable = false;
				aiControllable = false;
				ejectEffect = Fx.none;
				shootSound = Sounds.shootMissileLarge;
				bullet = new BasicBulletType(7f, 60f){{
					lifetime = 42f;
					//The section is moving along its ring: a shot that kept its speed would drift sideways off a target at the middle of it.
					keepVelocity = false;
					width = 8f;
					height = 12f;
					backColor = purple;
					frontColor = light;
					trailColor = purple;
					trailWidth = 2f;
					trailLength = 8;
					hitColor = light;
				}};
			}});
		}};

		head = new SerpentHeadType("serpent"){{
			constructor = UnitEntity::create;
			outlineColor = Color.valueOf("1b1b22");
			flying = true;
			drawCell = false;
			allowedInPayloads = false;
			faceTarget = true;
			hitSize = SerpentChain.headWidth;
			health = 6000f;
			armor = 12f;
			speed = 1.1f;
			accel = 0.06f;
			drag = 0.05f;
			rotateSpeed = 2.4f;
			//No engine glow behind it.
			engineSize = 0f;
			engineOffset = 0f;
			//When it attacks it goes round the target in a ring, so that the body, which follows it round, has its sides to the target
			//(see SerpentOrbit); it can boost to double speed on a steady run.
			circleTarget = true;
			circleTargetRadius = 170f;
			aiController = SerpentFlyingAI::new;
			controller = u -> !u.type.playerControllable || (u.team.isAI() && !u.team.rules().rtsAi) ? new SerpentFlyingAI() : new SerpentCommandAI();
			commands = Seq.with(UnitCommand.moveCommand, growCommand);
			//The command panel only shows a row of stance buttons when there is more than one, so the usual ones are kept beside it.
			stances = Seq.with(mindustry.ai.UnitStance.stop, mindustry.ai.UnitStance.holdFire, mindustry.ai.UnitStance.pursueTarget, mindustry.ai.UnitStance.patrol, spreadStance);
			defaultCommand = UnitCommand.moveCommand;

			abilities.add(new PassiveHealAbility(0.015f));
			abilities.add(new SerpentGuardAbility());
			abilities.add(new BoostAbility(2f, 15f));
			abilities.add(new newhorizon.expand.units.serpent.SerpentSpreadAbility(spreadStance));

			//One laser cannon in the middle, straight ahead: a beam like the Meltdown's, which charges, then burns for a few seconds while the
			//head is slowed.
			Effect charge = new Effect(40f, 100f, e -> {
				arc.graphics.g2d.Draw.color(light, Color.white, e.fin());
				arc.graphics.g2d.Lines.stroke(e.fin() * 2.5f);
				arc.graphics.g2d.Lines.circle(e.x, e.y, 4f + e.fout() * 22f);
				arc.graphics.g2d.Fill.circle(e.x, e.y, e.fin() * 5f);
			}).followParent(true).rotWithParent(true);

			weapons.add(new mindustry.type.Weapon(NewHorizon.name("serpent-laser")){{
				x = 0f;
				y = 9f;
				shootY = 4f;
				mirror = false;
				rotate = false;
				continuous = true;
				reload = 200f;
				cooldownTime = 200f;
				shootCone = 6f;
				recoil = 2f;
				parentizeEffects = true;
				ejectEffect = Fx.none;
				chargeSound = Sounds.chargeVela;
				shootSound = Sounds.beamMeltdown;
				bullet = new ContinuousLaserBulletType(){{
					damage = 70f;
					length = 260f;
					lifetime = 160f;
					hitEffect = Fx.hitMeltdown;
					hitColor = light;
					drawSize = 420f;
					shake = 1f;
					despawnEffect = Fx.smokeCloud;
					smokeEffect = Fx.none;
					chargeEffect = charge;
					//It saps what it burns: slower, and it takes more damage.
					status = StatusEffects.sapped;
					statusDuration = 120f;
					incendChance = 0f;
					incendAmount = 0;
					colors = new Color[]{purple.cpy().a(0.35f), purple.cpy().a(0.7f), light, Color.white};
				}};
				shoot.firstShotDelay = 40f;
				shootStatus = StatusEffects.slow;
				shootStatusDuration = bullet.lifetime + shoot.firstShotDelay;
			}});
		}};

		SerpentChain.head = head;
		SerpentChain.body = body;
	}
}
