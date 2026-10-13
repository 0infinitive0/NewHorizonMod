package newhorizon.content.jade;

import mindustry.ai.types.SuicideAI;
import mindustry.content.Fx;
import mindustry.entities.abilities.EnergyFieldAbility;
import mindustry.entities.abilities.RepairFieldAbility;
import mindustry.entities.abilities.StatusFieldAbility;
import mindustry.entities.pattern.ShootSpread;
import mindustry.gen.MechUnit;
import mindustry.gen.Sounds;
import mindustry.gen.UnitEntity;
import mindustry.type.Weapon;
import mindustry.type.ammo.ItemAmmoType;
import newhorizon.NewHorizon;
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
		sporeling;

	public static void load(){
		sprig = new NHUnitType("sprig"){{
			constructor = MechUnit::create;
			outlineColor = JadeColor.outline;
			ammoType = new ItemAmmoType(JadeItems.verdantResin);

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
				shootSound = Sounds.pew;
				bullet = JadeBullets.needle;
			}});
		}};

		thornback = new NHUnitType("thornback"){{
			constructor = MechUnit::create;
			outlineColor = JadeColor.outline;
			ammoType = new ItemAmmoType(JadeItems.verdantResin);

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
				shootSound = Sounds.shotgun;
				bullet = JadeBullets.thornSpread;
			}});
		}};

		briarlord = new NHUnitType("briarlord"){{
			constructor = MechUnit::create;
			outlineColor = JadeColor.outline;
			ammoType = new ItemAmmoType(JadeItems.verdantResin);

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
					shootSound = Sounds.artillery;
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
					shootSound = Sounds.shotgun;
					bullet = JadeBullets.thornShard;
				}}
			);
		}};

		wisp = new NHUnitType("wisp"){{
			constructor = UnitEntity::create;
			outlineColor = JadeColor.outline;
			ammoType = new ItemAmmoType(JadeItems.verdantResin);

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
				shootSound = Sounds.pew;
				bullet = JadeBullets.seed;
			}});
		}};

		luminar = new NHUnitType("luminar"){{
			constructor = UnitEntity::create;
			outlineColor = JadeColor.outline;
			ammoType = new ItemAmmoType(JadeItems.verdantResin);

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
				shootSound = Sounds.pew;
				bullet = JadeBullets.lifeBolt;
			}});
		}};

		verdance = new NHUnitType("verdance"){{
			constructor = UnitEntity::create;
			outlineColor = JadeColor.outline;
			ammoType = new ItemAmmoType(JadeItems.verdantResin);

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
					shootSound = Sounds.missile;
					bullet = JadeBullets.bloomMissile;
				}},
				new Weapon(NewHorizon.name("luminar-pod")){{
					top = false;
					x = 6f;
					y = 10f;
					reload = 30f;
					recoil = 1.5f;
					ejectEffect = Fx.none;
					shootSound = Sounds.pew;
					bullet = JadeBullets.lifeBolt;
				}}
			);
		}};

		//Suicide unit. Whatever it infects bursts into more Sporelings when it dies, see JadeInfection.
		sporeling = new NHUnitType("sporeling"){{
			constructor = MechUnit::create;
			aiController = SuicideAI::new;
			outlineColor = JadeColor.outline;
			ammoType = new ItemAmmoType(JadeItems.verdantResin);

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
	}
}
