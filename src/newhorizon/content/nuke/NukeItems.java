package newhorizon.content.nuke;

import arc.Events;
import arc.graphics.Color;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.type.Item;
import mindustry.world.blocks.production.Drill;
import newhorizon.expand.block.env.OmniDeposit;

/**
 * Omni-matter: what a siphon orb that lived long enough leaves behind, as ore that can be mined but runs out. Only units can mine it;
 * every drill is told to leave it alone, so the supply stays finite.
 */
public class NukeItems{
	public static Item omniMatter;
	public static OmniDeposit omniOre;

	public static void load(){
		omniMatter = new Item("omni-matter", Color.valueOf("c9a0ff")){{
			hardness = 3;
			cost = 1.8f;
		}};
		omniOre = new OmniDeposit("ore-omni-matter", omniMatter);
		OmniDeposit.hook();

		//Once every block of every mod has been made, no drill may take it.
		Events.on(EventType.ContentInitEvent.class, e -> {
			for(mindustry.world.Block block : Vars.content.blocks()){
				if(!(block instanceof Drill)) continue;
				Drill drill = (Drill)block;
				if(drill.blockedItems == null) drill.blockedItems = new arc.struct.Seq<>();
				if(!drill.blockedItems.contains(omniMatter)) drill.blockedItems.add(omniMatter);
			}
		});
	}
}
