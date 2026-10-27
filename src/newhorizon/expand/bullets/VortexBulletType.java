package newhorizon.expand.bullets;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Bullet;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;

/**
 * A whirlpool. It stays where it was made, pulls every enemy naval unit inside its radius towards the middle and hurts
 * them while they are held there. Flying units are not touched.
 */
public class VortexBulletType extends BulletType{
	public float radius = 36f;
	/** Pull in world units per tick squared at the edge; it is stronger near the middle. */
	public float pull = 0.22f;
	/** Damage per second to a unit inside. */
	public float damagePerSecond = 40f;
	public Color color = Color.valueOf("7fd0ff");
	
	public VortexBulletType(float radius, float pull, float damagePerSecond, float lifetime){
		super(0f, 0f);
		this.radius = radius;
		this.pull = pull;
		this.damagePerSecond = damagePerSecond;
		this.lifetime = lifetime;
		
		collides = false;
		collidesAir = false;
		collidesGround = false;
		collidesTiles = false;
		hittable = false;
		absorbable = false;
		keepVelocity = false;
		hitEffect = despawnEffect = shootEffect = smokeEffect = mindustry.content.Fx.none;
		drawSize = radius * 2.5f;
	}
	
	@Override
	public void update(Bullet b){
		float fade = Mathf.curve(b.fin(), 0f, 0.1f) * Mathf.curve(b.fout(), 0f, 0.15f);
		boolean damageTick = b.timer(1, 6f);
		
		Units.nearbyEnemies(b.team, b.x - radius, b.y - radius, radius * 2f, radius * 2f, unit -> {
			if(!hits(unit) || !unit.within(b, radius))return;
			
			float close = 1f - unit.dst(b) / radius;
			float strength = pull * (0.35f + 0.65f * close) * fade;
			unit.vel.add((b.x - unit.x) / Math.max(1f, unit.dst(b)) * strength * Time.delta, (b.y - unit.y) / Math.max(1f, unit.dst(b)) * strength * Time.delta);
			
			if(damageTick && !Vars.net.client()){
				unit.damage(damagePerSecond * 6f / 60f * fade);
			}
		});
	}
	
	/** Only naval units are caught. */
	public static boolean hits(Unit unit){
		return unit.type.naval && !unit.isFlying() && !unit.dead;
	}
	
	@Override
	public void draw(Bullet b){
		float fade = Mathf.curve(b.fin(), 0f, 0.1f) * Mathf.curve(b.fout(), 0f, 0.15f);
		
		Draw.z(Layer.bullet - 3f);
		Draw.color(color, 0.12f * fade);
		Fill.circle(b.x, b.y, radius * fade);
		
		for(int i = 0; i < 4; i++){
			float r = radius * (0.25f + 0.25f * i) * fade;
			Draw.color(color, (0.35f + 0.12f * i) * fade);
			Lines.stroke(1.4f + (3 - i) * 0.4f);
			float spin = Time.time * (3.5f - i * 0.7f) * (i % 2 == 0 ? 1f : -1f);
			Lines.arc(b.x, b.y, r, 0.65f, spin);
			Lines.arc(b.x, b.y, r, 0.3f, spin + 190f);
		}
		
		Draw.reset();
	}
}
