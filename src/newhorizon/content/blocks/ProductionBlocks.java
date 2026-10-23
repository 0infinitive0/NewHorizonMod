package newhorizon.content.blocks;

import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import newhorizon.content.NHUnitTypes;
import newhorizon.expand.block.production.drill.*;
import newhorizon.expand.block.production.ice.IceMiningStation;

public class ProductionBlocks {
    public static AdaptDrill resonanceMiningFacility, beamMiningFacility, implosionMiningFacility;

    public static DrillModule speedModule, refineModule, deliveryModule;

    public static IceMiningStation iceMiningStation;

    public static void load(){
        resonanceMiningFacility = new ResonanceDrill();
        beamMiningFacility = new BeamDrill();
        implosionMiningFacility = new ImplosionDrill();

        iceMiningStation = new IceMiningStation("ice-mining-station"){{
            requirements(Category.production, ItemStack.with(Items.copper, 80, Items.graphite, 60, Items.silicon, 40, Items.titanium, 20));
            size = 3;
            health = 360;
            unitType = NHUnitTypes.iceMiner;
            maxUnits = 3;
            radius = 12 * 8f;
            consumePower(1f);
        }};

        speedModule = new SpeedModule();
        refineModule = new RefineModule();
        deliveryModule = new DeliveryModule();
    }
}
