package newhorizon.content.nuke;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.ai.UnitCommand;
import mindustry.content.Fx;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.LaserBulletType;
import mindustry.gen.Sounds;
import mindustry.gen.UnitEntity;
import newhorizon.NewHorizon;
import newhorizon.expand.units.serpent.OmniArmorAbility;
import newhorizon.expand.units.serpent.PassiveHealAbility;
import newhorizon.expand.units.serpent.SerpentChain;
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

	public static void load(){
		growCommand = new UnitCommand("nh-serpent-grow", "add", u -> new SerpentGrowAI());
		growCommand.switchToMove = false;

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
			hitSize = 26f;
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

			//Two cannons on each side, one behind the other. Each turns a little but can only fire to its own side.
			for(float y : new float[]{7f, -7f}){
				weapons.add(new SideWeapon(NewHorizon.name("serpent-cannon")){{
					x = 12f;
					this.y = y;
					mirror = true;
					alternate = false;
					rotate = true;
					rotateSpeed = 8f;
					baseRotation = -90f;
					rotationLimit = 80f;
					shootCone = 12f;
					reload = 18f;
					autoTarget = true;
					controllable = false;
					aiControllable = false;
					ejectEffect = Fx.none;
					shootSound = Sounds.shootMissileLarge;
					bullet = new BasicBulletType(7f, 26f){{
						lifetime = 42f;
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
			}
		}};

		head = new SerpentHeadType("serpent"){{
			constructor = UnitEntity::create;
			outlineColor = Color.valueOf("1b1b22");
			flying = true;
			drawCell = false;
			allowedInPayloads = false;
			faceTarget = true;
			hitSize = 36f;
			health = 6000f;
			armor = 12f;
			speed = 0.9f;
			accel = 0.06f;
			drag = 0.05f;
			rotateSpeed = 2.4f;
			engineOffset = 28f;
			engineSize = 7f;
			engineColor = light;
			commands = Seq.with(UnitCommand.moveCommand, growCommand);
			defaultCommand = UnitCommand.moveCommand;

			abilities.add(new PassiveHealAbility(0.015f));
			abilities.add(new SerpentGuardAbility());

			//One laser cannon in the middle, straight ahead.
			weapons.add(new mindustry.type.Weapon(NewHorizon.name("serpent-laser")){{
				x = 0f;
				y = 10f;
				mirror = false;
				rotate = false;
				reload = 100f;
				shootCone = 6f;
				recoil = 4f;
				ejectEffect = Fx.none;
				shootSound = Sounds.shootMissileLarge;
				bullet = new LaserBulletType(230f){{
					length = 290f;
					width = 26f;
					colors = new Color[]{purple.cpy().a(0.4f), light, Color.white};
					hitColor = light;
					lightColor = light;
					shootEffect = Fx.lancerLaserShoot;
					sideAngle = 25f;
					sideWidth = 1f;
					sideLength = 60f;
				}};
			}});
		}};

		SerpentChain.head = head;
		SerpentChain.body = body;
	}
}
