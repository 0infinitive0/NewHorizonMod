package newhorizon.expand.units.ablility;

import arc.Core;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import mindustry.type.StatusEffect;

/** Keeps a status effect on the unit for as long as it lives (the game has no field on a unit type for this). The effect should be {@code permanent}. */
public class KeepStatusAbility extends Ability{
	public StatusEffect effect;

	public KeepStatusAbility(StatusEffect effect){
		this.effect = effect;
	}

	@Override
	public void update(Unit unit){
		if(effect != null && !unit.hasEffect(effect)) unit.apply(effect, 1f);
	}

	@Override
	public void addStats(arc.scene.ui.layout.Table t){
		super.addStats(t);
		if(effect != null) t.add(effect.localizedName + ": " + effect.displayDescription()).wrap().width(descriptionWidth).row();
	}

	@Override
	public String localized(){
		return effect == null ? Core.bundle.get("ability.nh-keepstatus", "Armour") : effect.localizedName;
	}
}
