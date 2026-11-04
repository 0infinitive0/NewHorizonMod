package newhorizon.expand.units.gravity;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Time;
import mindustry.Vars;
import mindustry.ai.types.CommandAI;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Call;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import newhorizon.content.nuke.GravityUnits;
import newhorizon.expand.units.modular.TeleportAbility;

/**
 * Collapse: the unit sacrifices {@link #costShare} of its max health, paid out of its shield (it needs that much shield, and keeps
 * whatever is over), to drop a black hole ({@link #hole}, which lasts as long as its lifetime, 30 s) on a spot of its choosing within
 * {@link #range}. There is one hole at a time. It is used by hand (the Collapse command, see {@link CollapseAI}) and by itself: a unit that
 * nobody commands (an enemy's) uses it once a second when it can pay and enough enemies are bunched within reach, and a unit that is under
 * orders does that only while its Auto collapse button is on.
 *
 * <p>Only the server does it; the clients see the hole.
 */
public class CollapseAbility extends Ability{
	/** The hole. */
	public BulletType hole;
	/** The share of the unit's max health that is paid, from the shield. */
	public float costShare = 1f;
	/** How far from the unit the hole can be dropped. */
	public float range = 560f;
	/** How often the automatic use looks, and how many enemies (by size, a unit counts its hit size over 8 tiles, at least one) must be inside one hole for it to be worth it. */
	public float autoInterval = 60f, autoWeight = 4f;
	public Color color = Color.valueOf("8a6bff");

	/** Ticks the unit's hole has left (0: none), and where it is. */
	public float holdLeft;
	public final Vec2 spot = new Vec2();
	protected float timer;

	@Override
	public Ability copy(){
		CollapseAbility copy = (CollapseAbility)super.copy();
		copy.holdLeft = 0f;
		copy.timer = 0f;
		return copy;
	}

	public static CollapseAbility of(Unit unit){
		if(unit == null) return null;
		for(Ability a : unit.abilities){
			if(a instanceof CollapseAbility) return (CollapseAbility)a;
		}
		return null;
	}

	public float cost(Unit unit){
		return unit.maxHealth * costShare;
	}

	public boolean holding(){
		return holdLeft > 0f;
	}

	/** Whether it could be used now: enough shield and no hole of its own. */
	public boolean ready(Unit unit){
		return !unit.dead && hole != null && !holding() && unit.shield >= cost(unit);
	}

	/** Where a hole toward a point would really go: at most {@link #range} from the unit. */
	public Vec2 target(Unit unit, Vec2 to){
		Vec2 d = new Vec2(to).sub(unit);
		if(d.len() > range) d.setLength(range);
		return d.add(unit);
	}

	/** Pays and drops the hole toward the point. Server side. Returns whether it did. */
	public boolean use(Unit unit, Vec2 to){
		if(Vars.net.client() || to == null || !ready(unit)) return false;
		Vec2 d = target(unit, to);
		unit.shield -= cost(unit);
		spot.set(d);
		holdLeft = hole.lifetime;
		//Made through the network call, so that everyone sees it; it has no owner, the hole only needs its team.
		Call.createBullet(hole, unit.team, d.x, d.y, 0f, -1f, 1f, 1f);
		Call.effect(TeleportAbility.out, unit.x, unit.y, unit.hitSize, color);
		Call.effect(TeleportAbility.in, d.x, d.y, holeRadius() * 0.3f, color);
		return true;
	}

	@Override
	public void update(Unit unit){
		if(holdLeft > 0f) holdLeft = Math.max(0f, holdLeft - Time.delta);
		if(Vars.net.client() || unit.dead || hole == null) return;
		timer += Time.delta;
		if(timer < autoInterval) return;
		timer = 0f;
		auto(unit);
	}

	/** Whether the unit may use it by itself: not a player's, and when it is under orders only with the Auto collapse button on. */
	protected boolean mayAuto(Unit unit){
		if(unit.isPlayer()) return false;
		if(unit.controller() instanceof CommandAI) return ((CommandAI)unit.controller()).hasStance(GravityUnits.autoCollapse);
		return true;
	}

	protected void auto(Unit unit){
		if(!mayAuto(unit) || !ready(unit)) return;
		float holeRadius = holeRadius();
		Vec2 best = null;
		float bestScore = 0f;
		//Every enemy within reach is a candidate centre; the score is how much enemy there is round it.
		var near = new arc.struct.Seq<Unit>();
		Units.nearbyEnemies(unit.team, unit.x - range - holeRadius, unit.y - range - holeRadius, (range + holeRadius) * 2f, (range + holeRadius) * 2f, u -> {
			if(!u.dead && u.isAdded() && u.targetable(unit.team)) near.add(u);
		});
		for(Unit c : near){
			if(!c.within(unit, range)) continue;
			float score = 0f;
			for(Unit u : near){
				if(u.within(c, holeRadius * 0.8f)) score += Math.max(1f, u.hitSize / 8f);
			}
			if(score > bestScore){
				bestScore = score;
				best = new Vec2(c.x, c.y);
			}
		}
		if(best != null && bestScore >= autoWeight) use(unit, best);
	}

	protected float holeRadius(){
		return hole instanceof newhorizon.expand.bullets.chronos.BlackHoleBulletType ? ((newhorizon.expand.bullets.chronos.BlackHoleBulletType)hole).radius : 200f;
	}

	@Override
	public void draw(Unit unit){
		if(!holding()) return;
		Draw.z(Layer.effect);
		Draw.color(color, 0.5f + 0.2f * Mathf.absin(6f, 1f));
		Lines.stroke(1.2f);
		Lines.dashLine(unit.x, unit.y, spot.x, spot.y, Math.max(2, (int)(unit.dst(spot) / 16f)));
		Draw.reset();
	}

	@Override
	public String localized(){
		return Core.bundle.format("ability.nh-collapse", (int)(costShare * 100f), (int)((hole == null ? 0f : hole.lifetime) / 60f));
	}
}
