package newhorizon.expand.units.ablility;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectFloatMap;
import arc.struct.Seq;
import arc.util.Strings;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.abilities.Ability;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;

/**
 * The Ram aura: a unit that is moving fast hurts everything of another team that it touches, and the faster it moves the harder it hits.
 *
 * <p>Speed is counted as a fraction of the unit's own top speed ({@code velocity / type.speed}), so a unit that is still speeding up hits weakly
 * and one that a status makes faster (a speed buff raises the top speed it is measured against only through the speed it really reaches)
 * hits harder. Below {@link #minFraction} nothing happens. From there the damage of one hit rises in a straight line from {@link #minDamage} to
 * {@link #maxDamage} at top speed, and above top speed it is multiplied by the fraction (up to {@link #overspeedCap}).
 *
 * <p>It touches everything inside a ring a little wider than the unit: units (flying and walking) and buildings. A target is hit once every
 * {@link #hitCooldown} ticks, so one it stays inside is not shredded every tick. Units are knocked away. <b>Every hit costs the unit itself</b>
 * {@link #selfRatio} of the damage it dealt, straight off its health (armour and status effects cannot soften it), but never its last point of
 * health: ramming alone cannot kill it.
 */
public class RamAuraAbility extends Ability{
	/** The fraction of top speed below which it does nothing. */
	public float minFraction = 0.4f;
	/** The damage of a hit at {@link #minFraction} and at top speed. */
	public float minDamage = 30f, maxDamage = 350f;
	/** The fastest it counts as being, as a fraction of top speed (a speed buff can take it past 1). */
	public float overspeedCap = 1.5f;
	/** Ticks before the same target can be hit again. */
	public float hitCooldown = 20f;
	/** How far outside the unit's body the ring reaches. */
	public float ringPad = 8f;
	/** The part of the damage it deals that it takes itself. */
	public float selfRatio = 0.15f;
	/** How hard units are knocked away at top speed (a speed change, in px a tick). */
	public float knockback = 6f;
	public Color color = Color.valueOf("ff9a4d");
	public Effect hitEffect = Fx.hitLancer;

	/** Targets that were hit and when they can be hit again (by identity: units and buildings). */
	protected ObjectFloatMap<Object> hits = new ObjectFloatMap<>();
	protected float cleanTimer;
	/** What happened last: for the tests and for readouts. */
	public float lastDamage, totalDealt, totalSelf;
	public int hitCount;

	/** Every unit gets a copy of the ability, and a copy must not share the list of targets. */
	@Override
	public Ability copy(){
		RamAuraAbility c = (RamAuraAbility)super.copy();
		c.hits = new ObjectFloatMap<>();
		return c;
	}

	/** The unit's speed as a fraction of its top speed. */
	public float fraction(Unit unit){
		return unit.vel.len() / Math.max(0.0001f, unit.type.speed);
	}

	/** The damage of one hit at a speed fraction (0 below the threshold). */
	public float damageAt(float fraction){
		if(fraction < minFraction) return 0f;
		float f = Math.min(fraction, overspeedCap);
		float d = Mathf.lerp(minDamage, maxDamage, Mathf.clamp((f - minFraction) / Math.max(0.0001f, 1f - minFraction)));
		return f > 1f ? d * f : d;
	}

	public float ringRadius(Unit unit){
		return unit.hitSize / 2f + ringPad;
	}

	@Override
	public void update(Unit unit){
		if(Vars.net.client() || !unit.isValid()) return;

		cleanTimer += Time.delta;
		if(cleanTimer >= 180f){
			cleanTimer = 0f;
			Seq<Object> old = new Seq<>();
			for(var e : hits) if(e.value <= Time.time) old.add(e.key);
			old.each(k -> hits.remove(k, 0f));
		}

		float f = fraction(unit);
		float dmg = damageAt(f);
		if(dmg <= 0f) return;

		float r = ringRadius(unit);
		float scan = r + 60f;
		Groups.unit.intersect(unit.x - scan, unit.y - scan, scan * 2f, scan * 2f, u -> {
			if(u == unit || u.team == unit.team || u.team == Team.derelict || !u.isValid() || !u.targetable(unit.team)) return;
			if(!u.within(unit, r + u.hitSize / 2f) || !ready(u)) return;
			hit(unit, u, u.x, u.y);
			u.damage(dmg);
			float push = knockback * f * Math.min(1f, unit.mass() / Math.max(1f, u.mass()));
			if(push > 0f && u.isValid()) u.vel.add(Tmp.v1.set(u.x - unit.x, u.y - unit.y).setLength(push));
			pay(unit, dmg);
		});
		Vars.indexer.allBuildings(unit.x, unit.y, scan, b -> {
			if(b.team == unit.team || b.team == Team.derelict || !b.isValid()) return;
			if(!b.within(unit, r + b.hitSize() / 2f) || !ready(b)) return;
			hit(unit, b, b.x, b.y);
			b.damage(unit.team, dmg);
			pay(unit, dmg);
		});
	}

	/** Whether a target may be hit now. */
	protected boolean ready(Object target){
		return hits.get(target, 0f) <= Time.time;
	}

	protected void hit(Unit unit, Object target, float x, float y){
		hits.put(target, Time.time + hitCooldown);
		hitCount++;
		hitEffect.at(x, y, Mathf.angle(x - unit.x, y - unit.y), color);
	}

	/** What a hit costs the unit: health taken directly (nothing softens it), never taking it below one point. */
	protected void pay(Unit unit, float dealt){
		lastDamage = dealt;
		totalDealt += dealt;
		float cost = dealt * selfRatio;
		float room = unit.health - 1f;
		if(cost <= 0f || room <= 0f) return;
		float taken = Math.min(cost, room);
		totalSelf += taken;
		//straight off its health: even piercing damage is divided by the unit's health multiplier (the Rammer's Bulwark) and the rules' unit health
		unit.health -= taken;
	}

	@Override
	public void draw(Unit unit){
		float f = fraction(unit);
		if(f < minFraction) return;
		float a = Mathf.clamp((f - minFraction) / (1f - minFraction));
		Draw.z(Layer.effect);
		Draw.color(color, 0.12f + 0.28f * a);
		Lines.stroke(1.2f + a);
		Lines.circle(unit.x, unit.y, ringRadius(unit));
		Draw.reset();
	}

	@Override
	public void addStats(Table t){
		super.addStats(t);
		t.add(Core.bundle.format("ability.nh-ramaura.stats", (int)(minFraction * 100f), (int)minDamage, (int)maxDamage, Strings.fixed(hitCooldown / 60f, 2), (int)(selfRatio * 100f), (int)(overspeedCap * 100f))).wrap().width(descriptionWidth).row();
	}

	@Override
	public String localized(){
		return Core.bundle.get("ability.nh-ramaura", "Ram aura");
	}
}
