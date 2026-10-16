package newhorizon.content.jade;

import arc.struct.Seq;
import mindustry.content.Items;
import mindustry.game.Objectives;
import mindustry.type.ItemStack;
import newhorizon.content.NHBlocks;

import static mindustry.content.TechTree.*;

/** Adds the Jade Accord branch. Must be called inside the New Horizon tech tree root. */
public class JadeTechTree{
	public static void load(){
		node(JadeBlocks.resinTapper, ItemStack.with(Items.copper, 600, Items.lead, 400, Items.graphite, 250), Seq.with(new Objectives.Research(NHBlocks.jumpGatePrimary)), () -> {
			nodeProduce(JadeItems.verdantResin, () -> {
				nodeProduce(JadeItems.jadeSilk, () -> {
					nodeProduce(JadeItems.everbloomCore, () -> {});
				});
			});

			node(JadeBlocks.sporeFurnace);

			node(JadeBlocks.silkLoom, () -> {
				node(JadeBlocks.bloomConservatory);
				node(JadeBlocks.lifebloomPylon, () -> {
					node(JadeBlocks.verdantClinic);
				});
				node(JadeBlocks.jadeWall, () -> {
					node(JadeBlocks.jadeWallLarge);
				});
			});

			node(JadeBlocks.thornSpitter, () -> {
				node(JadeBlocks.briarLance, () -> {
					node(JadeBlocks.canopyMortar, Seq.with(new Objectives.Produce(JadeItems.everbloomCore)), () -> {});
				});
			});

			node(JadeBlocks.jadeNursery, () -> {
				node(JadeUnits.sprig, () -> {
					node(JadeUnits.thornback, Seq.with(new Objectives.Research(JadeBlocks.verdantCultivator)), () -> {
						node(JadeUnits.briarlord, Seq.with(new Objectives.Research(JadeBlocks.groveCultivator)), () -> {});
					});
				});
				node(JadeUnits.wisp, () -> {
					node(JadeUnits.luminar, Seq.with(new Objectives.Research(JadeBlocks.verdantCultivator)), () -> {
						node(JadeUnits.verdance, Seq.with(new Objectives.Research(JadeBlocks.groveCultivator)), () -> {});
					});
				});
				node(JadeUnits.gardener, () -> {});
				node(JadeUnits.herbalist, () -> {});
				node(JadeUnits.sporeling, () -> {
					node(JadeBlocks.sporeCatapult);
				});
				node(JadeBlocks.verdantCultivator, () -> {
					node(JadeBlocks.groveCultivator, Seq.with(new Objectives.Research(JadeBlocks.silkLoom)), () -> {});
				});
			});
		});
	}
}
