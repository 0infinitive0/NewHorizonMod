package newhorizon.content.nuke;

import arc.Events;
import arc.graphics.Color;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.type.Item;
import mindustry.type.UnitType;
import newhorizon.expand.block.env.OmniDeposit;

/**
 * Omni-matter: what a siphon orb that lived long enough leaves behind, as solid deposits that can be mined but run out. They sit over
 * the ground like the wall ores of Erekir, over any ore there, and that comes back when a deposit is used up. Only units can mine them.
 */
public class NukeItems{
	public static Item omniMatter;
	public static OmniDeposit omniDeposit;

	public static void load(){
		omniMatter = new Item("omni-matter", Color.valueOf("c9a0ff")){{
			hardness = 3;
			cost = 1.8f;
		}};
		omniDeposit = new OmniDeposit("omni-matter-deposit", omniMatter);
		OmniDeposit.hook();

		//Units mine solid deposits only if they are allowed to mine walls (the units of Serpulo are not, by default). Once every unit of
		//every mod has been made, those that mine at all may mine this: nothing else in the game has a drop to be mined from a wall that
		//a unit could not mine before, other than the wall ores of Erekir.
		Events.on(EventType.ContentInitEvent.class, e -> {
			for(UnitType type : Vars.content.units()){
				if(type.mineSpeed > 0f && type.mineTier > 0) type.mineWalls = true;
			}
		});
	}
}
