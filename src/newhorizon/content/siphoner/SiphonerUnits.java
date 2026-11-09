package newhorizon.content.siphoner;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.ai.UnitCommand;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.StatusEffects;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.bullet.LightningBulletType;
import mindustry.gen.Sounds;
import mindustry.gen.UnitEntity;
import mindustry.type.Weapon;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.meta.BlockFlag;
import newhorizon.NewHorizon;
import newhorizon.expand.units.siphoner.PowerDrainAbility;
import newhorizon.expand.units.siphoner.PowerLeech;
import newhorizon.expand.units.siphoner.SiphonerAI;
import newhorizon.expand.units.unitType.NHUnitType;

import static mindustry.type.ItemStack.with;

/**
 * The E-Siphoner: a mid-tier flyer, built in the Air Factory. Its Tesla zaps and electrifies enemy units (and does nothing to buildings);
 * its Power drain ({@link PowerDrainAbility}) drains the power network of an enemy block it is near and turns the energy into a shield of up to
 * ten times its health. It can be piloted, and in an RTS command it has Move and Drain.
 */
public class SiphonerUnits{
	public static NHUnitType eSiphoner;
	public static UnitCommand drainCommand;
	public static final Color outline = Color.valueOf("1d2a3a"), arc = Color.valueOf("6fe8ff");

	public static void load(){
		PowerLeech.make();
		drainCommand = new UnitCommand("nh-drain", "power", u -> new SiphonerAI());

		eSiphoner = new NHUnitType("e-siphoner"){{
			constructor = UnitEntity::create;
			aiController = SiphonerAI::new;
			commands = Seq.with(UnitCommand.moveCommand, drainCommand);
			defaultCommand = drainCommand;
			outlineColor = outline;
			drawCell = false;
			targetFlags = new BlockFlag[]{BlockFlag.generator, BlockFlag.battery, null};

			flying = true;
			lowAltitude = true;
			health = 880f;
			armor = 4f;
			speed = 1.9f;
			accel = 0.05f;
			drag = 0.04f;
			rotateSpeed = 4.2f;
			hitSize = 15f;
			engineOffset = 8f;
			engineSize = 3f;
			engineColor = arc;

			abilities.add(new PowerDrainAbility());

			weapons.add(new Weapon(NewHorizon.name("e-siphoner-tesla")){{
				x = 6f;
				y = 2f;
				top = true;
				mirror = true;
				alternate = true;
				reload = 26f;
				ejectEffect = Fx.none;
				shootSound = Sounds.shootArc;
				bullet = tesla();
			}});
		}};

		//Built in the Air Factory, after what it already makes (so that the numbers saved in factories stay right).
		((UnitFactory)Blocks.airFactory).plans.add(new UnitFactory.UnitPlan(eSiphoner, 60f * 50f, with(Items.silicon, 80, Items.graphite, 30, Items.titanium, 50)));
	}

	/** A short-range lightning that electrifies what it hits and does no damage at all to buildings. */
	public static BulletType tesla(){
		return new LightningBulletType(){{
			damage = 24f;
			lightningLength = 14;
			lightningColor = arc;
			collidesAir = true;
			collidesGround = true;
			rangeOverride = 110f;
			ammoMultiplier = 1f;
			//The arc is made of these: they carry the damage and the status, so this is where buildings are spared.
			lightningType = new BulletType(0.0001f, 0f){{
				lifetime = Fx.lightning.lifetime;
				hitEffect = Fx.hitLancer;
				despawnEffect = Fx.none;
				status = StatusEffects.electrified;
				statusDuration = 120f;
				hittable = false;
				lightColor = Color.white;
				collidesAir = true;
				buildingDamageMultiplier = 0f;
			}};
		}};
	}
}
