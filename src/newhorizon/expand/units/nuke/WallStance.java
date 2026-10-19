package newhorizon.expand.units.nuke;

import arc.scene.style.TextureRegionDrawable;
import mindustry.ai.UnitStance;
import mindustry.world.Block;

/**
 * Picks the wall a wall nuke will fill with. It appears among the stance buttons of a selected inactive wall nuke, one per kind of
 * one-tile wall, and only one can be on at a time. The nuke reads it, see {@link NukeAbility}.
 */
public class WallStance extends UnitStance{
	public final Block block;

	public WallStance(Block block){
		super("nuke-wall-" + block.name, "", null);
		this.block = block;
	}

	@Override
	public String localized(){
		return block.localizedName;
	}

	@Override
	public TextureRegionDrawable getIcon(){
		return new TextureRegionDrawable(block.uiIcon);
	}
}
