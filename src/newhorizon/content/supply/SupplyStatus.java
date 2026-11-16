package newhorizon.content.supply;

import arc.graphics.Color;
import mindustry.content.Fx;
import mindustry.entities.units.StatusEntry;
import mindustry.gen.Unit;
import mindustry.type.StatusEffect;
import newhorizon.expand.status.TurnLimit;

/**
 * Experimental status effects with negative values: Reverse is sprayed by the Reverse Sprayer turret, Mending Shots is given by the Status Towers. The game was not made for these, so they may do odd things:
 * <ul>
 * <li><b>Reverse</b>: a speed multiplier of -1, so the unit moves backwards (away from where it is trying to go).</li>
 * <li><b>Mending shots</b>: a damage multiplier of -1, so what the unit's bullets hit is healed instead of hurt (armour still takes its share,
 * the game works out damage as a positive number first, so a hit heals for a fraction of what it would have dealt).</li>
 * <li><b>Runaway</b>: three times as fast, but slow to turn (it keeps 30% of its turning speed) and slow to stop (a drag multiplier of 0.2, so it
 * slides on). Sprayed by the Reverse Sprayer from coal.</li>
 * </ul>
 */
public class SupplyStatus{
	public static StatusEffect reverse, mendingShots, runaway;

	public static void load(){
		TurnLimit.hook();
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

		runaway = new StatusEffect("runaway"){
			{
				color = Color.valueOf("ffb347");
				speedMultiplier = 3f;
				dragMultiplier = 0.2f;
				effect = Fx.fire;
				effectChance = 0.08f;
			}

			@Override
			public void update(Unit unit, StatusEntry entry){
				super.update(unit, entry);
				TurnLimit.limit(unit, 0.3f);
			}

			@Override
			public void onRemoved(Unit unit){
				super.onRemoved(unit);
				TurnLimit.forget(unit);
			}
		};
	}
}
