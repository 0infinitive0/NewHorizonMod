package newhorizon.expand.block.hiemara;

import arc.Core;
import arc.util.Strings;
import mindustry.content.Liquids;
import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.meta.Stat;

/**
 * An optional ozone input that speeds a production block up. It is kept apart from the base game's optional consumers on purpose:
 * those are all judged together (the boost is the weakest of them), while ozone and power here are independent and multiply:
 * power x2, ozone x1.5, both x3. The block has to call {@link #setup} in init(), {@link #stats} in setStats(), {@link #accepts}
 * in acceptLiquid() and {@link #take} once per tick while it works.
 */
public class OzoneBoost{
	/** Extra speed while the ozone is there: 0.5 is 50% faster. */
	public static float boost = 0.5f;
	
	/** @return ozone used per tick by a block, by its size. */
	public static float use(Block block){
		return 0.01f * block.size * block.size;
	}
	
	/** Gives the block room for ozone. */
	public static void setup(Block block){
		block.hasLiquids = true;
		block.liquidCapacity = Math.max(block.liquidCapacity, 20f);
	}
	
	public static void stats(Block block){
		block.stats.add(Stat.booster, table -> {
			table.row();
			table.add(Core.bundle.format("stat.ozone-boost", Liquids.ozone.localizedName, Strings.autoFixed(use(block) * 60f, 1), (int)(boost * 100f))).left();
		});
	}
	
	/** @return whether the building takes the liquid as an ozone boost input. */
	public static boolean accepts(Building build, Liquid liquid){
		return liquid == Liquids.ozone && build.liquids.get(Liquids.ozone) < build.block.liquidCapacity - 0.0001f;
	}
	
	/** Takes the ozone of one tick, if there is enough. @return the speed multiplier: 1 without ozone, 1 + boost with it. */
	public static float take(Building build, float delta){
		float need = use(build.block) * delta;
		if(build.liquids.get(Liquids.ozone) >= need){
			build.liquids.remove(Liquids.ozone, need);
			return 1f + boost;
		}
		return 1f;
	}
}
