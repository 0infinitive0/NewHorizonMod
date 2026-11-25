package newhorizon.content.blocks;

import arc.graphics.Color;
import arc.graphics.g2d.Lines;
import arc.math.Interp;
import arc.math.Mathf;
import mindustry.entities.Effect;
import mindustry.type.Category;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import newhorizon.content.NHBlocks;
import newhorizon.content.NHItems;
import newhorizon.expand.block.distribution.FastConveyor;
import newhorizon.expand.block.distribution.FastJunction;
import newhorizon.expand.block.distribution.FastRouter;
import newhorizon.expand.block.distribution.FloatStackConveyor;

import static mindustry.Vars.tilesize;
import static mindustry.type.ItemStack.with;

/**
 * Three tiers of plain conveyor and three of the plastanium (batch) kind, each tier twice as fast as the one before, starting at twice the fastest belt
 * of each kind that was there (the Multi-Steel Conveyor, 18 items a second; the Iridium Panel Conveyor, 150 a second).
 */
public class FastBelts{
	/** Items a second for each tier of the plain belt and of the batch belt. */
	public static final float[] plainRate = {36f, 72f, 144f};
	public static final float[] batchRate = {300f, 600f, 1200f};
	/** A plain belt moves {@code speed * 150} items a second (items 0.4 tile apart). */
	public static final float plainFactor = 150f;
	/**
	 * A batch belt passes a stack of {@code capacity} items on every {@code recharge / speed + 1} ticks (the extra tick is the receiving tile, which can
	 * pass its stack on only the tick after it gets it), measured in the tests: speed 0.34 and 20 items is 300 a second, speed 1 and 20 is 600, speed 1 and 40 is 1200.
	 */
	public static final float[] batchSpeed = {0.34f, 1f, 1f};
	public static final int[] batchCapacity = {20, 20, 40};

	public static final String[] tierNames = {"turbo", "hyper", "ultra"};
	public static final Color[] tierColors = {Color.valueOf("5fe0d0"), Color.valueOf("b08cff"), Color.valueOf("ffb04a")};
	public static final FastConveyor[] plain = new FastConveyor[3];
	public static final FloatStackConveyor[] batch = new FloatStackConveyor[3];
	/** A junction (items a second per direction), a router and a 2x2 distributor (items a second in all, twice the belt's) for each tier. */
	public static final FastJunction[] junction = new FastJunction[3];
	public static final FastRouter[] router = new FastRouter[3], distributor = new FastRouter[3];

	public static void load(){
		for(int i = 0; i < 3; i++){
			int t = i;
			Color tierColor = tierColors[i];

			plain[i] = new FastConveyor(tierNames[i] + "-conveyor"){{
				requirements(Category.distribution, cost(t, false));
				speed = plainRate[t] / plainFactor;
				displayedSpeed = plainRate[t];
				health = 600 + 100 * t;
				armor = 4 + 2 * t;
				//the push on a unit is made for the base belt's speeds
				if(speed > 0.4f) pushUnits = false;
				junctionReplacement = NHBlocks.multiJunction;
			}};

			batch[i] = new FloatStackConveyor(tierNames[i] + "-plast-conveyor"){{
				requirements(Category.distribution, cost(t, true));
				speed = batchSpeed[t];
				itemCapacity = batchCapacity[t];
				recharge = 1f;
				health = 800 + 100 * t;
				armor = 6 + t;
				loadEffect = unloadEffect = new Effect(30f, e -> {
					Lines.stroke(1.5f * e.fout(Interp.pow2Out), tierColor);
					Lines.square(e.x, e.y, tilesize / 8f * Mathf.sqrt2 * (e.fin(Interp.pow2Out) * 3 + 1f), 45f);
				});
			}
				/** The base belt's number leaves out the tick a stack waits in each tile; this is what the belt really moves. */
				@Override
				public void setStats(){
					super.setStats();
					stats.remove(Stat.itemsMoved);
					stats.add(Stat.itemsMoved, batchRate[t], StatUnit.itemsSecond);
				}
			};

			junction[i] = new FastJunction(tierNames[i] + "-junction"){{
				requirements(Category.distribution, times(cost(t, false), 3));
				speed = junctionTime;
				displayedSpeed = plainRate[t];
				capacity = (int)Math.ceil(plainRate[t] * junctionTime / 60f) + 2;
				health = 500 + 100 * t;
			}};
			plain[i].junctionReplacement = junction[i];

			router[i] = fastRouter(tierNames[i] + "-router", 1, plainRate[t], times(cost(t, false), 3), 500 + 100 * t);
			distributor[i] = fastRouter(tierNames[i] + "-distributor", 2, plainRate[t] * 2f, times(cost(t, false), 12), 1200 + 200 * t);
		}
	}

	//(The names of the parameters differ from the fields' on purpose: inside the block a field's name would hide the parameter.)
	private static FastRouter fastRouter(String name, int sz, float rt, mindustry.type.ItemStack[] costs, int hitpoints){
		return new FastRouter(name){{
			requirements(Category.distribution, costs);
			size = sz;
			rate = rt;
			queueSize = (int)Math.ceil(rt * 2f / 60f) + 2;
			perTick = (int)Math.ceil(rt / 60f) + 1;
			health = hitpoints;
		}};
	}

	private static mindustry.type.ItemStack[] times(mindustry.type.ItemStack[] stacks, int k){
		mindustry.type.ItemStack[] out = new mindustry.type.ItemStack[stacks.length];
		for(int i = 0; i < stacks.length; i++) out[i] = new mindustry.type.ItemStack(stacks[i].item, stacks[i].amount * k);
		return out;
	}

	/** Ticks an item spends inside a fast junction. */
	public static final float junctionTime = 4f;

	/** Costs for a tile of tier {@code t}: the Multi-Steel / Iridium Panel belts' items, with a senior processor from the second tier and fusion energy from the third. */
	private static mindustry.type.ItemStack[] cost(int t, boolean batch){
		int k = batch ? 2 : 1;
		switch(t){
			case 0: return with(NHItems.zeta, 2 * k, NHItems.multipleSteel, 2 * k, NHItems.irayrondPanel, k);
			case 1: return with(NHItems.zeta, 3 * k, NHItems.multipleSteel, 2 * k, NHItems.irayrondPanel, 2 * k, NHItems.seniorProcessor, k);
			default: return with(NHItems.zeta, 4 * k, NHItems.multipleSteel, 3 * k, NHItems.irayrondPanel, 2 * k, NHItems.seniorProcessor, k, NHItems.fusionEnergy, k);
		}
	}
}
