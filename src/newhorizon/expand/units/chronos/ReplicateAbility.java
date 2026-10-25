package newhorizon.expand.units.chronos;

import arc.Core;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import newhorizon.content.chronos.ChronosFx;

/**
 * Splits off a copy of the unit every {@link #reload} ticks. The copy costs the parent a share of its health and starts with that
 * much, so a swarm cannot grow out of nothing, and nothing happens once the team has {@link #cap} of this kind (or the unit cap is
 * reached) or the parent is too hurt.
 */
public class ReplicateAbility extends Ability{
	public float reload = 60f * 12f;
	/** Most units of this kind the team may have. */
	public int cap = 18;
	/** Share of the parent's max health the copy takes, and the health share the parent must keep afterwards. */
	public float cost = 0.3f, keep = 0.4f;

	protected float timer;

	@Override
	public void update(Unit unit){
		if(Vars.net.client()) return;

		timer = Math.min(timer + Time.delta, reload);
		if(timer < reload || !canSplit(unit)) return;

		timer = 0f;
		split(unit);
	}

	public boolean canSplit(Unit unit){
		return unit.healthf() >= cost + keep && unit.team.data().countType(unit.type) < cap && Units.canCreate(unit.team, unit.type);
	}

	/** Makes the copy, if possible. */
	public Unit split(Unit unit){
		if(!canSplit(unit)) return null;

		Unit copy = unit.type.create(unit.team);
		copy.set(unit.x + Mathf.range(unit.hitSize), unit.y + Mathf.range(unit.hitSize));
		copy.rotation = unit.rotation;
		float share = unit.maxHealth * cost;
		unit.health -= share;
		copy.health = Math.min(copy.maxHealth, share);
		copy.add();
		ChronosFx.sparkBurst.at(unit.x, unit.y);
		return copy;
	}

	@Override
	public String localized(){
		return Core.bundle.get("ability.nh-replicate", "Replicate");
	}
}
