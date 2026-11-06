package newhorizon.expand.bullets.ricochet;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Position;
import arc.math.geom.Vec2;
import arc.struct.ObjectSet;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Damage;
import mindustry.entities.Units;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.gen.Hitboxc;
import mindustry.gen.Sounds;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.type.StatusEffect;

/**
 * A bolt that, when it hits something, turns into a string of lightning arcs: from the thing it hit the arc jumps to the closest enemy
 * (unit or building) within {@link #bounceRange}, from there to the next, and so on, {@link #maxBounces} times. Every target an arc reaches
 * takes the bolt's full damage (there is no falloff), the ammo's status effect and, for some ammo, splash damage, fire or an
 * {@link AntiHeal} lock.
 *
 * <p>Each jump prefers an enemy the chain has not hit yet; only when none is left in range does it go back to one it already hit (never to
 * the one it is standing on), so two lone targets are bounced between until the bounces run out.
 */
public class RicochetBulletType extends BasicBulletType{
	/** How many times the arc jumps on after the first hit. */
	public int maxBounces = 10;
	public float bounceRange = 90f;
	/** Ticks between one jump and the next, so the arc visibly travels. */
	public float hopDelay = 3f;
	public Color arcColor = Color.white;
	/** Applied to every target the arcs reach (the first target gets the bolt's own {@code status}). */
	public StatusEffect arcStatus;
	public float arcStatusDuration = 60f * 3f;
	/** Ticks of {@link AntiHeal} given to every target hit (the first one too); 0 for none. */
	public float antiHeal = 0f;

	public RicochetBulletType(float speed, float damage){
		super(speed, damage);
	}

	@Override
	public void hitEntity(Bullet b, Hitboxc entity, float health){
		super.hitEntity(b, entity, health);
		if(entity instanceof Unit){
			if(antiHeal > 0f) AntiHeal.apply((Unit)entity, antiHeal);
			startChain(b, (Unit)entity);
		}
	}

	@Override
	public void hitTile(Bullet b, Building build, float x, float y, float initialHealth, boolean direct){
		super.hitTile(b, build, x, y, initialHealth, direct);
		if(build != null && build.isValid()){
			if(antiHeal > 0f) AntiHeal.apply(build, antiHeal);
			startChain(b, build);
		}
	}

	/** Starts the arcs at {@code first}, one jump every {@link #hopDelay} ticks. */
	public Chain startChain(Bullet b, Teamc first){
		return startChain(b.team, b.damage, first);
	}

	public Chain startChain(Team team, float damage, Teamc first){
		Chain chain = newChain(team, damage, first);
		chain.auto = true;
		if(maxBounces > 0) Time.run(hopDelay, chain::step);
		return chain;
	}

	/** A chain that has not been scheduled: the caller calls {@link Chain#step()} itself (public so a test can drive it). */
	public Chain newChain(Team team, float damage, Teamc first){
		return new Chain(team, damage, first);
	}

	/** True for a target an arc may reach. */
	protected boolean reachable(Teamc t){
		if(t instanceof Building) return ((Building)t).isValid();
		if(t instanceof Unit) return ((Unit)t).isAdded() && !((Unit)t).dead();
		return false;
	}

	/** The next target for a chain standing at {@code (x, y)} on {@code cur}: a fresh one if there is one, else one already hit. */
	public Teamc nextTarget(Team team, float x, float y, Teamc cur, ObjectSet<Teamc> hit){
		Teamc next = Units.closestTarget(team, x, y, bounceRange,
			u -> u != cur && !hit.contains(u) && u.checkTarget(collidesAir, collidesGround) && u.targetable(team),
			t -> t != cur && !hit.contains(t) && collidesGround);
		if(next == null){
			next = Units.closestTarget(team, x, y, bounceRange,
				u -> u != cur && u.checkTarget(collidesAir, collidesGround) && u.targetable(team),
				t -> t != cur && collidesGround);
		}
		return next;
	}

	/** What an arc does to a target it reaches (damage and the rest only on the server). */
	protected void arcHit(Team team, float dmg, Teamc target){
		float tx = target.getX(), ty = target.getY();
		hitEffect.at(tx, ty, 0f, arcColor);
		if(Vars.net.client()) return;

		if(target instanceof Unit){
			Unit unit = (Unit)target;
			if(pierceArmor) unit.damagePierce(dmg); else unit.damage(dmg);
			if(arcStatus != null) unit.apply(arcStatus, arcStatusDuration);
		}else if(target instanceof Building){
			((Building)target).damage(team, dmg * buildingDamageMultiplier);
		}
		if(antiHeal > 0f && target instanceof mindustry.gen.Healthc) AntiHeal.apply((mindustry.gen.Healthc)target, antiHeal);
		if(splashDamage > 0f && splashDamageRadius > 0f){
			Damage.damage(team, tx, ty, splashDamageRadius, splashDamage, splashDamagePierce, collidesAir, collidesGround);
		}
		if(incendChance > 0f && Mathf.chance(incendChance)){
			Damage.createIncend(tx, ty, incendSpread, incendAmount);
		}
	}

	/** One running chain of arcs. */
	public class Chain{
		public final Team team;
		public final float damage;
		/** Every target reached so far, the first one included. */
		public final ObjectSet<Teamc> hit = new ObjectSet<>();
		public int left = maxBounces;
		public int hops;
		/** True when each jump schedules the next on the game's clock; a chain driven by hand by a test does not. */
		public boolean auto;
		public Teamc cur;
		/** Where the chain stands now (the last known spot if its target is gone). */
		public float x, y;

		Chain(Team team, float damage, Teamc first){
			this.team = team;
			this.damage = damage;
			this.cur = first;
			this.x = first.getX();
			this.y = first.getY();
			hit.add(first);
		}

		/** One jump; schedules the next. Public so a test can run the chain without the clock. */
		public boolean step(){
			if(left <= 0) return false;
			if(cur != null && reachable(cur)){
				x = cur.getX();
				y = cur.getY();
			}

			Teamc next = nextTarget(team, x, y, cur, hit);
			if(next == null) return false;

			left--;
			hops++;
			Position from = new Vec2(x, y);
			Fx.chainLightning.at(from.getX(), from.getY(), 0f, arcColor, next);
			Sounds.shootArc.at(next.getX(), next.getY(), 1.2f, 0.5f);
			arcHit(team, damage, next);
			hit.add(next);
			cur = next;
			x = next.getX();
			y = next.getY();

			if(auto && left > 0) Time.run(hopDelay, this::step);
			return true;
		}
	}
}
