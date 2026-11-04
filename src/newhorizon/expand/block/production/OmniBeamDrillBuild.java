package newhorizon.expand.block.production;

import mindustry.Vars;
import mindustry.type.Item;
import mindustry.world.Tile;
import mindustry.world.blocks.production.BeamDrill;
import newhorizon.content.nuke.NukeItems;
import newhorizon.expand.block.env.OmniDeposit;

/**
 * What a Plasma Bore or a Large Plasma Bore is made of, once it can mine omni-matter. The base game's bores already mine a deposit (a bore mines
 * the first solid tile in each lane in front of it that has something hard enough to mine, and a deposit is one), but nothing took anything off the
 * deposit, so a bore would have mined one for ever. Now each omni-matter a bore makes is taken off one of the deposit tiles it faces, as it is when
 * a unit mines: a bore makes one item per tile it faces per cycle, so each of those tiles loses one per cycle, and a tile that is used up goes.
 */
public class OmniBeamDrillBuild extends BeamDrill.BeamDrillBuild{
	private int cursor;

	public OmniBeamDrillBuild(BeamDrill block){
		block.super();
	}

	@Override
	public void produced(Item item){
		super.produced(item);
		if(item != NukeItems.omniMatter || Vars.net.client()) return;

		int deposits = 0;
		for(Tile tile : facing){
			if(OmniDeposit.isDeposit(tile)) deposits++;
		}
		if(deposits == 0) return;

		//The tile it turns to this time: every deposit tile it faces gets its turn in each cycle.
		int pick = Math.floorMod(cursor++, deposits);
		for(Tile tile : facing){
			if(!OmniDeposit.isDeposit(tile)) continue;
			if(pick-- == 0){
				OmniDeposit.mined(tile);
				return;
			}
		}
	}
}
