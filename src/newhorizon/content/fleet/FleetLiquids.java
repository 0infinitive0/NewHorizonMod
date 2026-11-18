package newhorizon.content.fleet;

import arc.graphics.Color;
import mindustry.content.Liquids;
import mindustry.content.StatusEffects;
import mindustry.type.Liquid;
import newhorizon.expand.liquid.CreepingLiquid;

/**
 * Napalm: a liquid that spreads like Neoplasm and burns like Oil. A puddle of it is not capped, so it keeps growing as it is fed, and it
 * creeps outward ({@link CreepingLiquid}), even under buildings, and nothing but fire clears it; it is thick, so it dries up slowly; and it is flammable enough that a fire on
 * it feeds on it and jumps from one puddle tile to the next. It does not burn by itself. The Napalm drone's bombs leave it.
 */
public class FleetLiquids{
	public static Liquid napalm;

	public static void load(){
		napalm = new CreepingLiquid("napalm", Color.valueOf("7a2e14")){{
			//like Neoplasm: spreads
			capPuddles = false;
			moveThroughBlocks = true;
			incinerable = false;
			blockReactive = false;
			viscosity = 0.85f;
			canStayOn.addAll(Liquids.neoplasm.canStayOn);
			//like Oil: burns
			flammability = 1.5f;
			explosiveness = 0.8f;
			temperature = 0.55f;
			heatCapacity = 0.4f;
			effect = StatusEffects.tarred;
			lightColor = Color.valueOf("ff8a3a");
		}};
	}
}
