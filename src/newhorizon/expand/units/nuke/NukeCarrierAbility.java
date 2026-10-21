package newhorizon.expand.units.nuke;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import mindustry.graphics.Layer;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Payloadc;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.UnitPayload;
import newhorizon.content.nuke.NukeFx;
import newhorizon.content.nuke.NukeUnits;

/**
 * What makes a ship a mobile launcher. It works like a Nuke Launcher: it holds one nuke, which an inactive nuke gets in by moving onto the
 * ship, arms it (the Arm command, or a pilot holding fire; it takes a few seconds and can be undone with Disarm) and launches it at a
 * target (the Launch command) or along the way the ship points (a pilot who keeps firing once it is armed). After a launch the ship needs a
 * while to reload.
 *
 * <p>Targeting works like a launcher's: the Select target command picks a point on the map (kept until Clear target), the Launch command fires
 * the nuke at it (straight ahead if none was picked).
 *
 * <p>Arming: the Armed stance (a toggle among the ship's buttons) keeps the nuke armed; a target given on the Launch command arms it by
 * itself if it was not armed yet, and fires as soon as it is.
 *
 * <p>What is aboard is kept in the ability's saved number, so it survives a save: the kind of nuke (its place in the list of inactive
 * nukes), whether it is armed, whether there is one at all, and the wall type a wall nuke was set to. Passengers of an infantry nuke are
 * kept in memory and go back into the nuke when it is launched; they are let out if the ship is destroyed, but they are not saved with the
 * ship: a ship loaded from a save has lost them.
 */
public class NukeCarrierAbility extends Ability{
	/** Ticks to arm (or disarm from fully armed) and to reload after a launch. */
	public float armTime = 150f, launchDelay = 240f;

	/** The part of the loaded nuke that is armed, 0 to 1. Not saved: a ship that is loaded from a save is armed or not. */
	public float armProgress;
	/** Whether the ship is arming now: the Armed stance is on, an order is waiting for the nuke to be ready, or a pilot is holding fire. */
	public boolean wantArmed;
	/** Until when an order (a target waiting for the nuke to be armed) keeps the arming going. */
	protected float orderUntil;
	protected float timer;
	/** The target picked for the ship, like a launcher's map target; null if none. Kept in memory only: a ship loaded from a save has none. */
	public Vec2 target;
	/** The units an infantry nuke was carrying when it came aboard. */
	protected Seq<Payload> cargo = new Seq<>();

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

	/** The wall a wall nuke aboard is set to, or null for the default (or for any other kind of nuke). */
	public Block wallType(){
		int id = ((int)data >> 7) & 4095;
		return id == 0 ? null : Vars.content.block(id - 1);
	}

	/** Takes a nuke of this kind on board, with nothing set up. Returns false if there is one already or this is not an inactive nuke. */
	public boolean load(UnitType inactive){
		int index = NukeUnits.inactive.indexOf(t -> t == inactive);
		if(index < 0 || index > 31 || loaded()) return false;
		data = 64 | index;
		cargo.clear();
		armProgress = 0f;
		wantArmed = false;
		return true;
	}

	/** Takes this nuke on board with what it has been set up with: its wall type and its passengers. The unit itself is not removed. */
	public boolean load(Unit nuke){
		if(!load(nuke.type)) return false;
		NukeAbility setup = NukeAI.abilityOf(nuke);
		if(setup != null && setup.data > 0f){
			Block wall = setup.wallType();
			if(wall != null && wall.id + 1 < 4096) data = (int)data | ((wall.id + 1) << 7);
		}
		if(nuke instanceof Payloadc){
			Payloadc carrier = (Payloadc)nuke;
			cargo.addAll(carrier.payloads());
			carrier.payloads().clear();
		}
		return true;
	}

	/** Takes the nuke off, whatever it was doing. */
	protected void unload(){
		data = 0f;
		cargo.clear();
		armProgress = 0f;
		wantArmed = false;
	}

	/** The passengers aboard, for tests and displays. */
	public Seq<Payload> cargo(){
		return cargo;
	}

	public boolean ready(){
		return armed() && timer <= 0f;
	}

	/** Picks the target. An enemy cloak hides its area: nothing can be picked there. Returns whether it was taken. */
	public boolean setTarget(Unit ship, Vec2 pos){
		if(pos == null || NukeDefense.cloaked(ship.team, pos.x, pos.y)) return false;
		target = pos.cpy();
		return true;
	}

	public void clearTarget(){
		target = null;
	}

	/** Keeps the arming going for this many more ticks, as an order for a launch does. */
	public void order(float ticks){
		orderUntil = Time.time + ticks;
	}

	@Override
	public void update(Unit unit){
		timer = Math.max(0f, timer - Time.delta);
		if(Vars.net.client()) return;
		//A cloak put up over the target after it was picked takes it away.
		if(target != null && NukeDefense.cloaked(unit.team, target.x, target.y)) target = null;

		if(!loaded()){
			armProgress = 0f;
			return;
		}

		//A pilot holds fire to arm and launch, and lets go to disarm, like at a launcher. Otherwise it arms while the Armed stance is on or a
		//launch order is waiting for it.
		if(unit.isPlayer()){
			wantArmed = unit.isShooting;
		}else{
			boolean stance = unit.controller() instanceof mindustry.ai.types.CommandAI && ((mindustry.ai.types.CommandAI)unit.controller()).hasStance(NukeUnits.carrierArmedStance);
			wantArmed = stance || Time.time < orderUntil;
		}

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

		//What the nuke was set up with goes with it.
		Block wall = wallType();
		Seq<Payload> passengers = new Seq<>(cargo);
		unload();
		float angle = aim != null ? ship.angleTo(aim.x, aim.y) : ship.rotation;
		Unit nuke = active.create(ship.team);
		nuke.set(ship.x + Angles.trnsx(angle, ship.hitSize / 2f), ship.y + Angles.trnsy(angle, ship.hitSize / 2f));
		nuke.rotation = angle;
		nuke.vel.trns(angle, active.speed * 0.6f);
		NukeAbility setup = NukeAI.abilityOf(nuke);
		if(wall != null && setup != null) setup.wallType(wall);
		if(nuke instanceof Payloadc) ((Payloadc)nuke).payloads().addAll(passengers);
		nuke.add();
		NukeUnits.fly(nuke, aim);

		timer = launchDelay;
		NukeFx.launch.at(nuke.x, nuke.y, angle);
		Sounds.shootMissileLarge.at(nuke.x, nuke.y, 1f, 0.8f);
		return true;
	}

	/** The picked target, drawn for the ship's own team: a line to it, a cross and the area the blast would cover. Only the host sees it. */
	@Override
	public void draw(Unit unit){
		if(target == null || Vars.player == null || unit.team != Vars.player.team()) return;
		UnitType inactive = type();
		UnitType active = inactive == null ? null : NukeUnits.counterpart(inactive);
		Warhead warhead = active == null ? null : NukeUnits.warheadOf(active);
		Color color = warhead == null ? Color.scarlet : warhead.color;
		float blast = warhead == null ? 60f : warhead.radius;

		Draw.z(Layer.overlayUI);
		Draw.color(color, 0.14f);
		Fill.circle(target.x, target.y, blast);
		Draw.color(color, 0.9f);
		Lines.stroke(1.6f);
		Lines.circle(target.x, target.y, blast);
		Lines.line(target.x - 10f, target.y, target.x + 10f, target.y);
		Lines.line(target.x, target.y - 10f, target.x, target.y + 10f);
		Lines.dashLine(unit.x, unit.y, target.x, target.y, Math.max(2, (int)(unit.dst(target) / 14f)));
		Draw.reset();
	}

	/** Whether this unit has this ability, and the ability. */
	public static NukeCarrierAbility of(Unit unit){
		for(Ability a : unit.abilities){
			if(a instanceof NukeCarrierAbility) return (NukeCarrierAbility)a;
		}
		return null;
	}

	/** A ship that is destroyed lets its passengers out. */
	@Override
	public void death(Unit unit){
		for(Payload payload : cargo){
			if(!(payload instanceof UnitPayload)) continue;
			Unit passenger = ((UnitPayload)payload).unit;
			passenger.set(unit.x + Mathf.range(unit.hitSize / 2f), unit.y + Mathf.range(unit.hitSize / 2f));
			passenger.vel.setZero();
			passenger.add();
		}
		cargo.clear();
	}

	@Override
	public Ability copy(){
		NukeCarrierAbility copy = (NukeCarrierAbility)super.copy();
		copy.cargo = new Seq<>();
		copy.timer = 0f;
		copy.armProgress = 0f;
		copy.wantArmed = false;
		copy.orderUntil = 0f;
		copy.target = null;
		return copy;
	}

	@Override
	public String localized(){
		return Core.bundle.get("ability.nh-nuke-carrier", "Nuke carrier");
	}
}
