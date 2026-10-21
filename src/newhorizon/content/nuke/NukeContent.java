package newhorizon.content.nuke;

import newhorizon.expand.units.nuke.Warhead;

/**
 * Nukes: a factory that builds them inactive, a launcher that arms, aims and fires them, and four kinds of warhead. They are not part
 * of any faction.
 *
 * <p>Load order matters: statuses -> warheads -> bullets -> units -> blocks. The tech tree branches are attached from {@code NHTechTree}.
 */
public class NukeContent{
	public static void load(){
		NukeStatus.load();
		NukeItems.load();
		Warhead.load();
		NukeBullets.load();
		NukeUnits.load();
		NukeBlocks.load();
		OmniBlocks.load();
		//The detected nukes are drawn on the minimap once the game is up.
		newhorizon.expand.block.nuke.NukeMinimap.hook();
	}
}
