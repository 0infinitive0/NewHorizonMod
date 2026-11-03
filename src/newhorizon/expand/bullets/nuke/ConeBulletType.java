package newhorizon.expand.bullets.nuke;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Interp;
import arc.math.Mathf;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.game.Team;
import mindustry.gen.Bullet;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.type.StatusEffect;

/**
 * A swipe or a pulse: for one tick, everything of the enemy's (units and buildings) in a wedge in front of the weapon, up to
 * {@link #radius} away and within {@link #cone} degrees of where it points, takes the bullet's damage and the status, and units are
 * pushed away by {@link BulletType#knockback} (scaled by each unit's own {@code knockbackMultiplier}, so a unit that cannot be pushed
 * is not). The wedge is drawn as a sweeping arc. The damage is done on the server only.
 *
 * <p>It does what a thin beam cannot: a sword that hits everything in its swing and a pulse that shoves a whole crowd.
 */
public class ConeBulletType extends BulletType{
	/** How far it reaches, and how wide the wedge is (the full angle, in degrees). */
	public float radius = 90f, cone = 100f;
	public Color color = Color.white;
	public StatusEffect coneStatus;
	public float coneStatusTime = 60f;

	/** The swipe, drawn from the weapon. The effect's data is {@code {radius, cone}}, its rotation the heading. */
	public static Effect swipe;

	static{
		swipe = new Effect(16f, 600f, e -> {
			if(!(e.data instanceof float[])) return;
			float[] d = (float[])e.data;
			float r = d[0], cone = d[1];
			float grow = e.fin(Interp.pow3Out);
			Draw.z(Layer.effect + 1f);
			Draw.color(e.color, 0.28f * e.fout());
			Fill.arc(e.x, e.y, r * (0.35f + 0.65f * grow), cone / 360f, e.rotation - cone / 2f);
			Draw.color(Color.white, e.color, e.fin());
			Lines.stroke(1.5f + 4f * e.fout());
			Lines.arc(e.x, e.y, r * (0.35f + 0.65f * grow), cone / 360f, e.rotation - cone / 2f);
			Lines.stroke(0.8f + 2f * e.fout());
			Lines.arc(e.x, e.y, r * 0.7f * grow, cone / 360f, e.rotation - cone / 2f);
			Draw.reset();
		});
	}

	public ConeBulletType(){
		super(0f, 0f);
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
	}

	@Override
	public float calculateRange(){
		return radius;
	}

	@Override
	public void init(Bullet b){
		super.init(b);
		swipe.at(b.x, b.y, b.rotation(), color, new float[]{radius, cone});
		if(Vars.net.client()) return;

		float rot = b.rotation();
		float reach = radius + 40f;
		Units.nearbyEnemies(b.team, b.x - reach, b.y - reach, reach * 2f, reach * 2f, unit -> {
			if(unit.dead() || !unit.isAdded() || !unit.targetable(b.team) || !unit.checkTarget(collidesAir, collidesGround)) return;
			if(!inside(b, rot, unit.x, unit.y, unit.hitSize / 2f)) return;
			unit.damage(b.damage);
			if(coneStatus != null) unit.apply(coneStatus, coneStatusTime);
			if(knockback != 0f && unit.type.knockbackMultiplier > 0f){
				unit.impulse(Tmp.v3.set(unit).sub(b.x, b.y).nor().scl(knockback * 80f * unit.type.knockbackMultiplier));
			}
		});
		if(collidesGround){
			Units.nearbyBuildings(b.x, b.y, reach, build -> {
				if(!build.isValid() || build.team == b.team || build.team == Team.derelict || !build.block.targetable) return;
				if(!inside(b, rot, build.x, build.y, build.hitSize() / 2f)) return;
				build.damage(b.team, b.damage * buildingDamageMultiplier);
			});
		}
	}

	/** Whether something of the given size at the point is in the wedge: within reach, and within the half angle of the heading. */
	protected boolean inside(Bullet b, float rot, float x, float y, float size){
		float d = Mathf.dst(x, y, b.x, b.y);
		if(d > radius + size) return false;
		if(d <= size) return true;
		//Its own size widens the wedge as seen from the weapon.
		float widen = (float)Math.toDegrees(Math.atan2(size, d));
		return Angles.angleDist(rot, Angles.angle(b.x, b.y, x, y)) <= cone / 2f + widen;
	}
}
