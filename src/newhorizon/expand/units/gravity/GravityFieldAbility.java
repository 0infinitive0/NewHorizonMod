package newhorizon.expand.units.gravity;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.ObjectFloatMap;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.bullet.ContinuousBulletType;
import mindustry.entities.bullet.LaserBulletType;
import mindustry.entities.bullet.LightningBulletType;
import mindustry.entities.bullet.PointBulletType;
import mindustry.entities.bullet.PointLaserBulletType;
import mindustry.entities.bullet.RailBulletType;
import mindustry.entities.bullet.SapBulletType;
import mindustry.entities.bullet.ShrapnelBulletType;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;

/**
 * The Gravity Base's field: a disc of {@link #radius} round the unit in which
 * <ul>
 * <li>enemy units revolve round the unit and fall steadily toward it, down to a certain radius (a unit that cannot be pushed,
 * {@code knockbackMultiplier} 0, does neither);</li>
 * <li>enemy bullets are caught and made to revolve too, and the moment one is caught it changes to the unit's team, so it circles inside
 * the field (spiralling in to the same radius) hitting the enemy that is in it; bullets faster than {@link #maxCatchSpeed} keep their team but their path is bent round the
 * unit; lasers and instant hits are not affected at all;</li>
 * <li>everything of the enemy's (units, and buildings at a lower rate) is crushed: damage over time that goes past armor and is
 * stronger toward the centre;</li>
 * </ul>
 * and whenever an enemy unit or building is destroyed near the unit, the unit steals a shield worth {@link #shieldShare} of the victim's max health, up to
 * {@link #shieldCap} times its own.
 */
public class GravityFieldAbility extends Ability{
	/** How far the field reaches. */
	public float radius = 200f;
	/** How fast enemy units revolve, in degrees a tick (for a unit that can be pushed fully). */
	public float spin = 0.55f;
	/** The steady fall inward: units that can be pushed fall this many px a tick toward the unit, bullets that were caught this many, and a bent (fast) bullet leans {@link #bendInward} degrees inward; none of them falls closer than {@link #innerRadius} (for a unit, plus half its own size). */
	public float fall = 0.7f, bulletFall = 0.5f, bendInward = 25f, innerRadius = 60f;

	/** Bullets faster than this (px a tick) are not caught: they keep their team, and their path is bent toward going round the unit by {@link #bendRate} degrees a tick. Instant ones are not touched. */
	public float maxCatchSpeed = 10f, bendRate = 4f;
	/** A caught bullet circles at least this fast (px a tick) and for at least this long (ticks). */
	public float minOrbitSpeed = 3f, minBulletLife = 300f;
	/** The most bullets caught in a tick. */
	public int catchPerTick = 80;

	/** Crush: damage a second to a unit at the edge of the field, how many times more it is at the centre, and the share that buildings take. */
	public float crushDamage = 225f, crushCentre = 3f, crushBuildings = 0.45f;
	public float crushInterval = 5f;

	/** The victim of a death counts if it is within {@code radius * shieldRange} of the unit; the shield it gives is {@code maxHealth * shieldShare}, to at most {@code shieldCap} times the unit's own max health. */
	public float shieldRange = 1.25f, shieldShare = 0.5f, shieldCap = 10f;

	public Color color = Color.valueOf("8a6bff"), glow = Color.valueOf("c9f4ff");
	public int orbs = 6;

	/** The bullets this unit's field has caught, and the radius each is holding. */
	protected Seq<Bullet> caught = new Seq<>();
	protected ObjectFloatMap<Bullet> hold = new ObjectFloatMap<>();
	protected float timer;

	private static boolean hooked;
	private static final Vec2 v = new Vec2();

	@Override
	public Ability copy(){
		GravityFieldAbility copy = (GravityFieldAbility)super.copy();
		//the lists belong to a unit
		copy.caught = new Seq<>();
		copy.hold = new ObjectFloatMap<>();
		copy.timer = 0f;
		return copy;
	}

	/** Listens for deaths. Called once when the content loads. */
	public static void hook(){
		if(hooked) return;
		hooked = true;
		Events.on(EventType.UnitDestroyEvent.class, e -> {
			if(Vars.net.client() || e.unit == null) return;
			victim(e.unit.team, e.unit.x, e.unit.y, e.unit.maxHealth);
		});
		Events.on(EventType.BlockDestroyEvent.class, e -> {
			if(Vars.net.client() || e.tile == null) return;
			Building build = e.tile.build;
			Team team = build != null ? build.team : e.tile.team();
			float health = build != null ? build.maxHealth : e.tile.block().health;
			victim(team, build != null ? build.x : e.tile.worldx(), build != null ? build.y : e.tile.worldy(), health);
		});
	}

	/** Something of the team died at the point: every Gravity Base nearby that is against it gains the shield. */
	public static void victim(Team team, float x, float y, float health){
		if(team == null || team == Team.derelict || health <= 0f) return;
		for(int i = 0; i < Groups.unit.size(); i++){
			Unit u = Groups.unit.index(i);
			if(u.team == team || u.dead || !u.isAdded()) continue;
			GravityFieldAbility g = of(u);
			if(g != null && u.within(x, y, g.radius * g.shieldRange)){
				u.shield = Math.min(u.shield + health * g.shieldShare, u.maxHealth * g.shieldCap);
			}
		}
	}

	public static GravityFieldAbility of(Unit unit){
		for(Ability a : unit.abilities){
			if(a instanceof GravityFieldAbility) return (GravityFieldAbility)a;
		}
		return null;
	}

	/** Instant and beam bullets, which nothing here can touch. */
	protected boolean instant(Bullet b){
		BulletType t = b.type;
		return t instanceof ContinuousBulletType || t instanceof LaserBulletType || t instanceof LightningBulletType || t instanceof PointBulletType
			|| t instanceof PointLaserBulletType || t instanceof RailBulletType || t instanceof SapBulletType || t instanceof ShrapnelBulletType
			|| b.vel.len() <= 0.01f || b.lifetime <= 1f;
	}

	/** Whether a bullet can be caught: not an instant one, and not a very fast one. */
	public boolean catchable(Bullet b){
		return !instant(b) && b.vel.len() <= maxCatchSpeed && b.type.speed <= maxCatchSpeed;
	}

	/** Whether a bullet is only bent: a fast one that is not instant. */
	public boolean bendable(Bullet b){
		return !instant(b) && !catchable(b);
	}

	@Override
	public void update(Unit unit){
		if(unit.dead) return;
		catchBullets(unit);
		steerBullets(unit);
		if(Vars.net.client()) return;
		spinUnits(unit);
		timer += Time.delta;
		if(timer >= crushInterval){
			float dt = timer;
			timer = 0f;
			crush(unit, dt);
		}
	}

	protected void catchBullets(Unit unit){
		int[] left = {catchPerTick};
		Groups.bullet.intersect(unit.x - radius, unit.y - radius, radius * 2f, radius * 2f, b -> {
			if(b.team == unit.team || !b.within(unit, radius)) return;
			if(bendable(b)){
				//Too fast to be caught: it keeps its team and speed, and its path is bent round the unit.
				float desired = Angles.angle(unit.x, unit.y, b.x, b.y) + 90f + (b.within(unit, innerRadius) ? 0f : bendInward);
				b.vel.setAngle(Angles.moveToward(b.vel.angle(), desired, bendRate * Time.delta));
				b.rotation(b.vel.angle());
				return;
			}
			if(left[0] <= 0 || !catchable(b)) return;
			left[0]--;
			b.team = unit.team;
			b.owner = unit;
			if(b.collided != null) b.collided.clear();
			//It gets at least this long to circle.
			if(b.lifetime < minBulletLife) b.lifetime = minBulletLife;
			float min = b.lifetime - minBulletLife;
			if(b.time > min) b.time = min;
			hold.put(b, Mathf.clamp(b.dst(unit), innerRadius, radius * 0.85f));
			caught.add(b);
			Fx.hitLaserBlast.at(b.x, b.y, b.rotation(), color);
		});
	}

	/** Keeps the caught bullets going round: velocity along the tangent, nudged to hold a radius that shrinks to the inner radius. */
	protected void steerBullets(Unit unit){
		for(int i = caught.size - 1; i >= 0; i--){
			Bullet b = caught.get(i);
			if(!b.isAdded() || b.team != unit.team){
				caught.remove(i);
				hold.remove(b, 0f);
				continue;
			}
			float dist = Math.max(b.dst(unit), 1f), want = hold.get(b, dist);
			//The radius it holds shrinks steadily, down to the inner radius: it spirals in and then circles there.
			if(want > innerRadius){
				want = Math.max(innerRadius, want - bulletFall * Time.delta);
				hold.put(b, want);
			}
			float angle = Angles.angle(unit.x, unit.y, b.x, b.y);
			float desired = angle + 90f + Mathf.clamp((dist - want) * 0.25f, -40f, 40f);
			float speed = Math.max(b.vel.len(), minOrbitSpeed);
			b.vel.setLength(speed).setAngle(Angles.moveToward(b.vel.angle(), desired, 14f * Time.delta));
			b.rotation(b.vel.angle());
		}
	}

	protected void spinUnits(Unit unit){
		Units.nearbyEnemies(unit.team, unit.x - radius, unit.y - radius, radius * 2f, radius * 2f, e -> {
			if(e.dead || !e.isAdded() || !e.within(unit, radius)) return;
			float scale = e.type.knockbackMultiplier;
			if(scale <= 0f) return;
			float dx = e.x - unit.x, dy = e.y - unit.y;
			v.set(dx, dy).rotate(spin * scale * Time.delta).sub(dx, dy);
			//and falls toward the unit, but not past the inner radius.
			float dist = Mathf.dst(dx, dy), stop = innerRadius + e.hitSize / 2f;
			if(dist > stop){
				float step = Math.min(fall * scale * Time.delta, dist - stop);
				v.add(-dx / dist * step, -dy / dist * step);
			}
			e.move(v.x, v.y);
		});
	}

	protected void crush(Unit unit, float dt){
		float per = dt / 60f;
		Units.nearbyEnemies(unit.team, unit.x - radius, unit.y - radius, radius * 2f, radius * 2f, e -> {
			if(e.dead || !e.isAdded()) return;
			float d = e.dst(unit) - e.hitSize / 2f;
			if(d > radius) return;
			e.damagePierce(crushDamage * per * falloff(d));
			if(Mathf.chance(0.15)) Fx.hitBeam.at(e.x, e.y, 0f, color);
		});
		Units.nearbyBuildings(unit.x, unit.y, radius, build -> {
			if(build.team == unit.team || build.team == Team.derelict || !build.isValid() || !build.block.targetable) return;
			float d = build.dst(unit) - build.hitSize() / 2f;
			if(d > radius) return;
			build.damage(unit.team, crushDamage * per * falloff(d) * crushBuildings);
			if(Mathf.chance(0.1)) Fx.hitBeam.at(build.x, build.y, 0f, color);
		});
	}

	/** 1 at the edge, {@link #crushCentre} at the centre. */
	public float falloff(float distance){
		return 1f + (crushCentre - 1f) * (1f - Mathf.clamp(distance / radius));
	}

	//---------------------------------------------------------------- looks

	@Override
	public void draw(Unit unit){
		float fill = Mathf.clamp(unit.shield / Math.max(1f, unit.maxHealth * shieldCap));

		//The edge of the field.
		Draw.z(Layer.shields - 2f);
		Draw.color(color, 0.05f);
		Fill.circle(unit.x, unit.y, radius);
		Draw.color(color, 0.35f);
		Lines.stroke(1.2f);
		Lines.dashCircle(unit.x, unit.y, radius);

		//The orbs: a trail behind each, brighter the more shield the unit holds.
		Draw.z(Layer.legUnit + 1f);
		float r = unit.hitSize * 0.78f, base = Time.time * 1.3f;
		for(int i = 0; i < orbs; i++){
			float ang = base + i * 360f / orbs;
			float ox = unit.x + Angles.trnsx(ang, r), oy = unit.y + Angles.trnsy(ang, r);
			Draw.color(color, 0.4f);
			Lines.stroke(2.2f);
			Lines.arc(unit.x, unit.y, r, 0.1f, ang - 36f);
			Draw.color(color, 0.55f);
			Fill.circle(ox, oy, 7.5f + 2f * fill);
			Draw.color(glow, 0.7f + 0.3f * fill);
			Fill.circle(ox, oy, 4.2f + 1.5f * fill);
			Draw.color(Color.white);
			Fill.circle(ox, oy, 1.8f);
		}
		Draw.reset();
	}

	@Override
	public String localized(){
		return Core.bundle.format("ability.nh-gravity-field", radius / Vars.tilesize, (int)shieldCap);
	}
}
