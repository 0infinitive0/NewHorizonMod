package newhorizon.content.fleet;

import arc.graphics.Color;
import arc.struct.ObjectSet;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.StatusEffects;
import mindustry.entities.abilities.ForceFieldAbility;
import mindustry.entities.abilities.StatusFieldAbility;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.UnitEntity;
import mindustry.type.weapons.RepairBeamWeapon;
import mindustry.world.blocks.units.UnitFactory;
import newhorizon.expand.units.ablility.KeepStatusAbility;
import newhorizon.expand.units.ablility.RamAuraAbility;
import newhorizon.expand.units.ablility.RammerBayAbility;
import newhorizon.expand.units.ai.RammerAI;
import newhorizon.expand.units.unitType.NHUnitType;

import static mindustry.type.ItemStack.with;

/**
 * The Mothership and its Rammers. The Mothership (built in the Air Factory) is a big, tough flyer with a repair beam, a shield projector and a speed
 * projector; it carries four Rammers ({@link RammerBayAbility}) that attack what it attacks and come back to it to be healed. A Rammer is a very
 * fast, very tough flyer that is slow to speed up but turns sharply ({@link RammerAI}); the faster it moves the harder it hits everything it
 * touches ({@link RamAuraAbility}), and every hit hurts it too.
 */
public class FleetUnits{
	public static NHUnitType rammer, mothership;
	public static final Color outline = Color.valueOf("2c2a33"), glow = Color.valueOf("ff9a4d");

	public static void load(){
		FleetStatus.load();

		rammer = new NHUnitType("rammer"){{
			constructor = UnitEntity::create;
			aiController = RammerAI::new;
			outlineColor = outline;
			drawCell = false;
			playerControllable = false;
			allowChangeCommands = false;
			useUnitCap = false;

			flying = true;
			lowAltitude = true;
			health = 3000f;
			armor = 75f;
			speed = 6.5f;
			accel = 0.05f;
			drag = 0.003f;
			rotateSpeed = 20f;
			hitSize = 18f;
			range = maxRange = 60f;
			engineOffset = 9f;
			engineSize = 4f;
			engineColor = glow;
			immunities = ObjectSet.with(StatusEffects.burning, StatusEffects.melting, StatusEffects.corroded, StatusEffects.sapped,
				StatusEffects.shocked, StatusEffects.electrified, StatusEffects.freezing);

			abilities.add(new KeepStatusAbility(FleetStatus.bulwark));
			abilities.add(new RamAuraAbility());
		}};

		mothership = new NHUnitType("mothership"){{
			constructor = UnitEntity::create;
			outlineColor = outline;
			drawCell = false;

			flying = true;
			lowAltitude = false;
			health = 24000f;
			armor = 25f;
			speed = 0.9f;
			accel = 0.02f;
			drag = 0.04f;
			rotateSpeed = 1.2f;
			hitSize = 56f;
			engineOffset = 30f;
			engineSize = 10f;
			engineColor = glow;
			immunities = ObjectSet.with(StatusEffects.burning, StatusEffects.melting, StatusEffects.corroded, StatusEffects.sapped);

			//the healing turret: the Rammers hover near it to be mended
			weapons.add(new RepairBeamWeapon("repair-beam-weapon-center"){{
				x = 18f;
				y = -4f;
				shootY = 6f;
				mirror = true;
				beamWidth = 0.9f;
				repairSpeed = 2.5f;
				fractionRepairSpeed = 0.05f;
				targetUnits = true;
				targetBuildings = true;
				bullet = new BulletType(){{
					maxRange = 200f;
				}};
			}});

			abilities.add(new RammerBayAbility(rammer, 4));
			//the shield projector
			abilities.add(new ForceFieldAbility(190f, 2.4f, 6000f, 400f));
			//the speed projector
			abilities.add(new StatusFieldAbility(FleetStatus.rally, 120f, 36f, 260f));
		}};

		//Built in the Air Factory, after what it already makes (so that the numbers saved in factories stay right).
		((UnitFactory)Blocks.airFactory).plans.add(new UnitFactory.UnitPlan(mothership, 60f * 120f,
			with(Items.silicon, 800, Items.titanium, 600, Items.thorium, 400, Items.plastanium, 300, Items.surgeAlloy, 100)));
	}
}
