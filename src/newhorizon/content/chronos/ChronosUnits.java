package newhorizon.content.chronos;

import arc.struct.Seq;
import mindustry.ai.UnitCommand;
import mindustry.content.Fx;
import mindustry.entities.abilities.RepairFieldAbility;
import mindustry.entities.pattern.ShootAlternate;
import mindustry.gen.Sounds;
import mindustry.gen.UnitEntity;
import mindustry.type.Weapon;
import mindustry.world.meta.BlockFlag;
import newhorizon.NewHorizon;
import newhorizon.expand.units.ai.HealerAI;
import newhorizon.expand.units.chronos.GridDrainAbility;
import newhorizon.expand.units.chronos.ReplicateAbility;
import newhorizon.expand.units.unitType.NHUnitType;

/**
 * Chronos units. All of them fly and are built in the Temporal Fabricator.
 * <ul>
 *     <li>{@link #nanobot}: tiny healer that copies itself and freezes everything around it when it dies</li>
 *     <li>{@link #siphon}: drains enemy power grids</li>
 *     <li>{@link #dissonance}: its shots knock turrets offline</li>
 *     <li>{@link #singularity}: bomber that drops black holes</li>
 * </ul>
 */
public class ChronosUnits{
	public static NHUnitType nanobot, siphon, dissonance, singularity;

	public static UnitCommand nanoHealCommand;

	public static void load(){
		nanoHealCommand = new UnitCommand("nh-nano-heal", "add", u -> new HealerAI());

		nanobot = new NHUnitType("nanobot"){{
			constructor = UnitEntity::create;
			aiController = HealerAI::new;
			commands = Seq.with(UnitCommand.moveCommand, nanoHealCommand);
			defaultCommand = nanoHealCommand;
			outlineColor = ChronosColor.outline;

			flying = true;
			health = 70f;
			speed = 3f;
			accel = 0.09f;
			drag = 0.06f;
			rotateSpeed = 9f;
			hitSize = 6f;
			engineOffset = 2.5f;
			engineSize = 1.4f;
			engineColor = ChronosColor.chrono;
			//Nanobots come in swarms.
			useUnitCap = false;
			fogRadius = 5f;

			abilities.add(
				new RepairFieldAbility(7f, 50f, 46f){{
					healEffect = ChronosFx.sparkBurst;
				}},
				new ReplicateAbility()
			);

			//The burst of cold it leaves behind.
			weapons.add(new Weapon(){{
				shootOnDeath = true;
				reload = 24f;
				shootCone = 180f;
				ejectEffect = Fx.none;
				shootSound = Sounds.explosion;
				x = shootY = 0f;
				mirror = false;
				bullet = ChronosBullets.nanoBlast;
			}});
		}};

		siphon = new NHUnitType("siphon"){{
			constructor = UnitEntity::create;
			outlineColor = ChronosColor.outline;
			targetFlags = new BlockFlag[]{BlockFlag.battery, BlockFlag.generator, null};

			flying = true;
			health = 520f;
			armor = 3f;
			speed = 2.1f;
			accel = 0.05f;
			drag = 0.05f;
			rotateSpeed = 4.5f;
			hitSize = 13f;
			range = 120f;
			engineOffset = 7f;
			engineSize = 2.8f;
			engineColor = ChronosColor.chrono;

			abilities.add(new GridDrainAbility());

			weapons.add(new Weapon(NewHorizon.name("siphon-pulse")){{
				top = false;
				x = 5f;
				y = 2f;
				reload = 30f;
				ejectEffect = Fx.none;
				shootSound = Sounds.shootLaser;
				bullet = ChronosBullets.siphonPulse;
			}});
		}};

		dissonance = new NHUnitType("dissonance"){{
			constructor = UnitEntity::create;
			outlineColor = ChronosColor.outline;
			targetFlags = new BlockFlag[]{BlockFlag.turret, BlockFlag.generator, null};

			flying = true;
			health = 1100f;
			armor = 5f;
			speed = 1.7f;
			accel = 0.05f;
			drag = 0.05f;
			rotateSpeed = 4f;
			hitSize = 19f;
			engineOffset = 9f;
			engineSize = 3.4f;
			engineColor = ChronosColor.violet;

			weapons.add(new Weapon(NewHorizon.name("dissonance-emitter")){{
				top = false;
				x = 8f;
				y = 3f;
				reload = 48f;
				shoot = new ShootAlternate(5f);
				ejectEffect = Fx.none;
				shootSound = Sounds.shootLaser;
				bullet = ChronosBullets.dissonanceShotSmall;
			}});
		}};

		singularity = new NHUnitType("singularity"){{
			constructor = UnitEntity::create;
			outlineColor = ChronosColor.outline;
			targetFlags = new BlockFlag[]{BlockFlag.turret, BlockFlag.factory, null};
			targetAir = false;
			circleTarget = true;

			flying = true;
			health = 2400f;
			armor = 7f;
			speed = 1.35f;
			accel = 0.04f;
			drag = 0.04f;
			rotateSpeed = 3f;
			hitSize = 26f;
			faceTarget = false;
			engineOffset = 12f;
			engineSize = 4.2f;
			engineColor = ChronosColor.violet;

			weapons.add(new Weapon(){{
				minShootVelocity = 0.6f;
				x = 0f;
				y = 0f;
				mirror = false;
				shootCone = 180f;
				ejectEffect = Fx.none;
				inaccuracy = 6f;
				ignoreRotation = true;
				shootSound = Sounds.shootMissileLarge;
				reload = 60f * 5f;
				bullet = ChronosBullets.blackHoleBomb;
			}});
		}};
	}
}
