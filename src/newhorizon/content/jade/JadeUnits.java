package newhorizon.content.jade;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.ai.UnitCommand;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.abilities.EnergyFieldAbility;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.abilities.RepairFieldAbility;
import mindustry.entities.abilities.StatusFieldAbility;
import mindustry.entities.pattern.ShootSpread;
import mindustry.gen.MechUnit;
import mindustry.gen.Sounds;
import mindustry.gen.UnitEntity;
import mindustry.type.Weapon;
import newhorizon.NewHorizon;
import newhorizon.expand.units.ai.HealerAI;
import newhorizon.expand.units.ai.MimicAI;
import newhorizon.expand.units.mimic.MimicAbility;
import newhorizon.expand.units.ai.SporelingAI;
import newhorizon.expand.units.ablility.HealSeekAbility;
import newhorizon.expand.units.ablility.WaterRegenAbility;
import newhorizon.expand.units.scavenger.ScavengerAbility;
import newhorizon.expand.units.ai.gardener.GardenerAI;
import newhorizon.expand.units.ai.gardener.GardenerAbility;
import newhorizon.expand.units.unitType.NHUnitType;

/**
 * Jade Accord units. Two lines, three tiers each:
 * <ul>
 *     <li>Ground (thorn line): {@link #sprig} -> {@link #thornback} -> {@link #briarlord}</li>
 *     <li>Air (pollen line): {@link #wisp} -> {@link #luminar} -> {@link #verdance}</li>
 *     <li>Suicide: {@link #sporeling}, which infects what it blows up</li>
 * </ul>
 */
public class JadeUnits{
	public static NHUnitType
		sprig, thornback, briarlord,
		wisp, luminar, verdance,
		sporeling,
		gardener, herbalist, scavenger, bogguard, mimic;

	/** Reach of the Herbalist's repair aura, also used by units deciding whether they have arrived. */
	public static final float herbalistHealRange = 80f;

	/** The Gardener's own command: it picks {@link GardenerAI} so a player-team Gardener learns instead of standing around. */
	public static UnitCommand gardenCommand, healCommand, mimicCommand, huntCommand;

	public static void load(){
		sprig = new NHUnitType("sprig"){{
			constructor = MechUnit::create;
			outlineColor = JadeColor.outline;

			health = 230f;
			armor = 1f;
			speed = 0.55f;
			hitSize = 9f;
			rotateSpeed = 3.6f;
			mechStepParticles = true;
			stepShake = 0.05f;

			weapons.add(new Weapon(NewHorizon.name("sprig-needler")){{
				top = false;
				x = 4.5f;
				y = 1f;
				reload = 14f;
				recoil = 1.5f;
				inaccuracy = 2f;
				ejectEffect = Fx.none;
				shootSound = Sounds.shootDuo;
				bullet = JadeBullets.needle;
			}});
		}};

		thornback = new NHUnitType("thornback"){{
			constructor = MechUnit::create;
			outlineColor = JadeColor.outline;

			health = 900f;
			armor = 4f;
			speed = 0.48f;
			hitSize = 13f;
			rotateSpeed = 3f;
			mechStepParticles = true;
			stepShake = 0.1f;

			weapons.add(new Weapon(NewHorizon.name("thornback-thorn")){{
				top = false;
				x = 7.5f;
				y = 1f;
				reload = 42f;
				recoil = 2.5f;
				shoot = new ShootSpread(3, 11f);
				inaccuracy = 3f;
				ejectEffect = Fx.none;
				shootSound = Sounds.shootToxopidShotgun;
				bullet = JadeBullets.thornSpread;
			}});
		}};

		briarlord = new NHUnitType("briarlord"){{
			constructor = MechUnit::create;
			outlineColor = JadeColor.outline;

			health = 3300f;
			armor = 9f;
			speed = 0.4f;
			hitSize = 21f;
			rotateSpeed = 2.4f;
			mechStepParticles = true;
			stepShake = 0.2f;
			mechLandShake = 2f;

			abilities.add(new StatusFieldAbility(JadeStatus.regrowth, 60f * 6f, 60f * 5f, 90f));

			weapons.add(
				new Weapon(NewHorizon.name("briarlord-mortar")){{
					top = true;
					mirror = false;
					rotate = true;
					rotateSpeed = 2.2f;
					x = 0f;
					y = -2f;
					shootY = 12f;
					reload = 95f;
					recoil = 4.5f;
					shake = 2f;
					ejectEffect = Fx.none;
					shootSound = Sounds.shootArtillery;
					bullet = JadeBullets.briarMortar;
				}},
				new Weapon(NewHorizon.name("thornback-thorn")){{
					top = false;
					x = 14f;
					y = 2f;
					reload = 26f;
					recoil = 2f;
					shoot = new ShootSpread(2, 7f);
					inaccuracy = 2.5f;
					ejectEffect = Fx.none;
					shootSound = Sounds.shootToxopidShotgun;
					bullet = JadeBullets.thornShard;
				}}
			);
		}};

		wisp = new NHUnitType("wisp"){{
			constructor = UnitEntity::create;
			outlineColor = JadeColor.outline;

			flying = true;
			health = 130f;
			speed = 2.7f;
			accel = 0.08f;
			drag = 0.04f;
			rotateSpeed = 6f;
			hitSize = 8f;
			engineOffset = 5f;
			engineSize = 2.2f;
			engineColor = JadeColor.pollen;

			weapons.add(new Weapon(){{
				top = false;
				mirror = false;
				x = 0f;
				y = 1f;
				reload = 28f;
				inaccuracy = 3f;
				ejectEffect = Fx.none;
				shootSound = Sounds.shootDuo;
				bullet = JadeBullets.seed;
			}});
		}};

		luminar = new NHUnitType("luminar"){{
			constructor = UnitEntity::create;
			outlineColor = JadeColor.outline;

			flying = true;
			health = 580f;
			armor = 3f;
			speed = 2.2f;
			accel = 0.07f;
			drag = 0.05f;
			rotateSpeed = 4.6f;
			hitSize = 14f;
			engineOffset = 8f;
			engineSize = 3f;
			engineColor = JadeColor.jadeLight;

			abilities.add(
				new RepairFieldAbility(16f, 60f * 1.5f, 64f),
				new StatusFieldAbility(JadeStatus.regrowth, 60f * 5f, 60f * 4f, 64f)
			);

			weapons.add(new Weapon(NewHorizon.name("luminar-pod")){{
				top = false;
				x = 6f;
				y = 1f;
				reload = 34f;
				recoil = 1.5f;
				inaccuracy = 2f;
				ejectEffect = Fx.none;
				shootSound = Sounds.shootDuo;
				bullet = JadeBullets.lifeBolt;
			}});
		}};

		verdance = new NHUnitType("verdance"){{
			constructor = UnitEntity::create;
			outlineColor = JadeColor.outline;

			flying = true;
			health = 2500f;
			armor = 7f;
			speed = 1.6f;
			accel = 0.05f;
			drag = 0.04f;
			rotateSpeed = 2.6f;
			hitSize = 26f;
			engineOffset = 18f;
			engineSize = 5f;
			engineColor = JadeColor.pollen;
			lowAltitude = true;

			abilities.add(new EnergyFieldAbility(34f, 70f, 160f){{
				color = JadeColor.jade;
				status = JadeStatus.bloom;
				statusDuration = 60f * 4f;
				maxTargets = 14;
				healPercent = 2f;
				effectRadius = 5f;
				sectors = 5;
			}});

			weapons.add(
				new Weapon(NewHorizon.name("verdance-launcher")){{
					top = false;
					x = 13f;
					y = -3f;
					reload = 58f;
					recoil = 3f;
					shoot = new ShootSpread(2, 8f);
					inaccuracy = 2f;
					ejectEffect = Fx.none;
					shootSound = Sounds.shootMissile;
					bullet = JadeBullets.bloomMissile;
				}},
				new Weapon(NewHorizon.name("luminar-pod")){{
					top = false;
					x = 6f;
					y = 10f;
					reload = 30f;
					recoil = 1.5f;
					ejectEffect = Fx.none;
					shootSound = Sounds.shootDuo;
					bullet = JadeBullets.lifeBolt;
				}}
			);
		}};

		//Hunt is the default command. A click on the ground or on an enemy while it is selected only guides it while it is idle
		//instead of switching it to plain moving, so it keeps hunting anything in reach (SporelingAI).
		huntCommand = new UnitCommand("nh-hunt", "units", u -> new SporelingAI());
		huntCommand.switchToMove = false;

		//Suicide unit. Whatever it infects bursts into more Sporelings when it dies, see JadeInfection.
		sporeling = new NHUnitType("sporeling"){{
			constructor = MechUnit::create;
			aiController = SporelingAI::new;
			commands = Seq.with(huntCommand, UnitCommand.moveCommand);
			defaultCommand = huntCommand;
			outlineColor = JadeColor.outline;

			health = 110f;
			speed = 1.05f;
			hitSize = 8f;
			range = 40f;
			targetAir = false;
			//Sporelings burst out in swarms (one per 100 max health of the host), which the normal unit cap would cull.
			useUnitCap = false;
			mechSideSway = 0.25f;
			mechStepParticles = false;

			weapons.add(new Weapon(){{
				shootOnDeath = true;
				reload = 24f;
				shootCone = 180f;
				ejectEffect = Fx.none;
				shootSound = Sounds.explosion;
				x = shootY = 0f;
				mirror = false;
				bullet = JadeBullets.sporelingBlast;
			}});
		}};

		gardenCommand = new UnitCommand("nh-gardener", "hammer", u -> new GardenerAI());

		//A builder that learns: starts as a plain BuilderAI and evolves with experience, see GardenerMind.
		gardener = new NHUnitType("gardener"){{
			constructor = UnitEntity::create;
			aiController = GardenerAI::new;
			commands = Seq.with(UnitCommand.moveCommand, gardenCommand, UnitCommand.assistCommand);
			defaultCommand = gardenCommand;
			outlineColor = JadeColor.outline;

			flying = true;
			health = 420f;
			armor = 2f;
			speed = 2.3f;
			accel = 0.06f;
			drag = 0.05f;
			rotateSpeed = 5f;
			hitSize = 12f;
			engineOffset = 6f;
			engineSize = 2.4f;
			engineColor = JadeColor.jadeLight;

			buildSpeed = 1.1f;
			itemCapacity = 30;

			abilities.add(new GardenerAbility());
		}};

		healCommand = new UnitCommand("nh-heal", "add", u -> new HealerAI());

		//Support flier that goes to whoever is hurt most and heals them with an aura.
		herbalist = new NHUnitType("herbalist"){{
			constructor = UnitEntity::create;
			aiController = HealerAI::new;
			commands = Seq.with(UnitCommand.moveCommand, healCommand);
			defaultCommand = healCommand;
			outlineColor = JadeColor.outline;

			flying = true;
			health = 380f;
			armor = 1f;
			speed = 2.6f;
			accel = 0.07f;
			drag = 0.05f;
			rotateSpeed = 5f;
			hitSize = 11f;
			engineOffset = 6f;
			engineSize = 2.4f;
			engineColor = JadeColor.pollen;

			abilities.add(
				new RepairFieldAbility(30f, 60f, herbalistHealRange),
				new StatusFieldAbility(JadeStatus.regrowth, 60f * 4f, 60f * 3f, herbalistHealRange)
			);
		}};

		//Starts as a weak little thing with a duo-like gun. Every enemy it helps destroy makes it tougher, and now and then
		//it walks away with a weapon or ability of the victim. See ScavengerAbility and Scavenging.
		scavenger = new NHUnitType("scavenger"){{
			constructor = MechUnit::create;
			outlineColor = JadeColor.outline;

			health = 190f;
			armor = 0f;
			speed = 0.62f;
			hitSize = 10f;
			rotateSpeed = 3.4f;
			mechStepParticles = true;
			stepShake = 0.05f;

			abilities.add(new ScavengerAbility());

			weapons.add(new Weapon(NewHorizon.name("scavenger-gun")){{
				top = true;
				mirror = false;
				x = 0f;
				y = 1f;
				shootY = 5f;
				reload = 26f;
				recoil = 1.5f;
				rotate = true;
				rotateSpeed = 12f;
				inaccuracy = 3f;
				ejectEffect = Fx.none;
				shootSound = Sounds.shoot;
				bullet = new BasicBulletType(2.8f, 9f){{
					width = 7f;
					height = 9f;
					lifetime = 55f;
					backColor = lightColor = hitColor = JadeColor.resin;
					frontColor = Color.white;
					shootEffect = Fx.shootSmall;
					hitEffect = despawnEffect = Fx.hitBulletSmall;
					smokeEffect = Fx.none;
				}};
			}});
		}};

		//Spores love water. A slow tank that soaks up a lot of damage and heals while it stands in water, and can wade through deep
		//water without drowning.
		bogguard = new NHUnitType("bogguard"){{
			constructor = MechUnit::create;
			outlineColor = JadeColor.outline;

			health = 2600f;
			armor = 12f;
			speed = 0.36f;
			hitSize = 17f;
			rotateSpeed = 2.4f;
			canDrown = false;
			mechStepParticles = true;
			stepShake = 0.15f;
			mechLandShake = 3f;
			immunities.add(StatusEffects.wet);

			abilities.add(new WaterRegenAbility());

			weapons.add(new Weapon(NewHorizon.name("thornback-thorn")){{
				top = false;
				x = 11f;
				y = 1f;
				reload = 38f;
				recoil = 2.5f;
				shoot = new ShootSpread(2, 9f);
				inaccuracy = 3f;
				ejectEffect = Fx.none;
				shootSound = Sounds.shootToxopidShotgun;
				bullet = JadeBullets.thornSpread;
			}});
		}};

		mimicCommand = new UnitCommand("nh-mimic", "copy", u -> new MimicAI());

		//No weapons. Next to any other unit, friendly or enemy, it turns into that kind of unit for good: all its weapons and
		//abilities, but only the health and armor stated here. See Mimicry. Pilot it and hold fire to copy the unit you aim at.
		mimic = new NHUnitType("mimic"){{
			constructor = UnitEntity::create;
			aiController = MimicAI::new;
			commands = Seq.with(UnitCommand.moveCommand, mimicCommand);
			defaultCommand = UnitCommand.moveCommand;
			outlineColor = JadeColor.outline;

			flying = true;
			health = 600f;
			armor = 4f;
			speed = 2.6f;
			accel = 0.07f;
			drag = 0.05f;
			rotateSpeed = 5.5f;
			hitSize = 11f;
			engineOffset = 5f;
			engineSize = 2.2f;
			engineColor = JadeColor.jadeLight;
			isEnemy = false;

			abilities.add(new MimicAbility());
		}};

		//Everything that can look after itself goes to a healer when idle and hurt. The Sporeling is a suicide unit: no point.
		for(NHUnitType type : new NHUnitType[]{sprig, thornback, briarlord, wisp, luminar, verdance, gardener, scavenger, bogguard, mimic}){
			type.abilities.add(new HealSeekAbility());
		}
	}
}
