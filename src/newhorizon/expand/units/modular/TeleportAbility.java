package newhorizon.expand.units.modular;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Time;
import mindustry.Vars;
import mindustry.ai.types.CommandAI;
import mindustry.content.StatusEffects;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Call;
import mindustry.gen.Legsc;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import newhorizon.content.nuke.ModularUnits;

/**
 * A teleport drive, short ({@link #longRange} false: instant, short range, quick to recharge) or long (a charge during which the frame is
 * held still, then a jump of a great distance, and a long recharge). Both are used by hand: the Blink and Jump commands, and a click on
 * the map for where to go ({@link TeleportAI}); a click farther than the drive's range sends the frame as far as the range goes, in that
 * direction. The short drive is also used by itself by a frame that no player is flying (see {@link #auto}): to get away when it is
 * taking a lot of damage, and to cross a stretch of ground it has been ordered over that is much longer than its range. A frame that is
 * commanded (as opposed to run by an enemy AI) only does that while its Auto blink button is on.
 *
 * <p>Only the server moves the frame; the clients see it arrive.
 */
public class TeleportAbility extends Ability{
	public boolean longRange;
	public float range = 144f, cooldown = 480f, charge;
	public Color color = Color.valueOf("8ad4ff");

	/** Ticks until the drive is ready again. */
	public float timer;
	/** Ticks of charge left while a jump is being prepared. */
	public float charging;
	public boolean isCharging;
	public Vec2 dest = new Vec2();

	/** What the frame has lost lately (decaying), for the automatic use. */
	protected float recent, lastHealth = -1f;

	/** The flash where the frame leaves and where it arrives, drawn at its size. */
	public static Effect out, in;

	static{
		out = new Effect(26f, 400f, e -> {
			Draw.z(Layer.effect + 1f);
			float r = e.rotation * (0.6f + 1.4f * e.fin(Interp.pow3Out));
			Draw.color(Color.white, e.color, e.fin());
			Lines.stroke(3f * e.fout());
			Lines.circle(e.x, e.y, r);
			Draw.color(e.color, 0.5f * e.fout());
			arc.graphics.g2d.Fill.circle(e.x, e.y, r * 0.8f * e.fout());
			Draw.reset();
		});
		in = new Effect(30f, 400f, e -> {
			Draw.z(Layer.effect + 1f);
			float r = e.rotation * (2f - 1.4f * e.fin(Interp.pow3Out));
			Draw.color(Color.white, e.color, e.fin());
			Lines.stroke(3f * e.fout() + 0.5f);
			Lines.circle(e.x, e.y, r);
			Draw.color(Color.white, 0.8f * e.fout());
			arc.graphics.g2d.Fill.circle(e.x, e.y, e.rotation * 0.5f * e.fout());
			Draw.reset();
		});
	}

	/** The unit's short or long drive, or null if it has none. */
	public static TeleportAbility of(Unit unit, boolean longRange){
		if(unit == null) return null;
		for(Ability a : unit.abilities){
			if(a instanceof TeleportAbility && ((TeleportAbility)a).longRange == longRange) return (TeleportAbility)a;
		}
		return null;
	}

	@Override
	public Ability copy(){
		TeleportAbility copy = (TeleportAbility)super.copy();
		copy.dest = new Vec2();
		copy.timer = 0f;
		copy.charging = 0f;
		copy.isCharging = false;
		copy.recent = 0f;
		copy.lastHealth = -1f;
		return copy;
	}

	public boolean ready(){
		return timer <= 0f && !isCharging;
	}

	/** Where a teleport toward a point would really end up: at most {@link #range} away, and inside the map. */
	public Vec2 target(Unit unit, Vec2 to){
		Vec2 d = new Vec2(to).sub(unit);
		if(d.len() > range) d.setLength(range);
		d.add(unit);
		float m = 16f;
		if(Vars.world != null && Vars.world.width() > 0){
			d.x = Mathf.clamp(d.x, m, Vars.world.unitWidth() - m);
			d.y = Mathf.clamp(d.y, m, Vars.world.unitHeight() - m);
		}
		return d;
	}

	/** Starts a teleport toward the point: at once for a short drive, after the charge for a long one. Server side. */
	public boolean start(Unit unit, Vec2 to){
		if(Vars.net.client() || unit == null || unit.dead || !ready() || to == null) return false;
		Vec2 d = target(unit, to);
		if(d.dst(unit) < unit.hitSize) return false;
		dest.set(d);
		if(charge <= 0f){
			go(unit);
		}else{
			isCharging = true;
			charging = charge;
		}
		return true;
	}

	protected void go(Unit unit){
		float ox = unit.x, oy = unit.y;
		Call.effect(out, ox, oy, unit.hitSize, color);
		unit.set(dest.x, dest.y);
		unit.vel.setZero();
		if(unit instanceof Legsc) ((Legsc)unit).resetLegs();
		Call.effect(in, dest.x, dest.y, unit.hitSize, color);
		unit.snapInterpolation();
		timer = cooldown;
		recent = 0f;
	}

	@Override
	public void update(Unit unit){
		timer = Math.max(0f, timer - Time.delta);
		if(Vars.net.client()) return;

		if(isCharging){
			if(unit.dead){
				isCharging = false;
				return;
			}
			//Held still while the drive charges.
			unit.apply(StatusEffects.unmoving, 4f);
			unit.vel.setZero();
			charging -= Time.delta;
			if(charging <= 0f){
				isCharging = false;
				go(unit);
			}
			return;
		}
		if(!longRange) auto(unit);
	}

	/** The short drive's own use, for a frame nobody is flying. */
	protected void auto(Unit unit){
		float hp = unit.health + unit.shield;
		if(lastHealth < 0f) lastHealth = hp;
		recent = recent * 0.985f + Math.max(0f, lastHealth - hp);
		lastHealth = hp;

		if(unit.dead || unit.isPlayer() || !ready()) return;
		//A frame that is under orders only does it when its Auto blink button is on; an enemy's has no buttons.
		if(unit.controller() instanceof CommandAI){
			CommandAI commander = (CommandAI)unit.controller();
			if(!commander.hasStance(ModularUnits.autoBlink)) return;

			//Across a long stretch it was sent over.
			if(commander.targetPos != null && commander.attackTarget == null && unit.dst(commander.targetPos) > range * 1.6f){
				start(unit, commander.targetPos);
				return;
			}
		}

		//Away from what is hurting it.
		if(recent > unit.maxHealth * 0.12f){
			Unit foe = Units.closestEnemy(unit.team, unit.x, unit.y, 900f, u -> !u.dead());
			Vec2 away = foe == null ? new Vec2(1f, 0f).setAngle(unit.rotation + 180f) : new Vec2().set(unit).sub(foe).nor();
			if(away.isZero()) away.set(1f, 0f);
			Vec2 to = new Vec2().set(unit).add(away.scl(range));
			if(start(unit, to)) recent = 0f;
		}
	}

	@Override
	public void draw(Unit unit){
		if(!isCharging) return;
		float f = 1f - charging / Math.max(1f, charge);
		Draw.z(Layer.effect);
		Draw.color(color, 0.8f);
		Lines.stroke(1.5f + 2f * f);
		Lines.circle(unit.x, unit.y, unit.hitSize * (1.6f - 0.9f * f));
		Lines.stroke(1f);
		Lines.dashLine(unit.x, unit.y, dest.x, dest.y, Math.max(2, (int)(unit.dst(dest) / 14f)));
		Lines.circle(dest.x, dest.y, unit.hitSize * (0.4f + 0.8f * f));
		Draw.reset();
	}

	@Override
	public String localized(){
		return Core.bundle.get(longRange ? "ability.nh-jump" : "ability.nh-blink", longRange ? "Jump drive" : "Blink drive");
	}
}
