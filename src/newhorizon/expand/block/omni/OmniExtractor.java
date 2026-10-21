package newhorizon.expand.block.omni;

import arc.math.Mathf;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.blocks.power.Battery;
import newhorizon.content.nuke.NukeItems;

/**
 * Turns omni-matter into power at once. It is a battery: its power buffer is {@code consumePowerBuffered}, and each omni-matter that
 * comes in is spent straight away to fill {@link #energyPerItem} of it, but only if the buffer has room for all of that; otherwise the
 * item waits, so nothing is wasted. It needs no power to run.
 */
public class OmniExtractor extends Battery{
	/** Power units one omni-matter is worth. */
	public float energyPerItem = 10000f;

	public OmniExtractor(String name){
		super(name);
		update = true;
		hasItems = true;
		itemCapacity = 5;
		unloadable = false;
		buildType = OmniExtractorBuild::new;
	}

	public class OmniExtractorBuild extends BatteryBuild{
		@Override
		public boolean acceptItem(Building source, Item item){
			return item == NukeItems.omniMatter && items.get(item) < itemCapacity;
		}

		@Override
		public void updateTile(){
			if(!items.has(NukeItems.omniMatter) || consPower == null) return;
			float capacity = consPower.capacity;
			if(capacity <= 0f) return;
			//Only when the buffer has room for the whole of it.
			if((1f - power.status) * capacity + 0.01f >= energyPerItem){
				items.remove(NukeItems.omniMatter, 1);
				power.status = Mathf.clamp(power.status + energyPerItem / capacity);
			}
		}
	}
}
