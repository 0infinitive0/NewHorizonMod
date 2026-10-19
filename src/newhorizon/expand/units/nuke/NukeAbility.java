package newhorizon.expand.units.nuke;

import arc.Core;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;

/**
 * Makes a unit a nuke. It does nothing until it dies: whatever kills it (a shot, a command, running out of fuel, running into a
 * target) it explodes, see {@link NukeBlast}. An active nuke also detonates when its pilot fires, and burns out after {@link #fuel}
 * ticks. An inactive one cannot be detonated on purpose, only destroyed.
 */
public class NukeAbility extends Ability{
	public Warhead warhead;
	public boolean active;
	/** Ticks an active nuke flies before it detonates by itself. */
	public float fuel = 60f * 45f;

	protected float age;

	public NukeAbility(Warhead warhead, boolean active){
		this.warhead = warhead;
		this.active = active;
	}

	@Override
	public void update(Unit unit){
		if(!active || Vars.net.client()) return;

		age += Time.delta;
		if(age >= fuel || (unit.isPlayer() && unit.isShooting)) detonate(unit);
	}

	/** Detonates an active nuke. Nothing for an inactive one. */
	public boolean detonate(Unit unit){
		if(!active || unit.dead() || !unit.isAdded()) return false;
		unit.kill();
		return true;
	}

	@Override
	public void death(Unit unit){
		NukeBlast.explode(unit, warhead, active);
	}

	@Override
	public Ability copy(){
		//Warhead and mode are shared by every copy; only the fuel clock is per unit.
		NukeAbility copy = (NukeAbility)super.copy();
		copy.age = 0f;
		return copy;
	}

	@Override
	public String localized(){
		return Core.bundle.get(active ? "ability.nh-nuke-active" : "ability.nh-nuke-inactive", "Nuke");
	}
}
