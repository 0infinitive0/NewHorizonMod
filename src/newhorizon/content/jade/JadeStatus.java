package newhorizon.content.jade;

import mindustry.content.Fx;
import mindustry.type.StatusEffect;

public class JadeStatus{
	/** Applied by Jade weapons: spores slow the target and sap its health. */
	public static StatusEffect bloom;
	/** Applied by Jade support units: slowly mends whatever carries it. */
	public static StatusEffect regrowth;
	/**
	 * Applied by the Sporeling. Never times out: it ends only when the host dies (spawning Sporelings, see
	 * {@link JadeInfection}) or is healed back to full health. Deals damage over time.
	 */
	public static StatusEffect infested;

	public static void load(){
		bloom = new StatusEffect("bloom"){{
			color = JadeColor.jade;
			speedMultiplier = 0.82f;
			healthMultiplier = 0.9f;
			damage = 0.25f;
			effect = Fx.sporeSlowed;
			effectChance = 0.1f;
		}};

		regrowth = new StatusEffect("regrowth"){{
			color = JadeColor.jadeLight;
			damage = -0.1f;
			healthMultiplier = 1.1f;
			effect = JadeFx.leafBurst;
			effectChance = 0.06f;
		}};

		infested = new StatusEffect("infested"){{
			color = JadeColor.infection;
			permanent = true;
			damage = 0.3f;
			speedMultiplier = 0.92f;
			effect = Fx.sporeSlowed;
			effectChance = 0.12f;
		}};
	}
}
