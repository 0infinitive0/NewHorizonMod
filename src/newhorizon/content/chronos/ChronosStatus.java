package newhorizon.content.chronos;

import mindustry.content.StatusEffects;
import mindustry.type.StatusEffect;

public class ChronosStatus{
	/** Time stops for the unit: it cannot move or shoot. Reapplied for as long as it stays in the tower's range. */
	public static StatusEffect stasis;
	/** Applied by Chronos cold weapons: a heavy slowdown and sluggish weapons. */
	public static StatusEffect temporalFreeze;
	/** Applied by disruptor shots: weapons are knocked offline for a moment. */
	public static StatusEffect dissonance;

	public static void load(){
		stasis = new StatusEffect("stasis"){{
			color = ChronosColor.violet;
			speedMultiplier = 0f;
			reloadMultiplier = 0f;
			dragMultiplier = 4f;
			disarm = true;
			effect = ChronosFx.disabled;
			effectChance = 0.1f;
		}};

		temporalFreeze = new StatusEffect("temporal-freeze"){{
			color = ChronosColor.chrono;
			speedMultiplier = 0.38f;
			reloadMultiplier = 0.55f;
			dragMultiplier = 1.6f;
			effect = mindustry.content.Fx.freezing;
			effectChance = 0.15f;
			init(() -> {
				opposite(StatusEffects.burning, StatusEffects.melting);
			});
		}};

		dissonance = new StatusEffect("dissonance"){{
			color = ChronosColor.gold;
			speedMultiplier = 0.85f;
			disarm = true;
			effect = ChronosFx.disabled;
			effectChance = 0.12f;
		}};
	}
}
