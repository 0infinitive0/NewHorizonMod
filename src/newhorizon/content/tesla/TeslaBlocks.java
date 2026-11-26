package newhorizon.content.tesla;

import arc.graphics.Color;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.type.Category;
import mindustry.type.StatusEffect;
import mindustry.world.Block;
import mindustry.world.draw.DrawTurret;
import newhorizon.expand.block.turrets.TeslaCoil;

import static mindustry.type.ItemStack.with;

/** The Tesla Coil: see {@link TeslaCoil}. Made of base game items; researched under the Meltdown in the Serpulo tree. */
public class TeslaBlocks{
	public static StatusEffect paralysed;
	public static TeslaCoil teslaCoil;

	public static void load(){
		//Speed 0: the unit cannot move (the Disarmed status of the base game takes its weapons).
		paralysed = new StatusEffect("tesla-paralysed"){{
			color = Color.valueOf("5fe8ff");
			speedMultiplier = 0f;
			effect = Fx.shockwave;
			effectChance = 0.05f;
		}};

		teslaCoil = new TeslaCoil("tesla-coil"){{
			requirements(Category.turret, with(Items.silicon, 900, Items.thorium, 500, Items.plastanium, 250, Items.surgeAlloy, 300, Items.phaseFabric, 200));
			size = 4;
			health = 3600;
			armor = 8f;
			range = 20f * 8f;
			lockStatus = paralysed;
			consumePower(150f);
			//no bullet is fired, but a turret needs one to count as armed
			shootType = new BasicBulletType(0f, 0f){{
				lifetime = 0f;
				hittable = false;
			}};
			drawer = new DrawTurret();
		}};
	}
}
