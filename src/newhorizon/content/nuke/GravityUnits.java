package newhorizon.content.nuke;

import arc.graphics.Color;
import mindustry.gen.LegsUnit;
import mindustry.gen.Sounds;
import mindustry.graphics.Layer;
import newhorizon.NewHorizon;
import newhorizon.expand.bullets.nuke.TractorBulletType;
import newhorizon.expand.units.crab.OnePerTeamAbility;
import newhorizon.expand.units.gravity.GravityFieldAbility;
import newhorizon.expand.units.gravity.PullWeapon;
import newhorizon.expand.units.unitType.NHUnitType;

/**
 * The Gravity Base: one to a team, a big round hovering unit that floats slowly over land, water and blocks with a ring of orbs circling
 * it. Enemy units and slow bullets inside its field revolve round it (the bullets turn to its side), everything of the enemy's there is
 * crushed, a tractor beam drags in enemies from outside, and what dies near it feeds its shield. It is made in the Gravity Dock.
 */
public class GravityUnits{
	public static NHUnitType gravityBase;

	public static void load(){
		Color violet = Color.valueOf("8a6bff"), ice = Color.valueOf("c9f4ff");
		float field = 300f;

		gravityBase = new NHUnitType("gravity-base"){{
			//A disc that hovers over everything: it is a legged unit with no legs, which is what lets it cross walls and deep water.
			constructor = LegsUnit::create;
			outlineColor = Color.valueOf("1a1630");
			drawCell = false;
			allowedInPayloads = false;
			hitSize = 88f;
			health = 90000f;
			armor = 40f;
			speed = 0.3f;
			drag = 0.12f;
			accel = 0.15f;
			rotateSpeed = 1.2f;
			faceTarget = false;
			omniMovement = true;
			rotateMoveFirst = false;
			knockbackMultiplier = 0f;
			hovering = true;
			canDrown = false;
			allowLegStep = true;
			legCount = 0;
			shadowElevation = 0.6f;
			groundLayer = Layer.legUnit;
			range = maxRange = 560f;

			abilities.add(new OnePerTeamAbility());
			abilities.add(new GravityFieldAbility(){{
				radius = field;
				color = violet;
				glow = ice;
			}});

			//The pull: a tractor beam that reaches well past the field, at enemy units that are not in it yet.
			weapons.add(new PullWeapon(NewHorizon.name("gravity-base-emitter")){{
				fieldRadius = field;
				x = 0f;
				y = 0f;
				mirror = false;
				rotate = true;
				rotateSpeed = 6f;
				recoil = 0f;
				shootY = 20f;
				shootSound = Sounds.shootLaser;
				bullet = new TractorBulletType(1f){{
					length = 560f;
					lifetime = 120f;
					width = 18f;
					force = 520f;
					scaledForce = 320f;
					holdDistance = field * 0.8f;
					skipWithin = field;
					color = hitColor = violet;
					beamWidth = 0.8f;
				}};
			}});
		}};
	}
}
