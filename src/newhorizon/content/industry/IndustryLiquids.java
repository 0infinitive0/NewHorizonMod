package newhorizon.content.industry;

import arc.graphics.Color;
import mindustry.content.StatusEffects;
import mindustry.type.Liquid;
import newhorizon.expand.block.industry.AntiMatterLiquid;
import newhorizon.expand.block.industry.PlasmaLiquid;

/**
 * The Fusion Reactor's byproducts.
 * <ul>
 *     <li>{@link #plasma}: super hot. A puddle of it sets fires and melts what stands in it.</li>
 *     <li>{@link #antiMatter}: explodes if it leaks out into the world (see {@link AntiMatterLiquid}). It is the Anti-Matter Cannon's ammo.</li>
 * </ul>
 */
public class IndustryLiquids{
	public static Liquid plasma, antiMatter;

	public static void load(){
		plasma = new PlasmaLiquid("plasma", Color.valueOf("ff7bd0")){{
			temperature = 5f;
			flammability = 1f;
			heatCapacity = 0.2f;
			viscosity = 0.1f;
			effect = StatusEffects.melting;
			lightColor = Color.valueOf("ff9be0");
		}};

		antiMatter = new AntiMatterLiquid("anti-matter", Color.valueOf("b9f4ff")){{
			explosiveness = 12f;
			temperature = 0.5f;
			heatCapacity = 0.4f;
			viscosity = 0.3f;
			lightColor = Color.valueOf("b9f4ff");
		}};
	}
}
