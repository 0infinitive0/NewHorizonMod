package newhorizon.content.nuke;

import newhorizon.expand.units.nuke.Warhead;

/**
 * Nukes: a factory that builds them inactive, a launcher that arms, aims and fires them, and four kinds of warhead. They are not part
 * of any faction.
 *
 * <p>Load order matters: statuses -> bullets -> warheads -> units -> blocks. The tech tree branches are attached from {@code NHTechTree}.
 */
public class NukeContent{
	public static void load(){
		NukeStatus.load();
		NukeBullets.load();
		Warhead.load();
		NukeUnits.load();
		NukeBlocks.load();
	}
}
