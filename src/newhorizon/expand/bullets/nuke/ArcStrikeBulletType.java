package newhorizon.expand.bullets.nuke;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Position;
import arc.math.geom.Vec2;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.type.StatusEffect;

/**
 * A single bolt of lightning, like the arcs of an Aegis but thick and heavy, thrown at one chosen target. It is there for one tick: when
 * the weapon fires, the one enemy (unit or building) closest to the point the weapon is aimed at, within {@link #snap} of it and within
 * reach of the weapon, takes {@link #damage} and the status, and nothing else is touched. Aimed at nothing, the bolt still strikes the
 * spot, and does no harm. The bolt is drawn everywhere; the damage is done on the server only.
 */
public class ArcStrikeBulletType extends BulletType{
	/**
	 * The health of the toughest wall of the base game (the large carbide wall, 4320) rounded up to the next thousand: what one bolt does.
	 * The tests check it against the base game's walls.
	 */
	public static final float wallDamage = 5000f;

	/** How far from the point aimed at a target may be and still be the one struck. */
	public float snap = 36f;
	public StatusEffect strikeStatus = StatusEffects.sapped;
	public float strikeStatusTime = 60f * 4f;
	public Color color = Color.valueOf("9c3fe8"), light = Color.valueOf("c885ff");

	/** The bolt, from the weapon to what it struck. The effect's data is the end of it. */
	public static Effect bolt;

	private static final Color tmp = new Color();

	static{
		bolt = new Effect(30f, 3000f, e -> {
			if(!(e.data instanceof Position)) return;
			Position to = (Position)e.data;
			Color base = e.color;
			float a = e.fout(arc.math.Interp.pow2Out);
			//Re-drawn with a new shape a few times in its short life, so that it crackles.
			long seed = e.id * 31L + (long)(e.time / 3f);

			Draw.z(Layer.effect + 2f);
			Drawf.light(e.x, e.y, 90f * a, base, 0.7f * a);
			Drawf.light(to.getX(), to.getY(), 130f * a, base, 0.8f * a);
			drawBolt(e.x, e.y, to.getX(), to.getY(), seed, 18f, base, 9f * a, 0.45f * a);
			drawBolt(e.x, e.y, to.getX(), to.getY(), seed, 18f, mix(base, Color.white, 0.35f), 5.5f * a, 0.85f * a);
			drawBolt(e.x, e.y, to.getX(), to.getY(), seed, 18f, Color.white, 2.2f * a, a);

			//The flash and the ring where it lands, and at the weapon.
			float tx = to.getX(), ty = to.getY();
			Draw.color(base, 0.5f * a);
			Fill.circle(tx, ty, 26f * e.fout());
			Draw.color(Color.white, a);
			Fill.circle(tx, ty, 11f * e.fout(arc.math.Interp.pow3Out));
			Draw.color(base, a);
			Lines.stroke(4f * e.fout());
			Lines.circle(tx, ty, 8f + 46f * e.fin(arc.math.Interp.pow3Out));
			Fill.circle(e.x, e.y, 9f * e.fout(arc.math.Interp.pow3Out));
			Draw.color(Color.white, a);
			Fill.circle(e.x, e.y, 4f * e.fout(arc.math.Interp.pow3Out));

			Draw.color(mix(base, Color.white, 0.5f), a);
			Lines.stroke(2.2f * e.fout());
			Angles.randLenVectors(e.id, 9, 10f + 56f * e.fin(arc.math.Interp.pow3Out), (x, y) ->
				Lines.lineAngle(tx + x, ty + y, Mathf.angle(x, y), 3f + 9f * e.fout()));
			Draw.reset();
		});
	}

	private static Color mix(Color a, Color b, float t){
		return tmp.set(a).lerp(b, t);
	}

	/** A jagged line from one point to another: the same shape for the same seed, jumping out sideways most in the middle. */
	public static void drawBolt(float x1, float y1, float x2, float y2, long seed, float jag, Color color, float width, float alpha){
		float len = Mathf.dst(x1, y1, x2, y2);
		if(len < 1f) return;
		int segs = Math.max(4, (int)(len / 13f));
		Rand rand = new Rand(seed);
		float angle = Angles.angle(x1, y1, x2, y2), nx = Angles.trnsx(angle + 90f, 1f), ny = Angles.trnsy(angle + 90f, 1f);
		Draw.color(color, alpha);
		Lines.stroke(width);
		float px = x1, py = y1;
		for(int i = 1; i <= segs; i++){
			float t = i / (float)segs;
			float off = i == segs ? 0f : rand.range(jag) * Mathf.sin(t * Mathf.pi);
			float cx = Mathf.lerp(x1, x2, t) + nx * off, cy = Mathf.lerp(y1, y2, t) + ny * off;
			Lines.line(px, py, cx, cy, false);
			Fill.circle(cx, cy, width / 2f);
			px = cx;
			py = cy;
		}
		Fill.circle(x1, y1, width / 2f);
	}

	public ArcStrikeBulletType(){
		super(0f, wallDamage);
		lifetime = 1f;
		collides = false;
		collidesTiles = false;
		collidesAir = true;
		collidesGround = true;
		absorbable = false;
		hittable = false;
		reflectable = false;
		keepVelocity = false;
		hitEffect = despawnEffect = Fx.none;
		smokeEffect = shootEffect = Fx.none;
		rangeOverride = 330f;
	}

	@Override
	public void init(Bullet b){
		super.init(b);
		float aimX = b.aimX, aimY = b.aimY;
		if(aimX < 0f && aimY < 0f){
			aimX = b.x + Angles.trnsx(b.rotation(), rangeOverride);
			aimY = b.y + Angles.trnsy(b.rotation(), rangeOverride);
		}

		Position target = pick(b, aimX, aimY);
		Vec2 end = new Vec2();
		if(target != null){
			end.set(target.getX(), target.getY());
		}else{
			//Nothing there: it strikes the spot, as far as the weapon reaches.
			end.set(aimX, aimY);
			if(end.dst(b) > rangeOverride) end.set(b).add(new Vec2(aimX - b.x, aimY - b.y).setLength(rangeOverride));
		}

		bolt.at(b.x, b.y, 0f, light, end);
		Effect.shake(target == null ? 2f : 7f, 14f, end.x, end.y);
		Sounds.acceleratorLightning1.at(end.x, end.y, 0.65f, 1.6f);
		if(target != null) Sounds.explosion.at(end.x, end.y, 0.7f, 1.3f);

		if(Vars.net.client() || target == null) return;
		if(target instanceof Unit){
			Unit unit = (Unit)target;
			unit.damage(damage);
			if(strikeStatus != null) unit.apply(strikeStatus, strikeStatusTime);
		}else if(target instanceof Building){
			((Building)target).damage(b.team, damage);
		}
	}

	/** The one enemy nearest the point aimed at, within {@link #snap} of it (its own size counts) and within reach of the weapon; null if none. */
	public Position pick(Bullet b, float aimX, float aimY){
		return pick(b.team, b.x, b.y, aimX, aimY);
	}

	/** The same, for a bolt that has not been made yet: from a muzzle at (fromX, fromY), for a team. */
	public Position pick(mindustry.game.Team team, float fromX, float fromY, float aimX, float aimY){
		float reach = rangeOverride;
		Object[] best = {null};
		float[] bestDst = {Float.MAX_VALUE};
		Units.nearbyEnemies(team, aimX - snap - 40f, aimY - snap - 40f, (snap + 40f) * 2f, (snap + 40f) * 2f, unit -> {
			if(unit.dead() || !unit.isAdded() || !unit.targetable(team) || !unit.checkTarget(collidesAir, collidesGround)) return;
			float d = unit.dst(aimX, aimY);
			if(d > snap + unit.hitSize / 2f || unit.dst(fromX, fromY) > reach + unit.hitSize / 2f || d >= bestDst[0]) return;
			bestDst[0] = d;
			best[0] = unit;
		});
		Units.nearbyBuildings(aimX, aimY, snap + 40f, build -> {
			if(!build.isValid() || build.team == team || build.team == mindustry.game.Team.derelict || !build.block.targetable) return;
			float d = build.dst(aimX, aimY);
			if(d > snap + build.hitSize() / 2f || build.dst(fromX, fromY) > reach + build.hitSize() / 2f || d >= bestDst[0]) return;
			bestDst[0] = d;
			best[0] = build;
		});
		return (Position)best[0];
	}
}
