package newhorizon.expand.units.crab;

import newhorizon.expand.units.serpent.OmniArmorAbility;

/** The Serpent's stored damage, with room for a lot more and a blast that is a multiple of what was stored: see {@link OmniArmorAbility}. */
public class StoredDamageAbility extends OmniArmorAbility{
	@Override
	public String localized(){
		return arc.Core.bundle.get("ability.nh-stored-damage");
	}
}
