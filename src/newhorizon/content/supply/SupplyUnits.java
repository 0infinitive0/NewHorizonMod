package newhorizon.content.supply;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.ai.UnitCommand;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.gen.MechUnit;
import mindustry.type.UnitType;
import mindustry.world.blocks.units.UnitFactory;
import newhorizon.expand.units.ai.GroundMinerAI;

import static mindustry.type.ItemStack.with;

/**
 * Four ground miners for the early and middle game, built in the Ground Factory: the Digger and the Delver cost less than the mono, the
 * Burrower reaches titanium and the Excavator thorium. They walk to the ore nearest the core, mine it, and walk the load back
 * ({@link GroundMinerAI}).
 */
public class SupplyUnits{
	public static UnitCommand mineCommand;
	public static UnitType digger, delver, burrower, excavator;
	public static final Color outline = Color.valueOf("2c2a33");

	public static void load(){
		mineCommand = new UnitCommand("nh-ground-mine", "production", u -> new GroundMinerAI());

		digger = miner("digger", 70f, 0.55f, 8f, 15, 1.0f, 1, new mindustry.type.Item[]{Items.copper, Items.lead});
		delver = miner("delver", 130f, 0.65f, 9f, 30, 1.5f, 2, new mindustry.type.Item[]{Items.copper, Items.lead, Items.sand, Items.scrap, Items.coal});
		burrower = miner("burrower", 220f, 0.7f, 10f, 50, 2.2f, 3, new mindustry.type.Item[]{Items.copper, Items.lead, Items.sand, Items.scrap, Items.coal, Items.titanium});
		excavator = miner("excavator", 350f, 0.75f, 12f, 70, 3.0f, 4, new mindustry.type.Item[]{Items.copper, Items.lead, Items.sand, Items.scrap, Items.coal, Items.titanium, Items.thorium});

		UnitFactory factory = (UnitFactory)Blocks.groundFactory;
		factory.plans.add(new UnitFactory.UnitPlan(digger, 60f * 20f, with(Items.silicon, 10, Items.lead, 10)));
		factory.plans.add(new UnitFactory.UnitPlan(delver, 60f * 30f, with(Items.silicon, 20, Items.lead, 15, Items.graphite, 5)));
		factory.plans.add(new UnitFactory.UnitPlan(burrower, 60f * 40f, with(Items.silicon, 40, Items.lead, 25, Items.titanium, 20)));
		factory.plans.add(new UnitFactory.UnitPlan(excavator, 60f * 55f, with(Items.silicon, 70, Items.lead, 40, Items.titanium, 40, Items.thorium, 20)));
	}

	private static UnitType miner(String name, float hp, float walk, float size, int capacity, float dig, int tier, mindustry.type.Item[] items){
		return new UnitType(name){{
			constructor = MechUnit::create;
			aiController = GroundMinerAI::new;
			commands = Seq.with(mineCommand, UnitCommand.moveCommand);
			defaultCommand = mineCommand;
			outlineColor = outline;

			//(The names of the parameters differ from the fields' on purpose: inside this block a field's name would hide the parameter.)
			health = hp;
			speed = walk;
			hitSize = size;
			itemCapacity = capacity;
			mineSpeed = dig;
			mineTier = tier;
			mineItems = Seq.with(items);
			mineFloor = true;
			isEnemy = false;
			fogRadius = 6f;
			canAttack = false;
			drawCell = false;
			mechStepParticles = false;
			mechSideSway = 0.3f;
		}};
	}
}
