package newhorizon.expand.block.industry;

import newhorizon.expand.block.chronos.ChronoRelay;

/**
 * The Item Teleporter: whatever is put into one comes out of another one set to the same channel, at once and over any distance
 * (the Chronos relay's behaviour, see {@link ChronoRelay}), with a bigger buffer and an output that hands items on every tick instead
 * of every fifth, so that it keeps up with a plastanium conveyor.
 */
public class ItemTeleporter extends ChronoRelay{
	public ItemTeleporter(String name){
		super(name);
		itemCapacity = 60;
		dumpTime = 1;
		buildType = ItemTeleporterBuild::new;
	}

	public class ItemTeleporterBuild extends ChronoRelayBuild{
	}
}
