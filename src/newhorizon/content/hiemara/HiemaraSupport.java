package newhorizon.content.hiemara;

import arc.struct.Seq;
import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.blocks.defense.ForceProjector;
import mindustry.world.blocks.defense.MendProjector;
import mindustry.world.blocks.defense.OverdriveProjector;
import mindustry.world.blocks.units.RepairTurret;

import static mindustry.type.ItemStack.with;

/**
 * Support blocks for the floating bases: menders, an overdrive projector, a shield projector and a repair point for ships.
 * They are the base game's own classes, placed on deep water.
 */
public class HiemaraSupport{
	public static MendProjector mender, mendProjector;
	public static OverdriveProjector overdriveProjector;
	public static ForceProjector forceProjector;
	public static RepairTurret repairPoint;
	
	public static final Seq<Block> all = new Seq<>();
	
	public static void load(){
		mender = new MendProjector("floating-mender"){{
			requirements(Category.effect, with(Items.copper, 30, Items.lead, 25));
			size = 1;
			health = 100;
			reload = 200f;
			range = 40f;
			healPercent = 4f;
			phaseBoost = 4f;
			phaseRangeBoost = 20f;
			placeableLiquid = true;
			consumePower(0.3f);
			consumeItem(Items.silicon).boost();
		}};
		
		mendProjector = new MendProjector("floating-mend-projector"){{
			requirements(Category.effect, with(Items.lead, 100, Items.titanium, 25, Items.silicon, 40, Items.copper, 50));
			size = 2;
			health = 320;
			reload = 250f;
			range = 85f;
			healPercent = 11f;
			phaseBoost = 15f;
			placeableLiquid = true;
			consumePower(1.5f);
			consumeItem(Items.phaseFabric).boost();
		}};
		
		overdriveProjector = new OverdriveProjector("floating-overdrive-projector"){{
			requirements(Category.effect, with(Items.lead, 100, Items.titanium, 75, Items.silicon, 75, Items.plastanium, 30));
			size = 2;
			health = 360;
			placeableLiquid = true;
			consumePower(3.5f);
			consumeItem(Items.phaseFabric).boost();
		}};
		
		forceProjector = new ForceProjector("floating-force-projector"){{
			requirements(Category.effect, with(Items.lead, 100, Items.titanium, 75, Items.silicon, 125));
			size = 3;
			health = 700;
			phaseRadiusBoost = 80f;
			radius = 101.7f;
			shieldHealth = 750f;
			cooldownNormal = 1.5f;
			cooldownLiquid = 1.2f;
			cooldownBrokenBase = 0.35f;
			placeableLiquid = true;
			itemConsumer = consumeItem(Items.phaseFabric).boost();
			consumePower(4f);
		}};
		
		repairPoint = new RepairTurret("floating-repair-point"){{
			requirements(Category.units, with(Items.lead, 30, Items.copper, 30, Items.silicon, 20));
			repairSpeed = 0.45f;
			repairRadius = 60f;
			powerUse = 1f;
			placeableLiquid = true;
		}};
		
		all.addAll(mender, mendProjector, overdriveProjector, forceProjector, repairPoint);
	}
}
