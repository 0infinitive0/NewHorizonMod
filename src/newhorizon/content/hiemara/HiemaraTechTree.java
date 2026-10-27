package newhorizon.content.hiemara;

import arc.struct.Seq;
import mindustry.content.Items;
import mindustry.game.Objectives;

import static mindustry.content.TechTree.*;
import static mindustry.type.ItemStack.with;
import newhorizon.content.NHPlanets;

/**
 * The tech tree of Hiemara. Besides the blocks it has one branch per cargo unit line: the pad or yard, then the tiers of
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
				node(HiemaraBlocks.sandDredger);
				node(HiemaraBlocks.iceHarvester);
				node(HiemaraBlocks.sporeCultivator, () -> {
					node(HiemaraBlocks.floatingSporePress, () -> {
						node(HiemaraBlocks.floatingCoalCentrifuge);
					});
				});
			});
			
			node(HiemaraBlocks.port, () -> {
				node(HiemaraBlocks.improvedPort1, () -> {
					node(HiemaraBlocks.improvedPort2, () -> {
						node(HiemaraBlocks.improvedPort3);
					});
				});
				node(HiemaraBlocks.liquidPort);
				node(HiemaraBlocks.depot, () -> {
					node(HiemaraBlocks.hangar);
					node(HiemaraBlocks.dock);
					node(HiemaraBlocks.liquidDepot);
					node(HiemaraBlocks.distributor1, () -> {
						node(HiemaraBlocks.distributor2, () -> {
							node(HiemaraBlocks.distributor3);
						});
					});
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
						node(HiemaraUnits.cargoCarrier, with(Items.lead, 500, Items.titanium, 250, Items.coal, 150, Items.thorium, 60), null, () -> {
							node(HiemaraUnits.cargoGalleon, with(Items.lead, 900, Items.titanium, 500, Items.silicon, 300, Items.coal, 200), null, () -> {
								node(HiemaraUnits.cargoLeviathan, with(Items.lead, 1500, Items.titanium, 1000, Items.silicon, 700, Items.thorium, 200), null, () -> {
									node(HiemaraUnits.cargoTitan, with(Items.titanium, 2500, Items.silicon, 1500, Items.thorium, 600, Items.tungsten, 300), null, () -> {});
								});
							});
						});
					});
				});
				node(HiemaraUnits.tankerCutter, () -> {
					node(HiemaraUnits.tankerBulk, with(Items.copper, 200, Items.lead, 250, Items.sand, 120, Items.coal, 60), null, () -> {
						node(HiemaraUnits.tankerSuper, with(Items.lead, 500, Items.titanium, 250, Items.silicon, 100, Items.coal, 120), null, () -> {});
					});
				});
			});
			
			node(HiemaraBlocks.generator, () -> {
				node(HiemaraBlocks.node, () -> {
					node(HiemaraBlocks.nodeLarge, () -> {
						node(HiemaraBlocks.surgeTower);
					});
				});
				node(HiemaraBlocks.heater, Seq.with(new Objectives.Research(HiemaraBlocks.generator)), () -> {});
				node(HiemaraBlocks.windTurbine);
				node(HiemaraBlocks.hydroGenerator);
				node(HiemaraBlocks.floatingSolar);
				node(HiemaraBlocks.battery1, () -> {
					node(HiemaraBlocks.battery2, () -> {
						node(HiemaraBlocks.battery3);
					});
				});
				node(HiemaraBlocks.floatingSiliconSmelter, () -> {
					node(HiemaraBlocks.floatingSiliconCrucible);
					node(HiemaraBlocks.floatingPyratiteMixer);
				});
			});
			
			node(HiemaraBlocks.floatingPump, () -> {
				node(HiemaraBlocks.floatingRotaryPump, () -> {
					node(HiemaraBlocks.electricSeparator, () -> {
						node(HiemaraBlocks.fusionReactor, Seq.with(new Objectives.Research(HiemaraBlocks.battery3), new Objectives.Research(HiemaraBlocks.surgeTower)), () -> {});
					});
				});
				node(HiemaraBlocks.liquidContainer, () -> {
					node(HiemaraBlocks.liquidTank);
				});
			});
		});
		
		root.planet = NHPlanets.hiemara;
		HiemaraContent.showOnPlanet();
	}
}
