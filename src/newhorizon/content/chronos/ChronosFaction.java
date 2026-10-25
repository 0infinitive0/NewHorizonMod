package newhorizon.content.chronos;

import newhorizon.expand.block.chronos.TemporalDisable;

/**
 * Chronos: a faction built around time. It stops enemies in place, slows their bullets, rewinds its own walls, teleports items and
 * units, and ends fights with black holes. Everything is made from silicon, graphite and two items crafted from them, so it works on
 * every planet.
 *
 * <p>Load order matters: statuses -> items -> bullets -> units -> blocks. The tech tree branches are attached from {@code NHTechTree}.
 */
public class ChronosFaction{
	public static void load(){
		ChronosStatus.load();
		ChronosItems.load();
		ChronosBullets.load();
		ChronosUnits.load();
		ChronosBlocks.load();

		TemporalDisable.init();
	}
}
