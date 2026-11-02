package newhorizon.expand.block.env;

import arc.graphics.Color;
import mindustry.content.Liquids;
import mindustry.content.StatusEffects;
import mindustry.graphics.CacheLayer;
import mindustry.world.blocks.environment.Floor;

/**
 * The vent of a hot spot: a patch of molten rock in the sea. It does not freeze, burns what stands in it and lights the water
 * around it. A volcano erupts from here now and then, see {@link newhorizon.expand.game.HotSpotManager}.
 */
public class HotSpotFloor extends Floor{
	public HotSpotFloor(String name){
		super(name, 0);
		
		isLiquid = true;
		liquidDrop = Liquids.slag;
		status = StatusEffects.melting;
		statusDuration = 240f;
		speedMultiplier = 0.5f;
		variants = 0;
		supportsOverlay = false;
		cacheLayer = CacheLayer.slag;
		emitLight = true;
		lightRadius = 56f;
		lightColor = Color.valueOf("ff7a2a").a(0.6f);
		mapColor = Color.valueOf("c8501e");
		albedo = 0.9f;
	}
}
