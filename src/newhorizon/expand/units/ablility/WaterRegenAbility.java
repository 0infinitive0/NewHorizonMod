package newhorizon.expand.units.ablility;

import arc.Core;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.Effect;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import newhorizon.content.jade.JadeFx;
import newhorizon.content.jade.JadeWater;

/** Heals the unit while it stands in water. Only the server heals: the result is synced to everyone else. */
public class WaterRegenAbility extends Ability{
	/** Health regained per second while in water, as a percentage of max health. */
	public float healPercentPerSecond = 5f;
	public Effect healEffect = JadeFx.leafBurst;

	@Override
	public void update(Unit unit){
		if(Vars.net.client() || unit.dead() || !unit.damaged()) return;
		if(!JadeWater.isWater(unit.floorOn())) return;

		unit.heal(unit.maxHealth * healPercentPerSecond / 100f * Time.delta / 60f);
		if(Mathf.chanceDelta(0.04f)) healEffect.at(unit.x + Mathf.range(unit.hitSize / 2f), unit.y + Mathf.range(unit.hitSize / 2f));
	}

	@Override
	public String localized(){
		return Core.bundle.get("ability.nh-waterregen", "Water Regeneration");
	}
}
