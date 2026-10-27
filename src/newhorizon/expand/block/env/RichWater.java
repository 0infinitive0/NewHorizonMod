package newhorizon.expand.block.env;

import arc.graphics.Color;
import arc.struct.ObjectMap;
import mindustry.content.Blocks;
import mindustry.content.Liquids;
import mindustry.content.StatusEffects;
import mindustry.graphics.CacheLayer;
import mindustry.type.Item;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

/**
 * Deep water that is rich in one ore. It has no item drop of its own, so drills and miners ignore it;
 * only filters read {@link #richItem}.
 */
public class RichWater extends Floor{
	public static final ObjectMap<Item, RichWater> byItem = new ObjectMap<>();
	
	public final Item richItem;
	
	public RichWater(String name, Item item){
		super(name, 0);
		this.richItem = item;
		byItem.put(item, this);
		
		speedMultiplier = 0.2f;
		liquidDrop = Liquids.water;
		liquidMultiplier = 1.5f;
		isLiquid = true;
		status = StatusEffects.wet;
		statusDuration = 120f;
		drownTime = 200f;
		cacheLayer = CacheLayer.water;
		albedo = 0.9f;
		supportsOverlay = true;
		playerUnmineable = true;
		blendGroup = Blocks.deepwater;
		mapColor = Blocks.deepwater.mapColor.cpy().lerp(item.color, 0.6f);
	}
	
	/** @return the ore this tile is rich in, also while it is frozen, or null. */
	public static Item richItemOf(Tile tile){
		Floor floor = tile.floor();
		if(floor instanceof IceFloor)floor = ((IceFloor)floor).melted;
		return floor instanceof RichWater ? ((RichWater)floor).richItem : null;
	}
	
	/** @return whether the tile is, or is the frozen form of, water of any kind. */
	public static boolean isWater(Tile tile){
		Floor floor = tile.floor();
		if(floor instanceof IceFloor)return true;
		return floor.isLiquid && floor.liquidDrop == Liquids.water;
	}
}
