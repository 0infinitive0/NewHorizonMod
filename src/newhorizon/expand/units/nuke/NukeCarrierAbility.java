package newhorizon.expand.units.nuke;

import arc.Core;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
import newhorizon.content.nuke.NukeFx;
import newhorizon.content.nuke.NukeUnits;

/**
 * What makes a ship a mobile launcher. It works like a Nuke Launcher: it holds one nuke, which an inactive nuke gets in by moving onto the
 * ship, arms it (the Arm command, or a pilot holding fire; it takes a few seconds and can be undone with Disarm) and launches it at a
 * target (the Launch command) or along the way the ship points (a pilot who keeps firing once it is armed). After a launch the ship needs a
 * while to reload.
 *
 * <p>What is aboard is kept in the ability's saved number, so it survives a save: the kind of nuke (its place in the list of inactive
 * nukes), whether it is armed, and whether there is one at all. What a nuke had been set up with (a wall type, passengers) is not kept.
 */
public class NukeCarrierAbility extends Ability{
	/** Ticks to arm (or disarm from fully armed) and to reload after a launch. */
	public float armTime = 150f, launchDelay = 240f;

	/** The part of the loaded nuke that is armed, 0 to 1. Not saved: a ship that is loaded from a save is armed or not. */
	public float armProgress;
	/** What the ship has been told: whether to arm. */
	public boolean wantArmed;
	protected float timer;

	public boolean loaded(){
		return ((int)data & 64) != 0;
	}

	public boolean armed(){
		return loaded() && ((int)data & 32) != 0;
	}

	/** The kind of the nuke aboard, in its inactive form. */
	public UnitType type(){
		if(!loaded()) return null;
		int index = (int)data & 31;
		return index < NukeUnits.inactive.size ? NukeUnits.inactive.get(index) : null;
	}

	public boolean free(){
		return !loaded();
	}

	/** Takes a nuke on board. Returns false if there is one already or this is not an inactive nuke. */
	public boolean load(UnitType inactive){
		int index = NukeUnits.inactive.indexOf(t -> t == inactive);
		if(index < 0 || index > 31 || loaded()) return false;
		data = 64 | index;
		armProgress = 0f;
		wantArmed = false;
		return true;
	}

	/** Takes the nuke off, whatever it was doing. */
	protected void unload(){
		data = 0f;
		armProgress = 0f;
		wantArmed = false;
	}

	public boolean ready(){
		return armed() && timer <= 0f;
	}

	@Override
	public void update(Unit unit){
		timer = Math.max(0f, timer - Time.delta);
		if(Vars.net.client()) return;

		if(!loaded()){
			armProgress = 0f;
			return;
		}

		//A pilot holds fire to arm and launch, and lets go to disarm, like at a launcher.
		if(unit.isPlayer()) wantArmed = unit.isShooting;

		armProgress = Mathf.approach(armProgress, wantArmed ? 1f : 0f, Time.delta / armTime);
		int bits = (int)data;
		if(armProgress >= 1f && (bits & 32) == 0){
			data = bits | 32;
			NukeFx.armed.at(unit.x, unit.y);
		}else if(armProgress <= 0f && (bits & 32) != 0){
			data = bits & ~32;
		}

		if(unit.isPlayer() && wantArmed && ready()) launch(unit, null);
	}

	/** Launches the nuke, armed, at the target (or straight ahead if there is none). Server side. Returns whether it went. */
	public boolean launch(Unit ship, Vec2 aim){
		if(Vars.net.client() || ship.dead() || !ready()) return false;
		//An enemy cloak hides its area from launchers.
		if(aim != null && NukeDefense.cloaked(ship.team, aim.x, aim.y)) return false;
		UnitType inactive = type();
		UnitType active = inactive == null ? null : NukeUnits.counterpart(inactive);
		if(active == null || !Units.canCreate(ship.team, active)) return false;

		unload();
		float angle = aim != null ? ship.angleTo(aim.x, aim.y) : ship.rotation;
		Unit nuke = active.create(ship.team);
		nuke.set(ship.x + Angles.trnsx(angle, ship.hitSize / 2f), ship.y + Angles.trnsy(angle, ship.hitSize / 2f));
		nuke.rotation = angle;
		nuke.vel.trns(angle, active.speed * 0.6f);
		nuke.add();
		NukeUnits.fly(nuke, aim);

		timer = launchDelay;
		NukeFx.launch.at(nuke.x, nuke.y, angle);
		Sounds.shootMissileLarge.at(nuke.x, nuke.y, 1f, 0.8f);
		return true;
	}

	/** Whether this unit has this ability, and the ability. */
	public static NukeCarrierAbility of(Unit unit){
		for(Ability a : unit.abilities){
			if(a instanceof NukeCarrierAbility) return (NukeCarrierAbility)a;
		}
		return null;
	}

	@Override
	public Ability copy(){
		NukeCarrierAbility copy = (NukeCarrierAbility)super.copy();
		copy.timer = 0f;
		copy.armProgress = 0f;
		copy.wantArmed = false;
		return copy;
	}

	@Override
	public String localized(){
		return Core.bundle.get("ability.nh-nuke-carrier", "Nuke carrier");
	}
}
