package newhorizon.content.nuke;

import arc.graphics.Color;
import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.world.Block;
import newhorizon.expand.block.omni.OmniExtractor;
import newhorizon.expand.block.omni.OmniFactory;
import newhorizon.expand.block.omni.OmniShield;
import newhorizon.expand.block.omni.OmniStabilizer;
import newhorizon.expand.units.serpent.SerpentGrowAI;
import newhorizon.expand.block.omni.OmniWall;
import newhorizon.expand.block.omni.SerpentNest;
import mindustry.world.blocks.units.UnitFactory;

import static mindustry.Vars.tilesize;

import static mindustry.type.ItemStack.with;

/**
 * What omni-matter is for: a factory that turns it into the resources of the base game, an extractor that turns it into power, a shield
 * that is paid for in it and a wall that stores the damage it takes and gives it back as a blast. They belong to no faction and are made
 * of items every planet has, but the shield and the wall also need omni-matter to build.
 */
public class OmniBlocks{
	public static Block omniFactory, omniExtractor, omniShield, omniWall, omniStabilizer;
	/** Makes the head of the Serpent and its body sections. */
	public static Block serpentNest;

	public static void load(){
		omniFactory = new OmniFactory("omni-factory"){{
			requirements(Category.crafting, with(Items.silicon, 150, Items.graphite, 100, Items.thorium, 80, Items.metaglass, 60, Items.surgeAlloy, 40));
			health = 1200;
			consumePower(2f);
		}};

		omniExtractor = new OmniExtractor("omni-extractor"){{
			requirements(Category.power, with(Items.silicon, 40, Items.graphite, 30, Items.thorium, 15, Items.surgeAlloy, 10));
			health = 200;
			consumePowerBuffered(20000f);
			energyPerItem = 10000f;
		}};

		omniShield = new OmniShield("omni-shield"){{
			requirements(Category.effect, with(Items.silicon, 120, Items.thorium, 100, Items.surgeAlloy, 60, Items.phaseFabric, 40, NukeItems.omniMatter, 60));
			size = 3;
			health = 1500;
			radius = 200f;
			sides = 6;
			shieldColor = Color.valueOf("c885ff");
			pointsPerItem = 400f;
		}};

		serpentNest = new SerpentNest("serpent-nest"){{
			requirements(Category.units, with(Items.silicon, 300, Items.thorium, 250, Items.surgeAlloy, 150, Items.phaseFabric, 100, NukeItems.omniMatter, 100));
			size = 4;
			health = 2800;
			consumePower(6f);
			plans.add(new UnitFactory.UnitPlan(SerpentUnits.head, 60f * 180f, with(Items.silicon, 800, Items.thorium, 600, Items.surgeAlloy, 400, Items.plastanium, 300, NukeItems.omniMatter, 400)));
			//A section costs what the Add section command costs: omni-matter only, delivered to the Nest instead of taken from the core. Only while there is a head.
			plans.add(new UnitFactory.UnitPlan(SerpentUnits.body, 60f * 20f, with(NukeItems.omniMatter, SerpentGrowAI.cost)));
		}};

		omniStabilizer = new OmniStabilizer("omni-stabilizer"){{
			requirements(Category.effect, with(Items.silicon, 80, Items.thorium, 60, Items.phaseFabric, 40, NukeItems.omniMatter, 40));
			size = 1;
			health = 300;
			consumePower(1f);
			range = 12f * tilesize;
		}};

		omniWall = new OmniWall("omni-wall"){{
			requirements(Category.defense, with(Items.thorium, 12, Items.plastanium, 8, NukeItems.omniMatter, 8));
			health = 5000;
			maxCharge = 3000f;
			threshold = 100f;
		}};
	}
}
