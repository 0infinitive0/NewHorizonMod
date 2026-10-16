package newhorizon.content.jade;

import mindustry.content.Fx;
import mindustry.entities.units.StatusEntry;
import mindustry.gen.Unit;
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
	/**
	 * Worn by a unit a Mimic turned into. Never times out. Holds the unit's health and armor at the Mimic's: the unit has the
	 * weapons and abilities of its new type, but not its toughness. Enforced every frame, so it also survives saving and loading,
	 * as a unit's statuses are saved with it.
	 */
	public static StatusEffect mimicked;

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

		mimicked = new StatusEffect("mimicked"){
			{
				color = JadeColor.jadeLight;
				permanent = true;
				effect = JadeFx.leafBurst;
				effectChance = 0.015f;
			}

			@Override
			public void update(Unit unit, StatusEntry entry){
			    float time = entry.time;
				super.update(unit, entry);

				float health = newhorizon.expand.units.mimic.Mimicry.health();
				if(unit.maxHealth != health){
					unit.maxHealth = health;
					unit.health = Math.min(unit.health, health);
				}
				unit.armor = newhorizon.expand.units.mimic.Mimicry.armor();
			}
		};
	}
}
