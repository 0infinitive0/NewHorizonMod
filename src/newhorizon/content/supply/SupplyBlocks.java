package newhorizon.content.supply;

import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.blocks.distribution.Conveyor;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.blocks.liquid.Conduit;
import mindustry.world.blocks.liquid.LiquidBridge;

import static mindustry.type.ItemStack.with;

/**
 * The supply blocks, all made of base game items and researched in the Serpulo tree: item-themed conveyors, pipes and bridges in four tiers
 * (lead, graphite, silicon, thorium), better drills, a wind turbine and the Status Towers. Everything here uses the base game's own block
 * classes where it can.
 */
public class SupplyBlocks{
	/** The four belt tiers, in order. */
	public static final String[] tiers = {"lead", "graphite", "silicon", "thorium"};
	/** Items a second (displayed) for each tier's conveyor, the speed the base game's titanium conveyor has for 10. */
	public static final float[] conveyorSpeed = {7f, 9f, 13f, 18f};
	public static final int[] conveyorHealth = {55, 75, 100, 140};
	public static final float[] pipeCapacity = {28f, 36f, 48f, 64f};
	public static final float[] pipePressure = {1.01f, 1.02f, 1.03f, 1.05f};
	public static final int[] bridgeRange = {6, 9, 13, 18};
	public static final float[] pipeBridgeCapacity = {120f, 160f, 220f, 320f};

	public static final Block[] conveyors = new Block[4], pipes = new Block[4], bridges = new Block[4], pipeBridges = new Block[4];

	public static void load(){
		loadBelts();
	}

	private static void loadBelts(){
		for(int i = 0; i < 4; i++){
			int t = i;
			String name = tiers[i];

			conveyors[i] = new Conveyor(name + "-conveyor"){{
				switch(t){
					case 0: requirements(Category.distribution, with(Items.copper, 1, Items.lead, 2)); break;
					case 1: requirements(Category.distribution, with(Items.lead, 1, Items.graphite, 2)); break;
					case 2: requirements(Category.distribution, with(Items.graphite, 1, Items.silicon, 2)); break;
					default: requirements(Category.distribution, with(Items.silicon, 1, Items.thorium, 2)); break;
				}
				health = conveyorHealth[t];
				displayedSpeed = conveyorSpeed[t];
				speed = displayedSpeed / 125f;
			}};

			pipes[i] = new Conduit(name + "-pipe"){{
				switch(t){
					case 0: requirements(Category.liquid, with(Items.metaglass, 1, Items.lead, 1)); break;
					case 1: requirements(Category.liquid, with(Items.metaglass, 1, Items.graphite, 1)); break;
					case 2: requirements(Category.liquid, with(Items.metaglass, 1, Items.silicon, 1)); break;
					default: requirements(Category.liquid, with(Items.metaglass, 1, Items.thorium, 1)); break;
				}
				liquidCapacity = pipeCapacity[t];
				liquidPressure = pipePressure[t];
				health = 55 + 25 * t;
				explosivenessScale = flammabilityScale = 10f / 20f;
			}};

			//No power, unlike the phase conveyor: a plain bridge that moves an item every transportTime ticks, so its rate matches its tier's conveyor.
			bridges[i] = new ItemBridge(name + "-bridge"){{
				switch(t){
					case 0: requirements(Category.distribution, with(Items.copper, 10, Items.lead, 12)); break;
					case 1: requirements(Category.distribution, with(Items.lead, 10, Items.graphite, 14)); break;
					case 2: requirements(Category.distribution, with(Items.lead, 10, Items.graphite, 10, Items.silicon, 16)); break;
					default: requirements(Category.distribution, with(Items.graphite, 10, Items.silicon, 15, Items.thorium, 12)); break;
				}
				range = bridgeRange[t];
				hasPower = false;
				transportTime = 60f / conveyorSpeed[t];
				fadeIn = moveArrows = false;
				arrowSpacing = 6f;
				itemCapacity = 10;
				health = 90 + 40 * t;
				crushFragile = true;
			}};

			pipeBridges[i] = new LiquidBridge(name + "-pipe-bridge"){{
				switch(t){
					case 0: requirements(Category.liquid, with(Items.metaglass, 10, Items.lead, 8)); break;
					case 1: requirements(Category.liquid, with(Items.metaglass, 10, Items.graphite, 12)); break;
					case 2: requirements(Category.liquid, with(Items.metaglass, 12, Items.silicon, 14)); break;
					default: requirements(Category.liquid, with(Items.metaglass, 14, Items.thorium, 12)); break;
				}
				range = bridgeRange[t];
				hasPower = false;
				floating = true;
				fadeIn = moveArrows = false;
				arrowSpacing = 6f;
				liquidCapacity = pipeBridgeCapacity[t];
				explosivenessScale = flammabilityScale = 20f / 100f;
				noAcceptDisabled = true;
				health = 90 + 40 * t;
			}};
		}

		//Auto-bridging: a conveyor that runs into something solid builds its own tier's bridge.
		for(int i = 0; i < 4; i++){
			((Conveyor)conveyors[i]).bridgeReplacement = bridges[i];
			((Conduit)pipes[i]).bridgeReplacement = pipeBridges[i];
		}
	}
}
