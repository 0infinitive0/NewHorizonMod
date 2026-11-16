package newhorizon.content.supply;

import arc.graphics.Color;
import mindustry.content.Fx;
import mindustry.type.StatusEffect;

/**
 * Experimental status effects with negative values, delivered by the Status Towers. The game was not made for these, so they may do odd things:
 * <ul>
 * <li><b>Reverse</b>: a speed multiplier of -1, so the unit moves backwards (away from where it is trying to go).</li>
 * <li><b>Mending shots</b>: a damage multiplier of -1, so what the unit's bullets hit is healed instead of hurt (armour still takes its share,
 * the game works out damage as a positive number first, so a hit heals for a fraction of what it would have dealt).</li>
 * </ul>
 */
public class SupplyStatus{
	public static StatusEffect reverse, mendingShots;

	public static void load(){
		reverse = new StatusEffect("reverse"){{
			color = Color.valueOf("c07bff");
			speedMultiplier = -1f;
			effect = Fx.shootSmallSmoke;
			effectChance = 0.1f;
		}};

		mendingShots = new StatusEffect("mending-shots"){{
			color = Color.valueOf("7dffa0");
			damageMultiplier = -1f;
			effect = Fx.heal;
			effectChance = 0.08f;
		}};
	}
}
