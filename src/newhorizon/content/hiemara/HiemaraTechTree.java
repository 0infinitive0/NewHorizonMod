package newhorizon.content.hiemara;

import arc.struct.Seq;
import mindustry.content.Items;
import mindustry.game.Objectives;
import mindustry.type.ItemStack;
import newhorizon.content.NHPlanets;

import static mindustry.content.TechTree.*;
import static mindustry.type.ItemStack.with;

/**
 * The tech tree of Hiemara. Besides the blocks it has one branch per cargo unit line: the pad, then the three tiers of
 * unit. The first tier of each line, and the blocks needed to get going, are unlocked from the start.
 */
public class HiemaraTechTree{
	/** Must be called inside the setup of the New Horizon tech tree, like the other branches. */
	public static void load(){
		var root = nodeRoot("hiemara", NHPlanets.hiemara, () -> {
			node(HiemaraBlocks.filterMk1, () -> {
				node(HiemaraBlocks.filterMk2, () -> {
					node(HiemaraBlocks.filterMk3, () -> {
						node(HiemaraBlocks.filterMk4);
					});
				});
			});
			
			node(HiemaraBlocks.port, () -> {
				node(HiemaraBlocks.depot, () -> {
					node(HiemaraBlocks.hangar);
					node(HiemaraBlocks.dock);
				});
			});
			
			node(HiemaraBlocks.airPad, () -> {
				node(HiemaraUnits.cargoDrone, () -> {
					node(HiemaraUnits.cargoHauler, with(Items.copper, 200, Items.lead, 200, Items.sand, 100, Items.coal, 60), null, () -> {
						node(HiemaraUnits.cargoFreighter, with(Items.lead, 400, Items.titanium, 200, Items.sand, 200, Items.coal, 120), null, () -> {});
					});
				});
			});
			
			node(HiemaraBlocks.boatYard, () -> {
				node(HiemaraUnits.cargoSkiff, () -> {
					node(HiemaraUnits.cargoBarge, with(Items.copper, 250, Items.lead, 250, Items.sand, 120, Items.coal, 80), null, () -> {
						node(HiemaraUnits.cargoCarrier, with(Items.lead, 500, Items.titanium, 250, Items.coal, 150, Items.thorium, 60), null, () -> {});
					});
				});
			});
			
			node(HiemaraBlocks.generator, () -> {
				node(HiemaraBlocks.node);
				node(HiemaraBlocks.heater, Seq.with(new Objectives.Research(HiemaraBlocks.generator)), () -> {});
			});
		});
		
		root.planet = NHPlanets.hiemara;
		HiemaraContent.showOnPlanet();
	}
}
