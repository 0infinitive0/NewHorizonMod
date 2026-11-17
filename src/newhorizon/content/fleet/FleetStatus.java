package newhorizon.content.fleet;

import arc.graphics.Color;
import mindustry.content.Fx;
import mindustry.type.StatusEffect;

/** Statuses of the Mothership and its Rammers. */
public class FleetStatus{
	/** Worn by a Rammer for life: it takes a quarter of the damage that gets through its armour. */
	public static StatusEffect bulwark;
	/** Given by the Mothership's speed projector to the allies near it: faster, and (as a Rammer's ram grows with its speed) hitting harder. */
	public static StatusEffect rally;

	public static void load(){
		bulwark = new StatusEffect("bulwark"){{
			color = Color.valueOf("c9d1e3");
			healthMultiplier = 4f;
			permanent = true;
			show = true;
		}};

		rally = new StatusEffect("rally"){{
			color = Color.valueOf("7fd8ff");
			speedMultiplier = 1.5f;
			damageMultiplier = 1.2f;
			effect = Fx.shootSmallSmoke;
			effectChance = 0.06f;
		}};
	}
}
