package newhorizon.content.jade;

import newhorizon.expand.units.ai.gardener.GardenerMind;
import newhorizon.expand.units.scavenger.Scavenging;

/**
 * The Jade Accord: a bio-tech faction that grows its arsenal from spore pods instead of smelting it.
 * Everything it fires leaves targets {@link JadeStatus#bloom blooming}; its support units and walls regrow.
 *
 * <p>Load order matters: statuses -> items -> bullets -> units -> blocks. The tech tree branch is attached
 * from {@code NHTechTree}, because it has to be created inside the New Horizon tech root.
 */
public class JadeFaction{
	public static void load(){
		JadeStatus.load();
		JadeItems.load();
		JadeBullets.load();
		JadeUnits.load();
		JadeBullets.loadPods();
		JadeInfection.load();
		JadeBlocks.load();
		GardenerMind.instance.init();
		Scavenging.init();
	}
}
