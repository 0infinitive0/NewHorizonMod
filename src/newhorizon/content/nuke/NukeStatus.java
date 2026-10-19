package newhorizon.content.nuke;

import arc.graphics.Color;
import mindustry.content.Fx;
import mindustry.type.StatusEffect;

public class NukeStatus{
	/** Radioactive fallout: constant damage and sluggishness. */
	public static StatusEffect irradiated;
	/** Hit by an electromagnetic pulse: nearly stopped, weapons offline. */
	public static StatusEffect empShock;
	/** Frozen in time and disarmed: cannot move or shoot. */
	public static StatusEffect temporalLock;

	public static void load(){
		irradiated = new StatusEffect("irradiated"){{
			color = Color.valueOf("9bff4a");
			damage = 0.45f;
			speedMultiplier = 0.8f;
			healthMultiplier = 0.85f;
			effect = Fx.sporeSlowed;
			effectChance = 0.12f;
		}};

		empShock = new StatusEffect("emp-shock"){{
			color = Color.valueOf("7fd8ff");
			speedMultiplier = 0.35f;
			reloadMultiplier = 0f;
			disarm = true;
			effect = Fx.shootSmallSmoke;
			effectChance = 0.1f;
		}};

		temporalLock = new StatusEffect("temporal-lock"){{
			color = Color.valueOf("2b9bff");
			speedMultiplier = 0f;
			reloadMultiplier = 0f;
			dragMultiplier = 5f;
			disarm = true;
			effect = Fx.freezing;
			effectChance = 0.06f;
		}};
	}
}
