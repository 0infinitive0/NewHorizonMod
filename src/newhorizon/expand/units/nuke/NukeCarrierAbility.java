package newhorizon.expand.units.nuke;

import arc.Core;
import arc.math.Angles;
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
 * What makes a ship a mobile launcher: it holds up to {@link #slots} inactive nukes (loaded by the Dock command) and launches them, armed,
 * one at a time. The nukes are kept in the ability's saved number: five bits for the kind of each (its place in the list of inactive
 * nukes) and three for how many there are, so they survive a save. What a nuke had been set up with (a wall type, passengers) is not kept.
 */
public class NukeCarrierAbility extends Ability{
	public static final int maxSlots = 4;

	public int slots = maxSlots;
	/** Ticks between launches. */
	public float launchDelay = 90f;
	protected float timer;

	public int count(){
		return ((int)data >> 20) & 7;
	}

	public boolean free(){
		return count() < slots;
	}

	/** The kind of the nuke in a slot. */
	public UnitType type(int slot){
		int index = ((int)data >> (slot * 5)) & 31;
		return index < NukeUnits.inactive.size ? NukeUnits.inactive.get(index) : null;
	}

	/** Takes a nuke on board. Returns false if it is full or this is not an inactive nuke. */
	public boolean load(UnitType inactive){
		int index = NukeUnits.inactive.indexOf(t -> t == inactive);
		if(index < 0 || index > 31 || !free()) return false;
		int packed = (int)data, c = count();
		packed |= index << (c * 5);
		packed = (packed & ~(7 << 20)) | ((c + 1) << 20);
		data = packed;
		return true;
	}

	/** Takes the last nuke off, or null if there is none. */
	protected UnitType pop(){
		int c = count();
		if(c == 0) return null;
		UnitType type = type(c - 1);
		int packed = (int)data;
		packed &= ~(31 << ((c - 1) * 5));
		packed = (packed & ~(7 << 20)) | ((c - 1) << 20);
		data = packed;
		return type;
	}

	public boolean ready(){
		return timer <= 0f && count() > 0;
	}

	@Override
	public void update(Unit unit){
		timer = Math.max(0f, timer - Time.delta);
		//A pilot launches along the way the ship points by firing.
		if(unit.isPlayer() && unit.isShooting && ready()) launch(unit, null);
	}

	/** Launches the last nuke, armed, at the target (or straight ahead if there is none). Server side. Returns whether one went. */
	public boolean launch(Unit ship, Vec2 aim){
		if(Vars.net.client() || ship.dead() || !ready()) return false;
		//An enemy cloak hides its area from launchers.
		if(aim != null && NukeDefense.cloaked(ship.team, aim.x, aim.y)) return false;
		UnitType next = type(count() - 1);
		UnitType active = next == null ? null : NukeUnits.counterpart(next);
		if(active == null || !Units.canCreate(ship.team, active)) return false;

		pop();
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

	@Override
	public Ability copy(){
		NukeCarrierAbility copy = (NukeCarrierAbility)super.copy();
		copy.timer = 0f;
		return copy;
	}

	@Override
	public String localized(){
		return Core.bundle.get("ability.nh-nuke-carrier", "Nuke carrier");
	}
}
