package newhorizon.expand.block.turrets;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.gen.Posc;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.type.StatusEffect;
import mindustry.ui.Bar;
import mindustry.world.blocks.defense.turrets.PowerTurret;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;
import mindustry.world.meta.StatUnit;

import static mindustry.Vars.indexer;
import static mindustry.Vars.tilesize;

/**
 * A turret that holds enemies still: it never turns and has no bullet; every {@link #zapInterval} ticks it fires an <b>instant arc</b> at the nearest
 * enemy <b>unit</b> in range (a block only when no unit is), and the arc jumps on to up to {@link #chainTargets} - 1 more targets, each within
 * {@link #chainRange} of the one before (units first again, then blocks). Every target is hurt ({@link #dps} a second to the first, less to the others, armour
 * ignored for units) and every unit hit is disarmed and held still (speed 0) for {@link #statusTime} ticks.
 *
 * <p>The coil <b>charges while it has nothing to shoot at</b> ({@link #chargeTime} ticks, empty to full) and spends the charge while it zaps
 * ({@link #drainTime} ticks, full to empty): a full charge adds {@link #maxRangeBonus} to its range and {@link #maxDamageBonus} to its damage.
 * It uses its (large) power while it zaps or charges.
 */
public class TeslaCoil extends PowerTurret{
	public static final Stat chainStat = new Stat("nh-tesla-chain", StatCat.function), chargeStat = new Stat("nh-tesla-charge", StatCat.function);

	/** Damage a second to the first target at no charge, what each next link gets (a fraction of the one before), ticks between arcs. */
	public float dps = 300f, falloff = 0.7f, zapInterval = 5f;
	/** The targets of one chain, and how far from one to the next. */
	public int chainTargets = 3;
	public float chainRange = 96f;
	/** Ticks a status lasts after an arc (every arc renews it). */
	public float statusTime = 12f;
	/** Ticks to charge empty to full while idle, and to drain full to empty while zapping. */
	public float chargeTime = 60f * 120f, drainTime = 60f * 60f;
	/** What a full charge adds to the range and to the damage (a fraction). */
	public float maxRangeBonus = 0.6f, maxDamageBonus = 1f;
	public StatusEffect lockStatus;
	public Color color = Color.valueOf("5fe8ff");

	public TeslaCoil(String name){
		super(name);
		//it never turns: it can zap whatever is in range at once
		shootCone = 360f;
		rotateSpeed = 0f;
		targetAir = targetGround = true;
		hasPower = true;
	}

	@Override
	public void setStats(){
		super.setStats();
		stats.remove(Stat.ammo);
		stats.remove(Stat.reload);
		stats.remove(Stat.inaccuracy);
		stats.add(Stat.damage, dps, StatUnit.perSecond);
		stats.add(chainStat, Core.bundle.format("stat.nh-tesla-chain.value", chainTargets, (int)(falloff * 100f), (int)(chainRange / tilesize)));
		stats.add(chargeStat, Core.bundle.format("stat.nh-tesla-charge.value", (int)(chargeTime / 60f), (int)(drainTime / 60f), (int)(maxRangeBonus * 100f), (int)(maxDamageBonus * 100f)));
	}

	@Override
	public void setBars(){
		super.setBars();
		addBar("nh-charge", (TeslaCoilBuild b) -> new Bar(() -> Core.bundle.format("bar.nh-charge", (int)(b.charge * 100f)), () -> color, () -> b.charge));
	}

	public class TeslaCoilBuild extends PowerTurretBuild{
		/** 0 to 1. */
		public float charge;
		protected float zapTimer;
		protected boolean zapping;
		/** The targets of the last arc (for the tests and the glow). */
		public final Seq<Posc> links = new Seq<>();

		@Override
		public float range(){
			return range * (1f + maxRangeBonus * charge);
		}

		@Override
		public boolean shouldTurn(){
			return false;
		}

		@Override
		public boolean shouldConsume(){
			return target != null || charge < 0.999f;
		}

		/** The nearest enemy unit in range, else the nearest enemy block. */
		@Override
		protected void findTarget(){
			float r = range();
			Unit u = Units.closestEnemy(team, x, y, r, e -> !e.dead() && e.checkTarget(targetAir, targetGround));
			if(u != null){
				target = u;
				return;
			}
			target = targetGround ? indexer.findEnemyTile(team, x, y, r, b -> true) : null;
		}

		@Override
		public void updateTile(){
			zapping = false;
			super.updateTile();
			if(zapping){
				charge = Math.max(0f, charge - edelta() / drainTime);
			}else if(target == null){
				charge = Math.min(1f, charge + edelta() / chargeTime);
			}
		}

		/** Instead of firing a bullet: an arc at the target every {@link #zapInterval} ticks. */
		@Override
		protected void updateShooting(){
			zapping = true;
			zapTimer += edelta();
			if(zapTimer < zapInterval) return;
			zapTimer %= zapInterval;
			zap();
		}

		protected void zap(){
			if(target == null) return;
			links.clear();
			links.add(target);
			while(links.size < chainTargets){
				Posc next = nextLink(links.peek());
				if(next == null) break;
				links.add(next);
			}

			float perZap = dps * zapInterval / 60f * (1f + maxDamageBonus * charge);
			float fx = x, fy = y;
			for(int i = 0; i < links.size; i++){
				Posc t = links.get(i);
				float dmg = perZap * Mathf.pow(falloff, i);
				if(t instanceof Unit u){
					u.damagePierce(dmg);
					u.apply(StatusEffects.disarmed, statusTime);
					if(lockStatus != null) u.apply(lockStatus, statusTime);
				}else if(t instanceof Building b){
					b.damage(team, dmg);
				}
				Fx.chainLightning.at(fx, fy, 0f, color, t);
				Fx.hitLancer.at(t.getX(), t.getY(), 0f, color);
				fx = t.getX();
				fy = t.getY();
			}
			if(!Vars.headless && Mathf.chance(0.35)) Sounds.acceleratorLightning1.at(x, y, 1f, 0.5f);
		}

		/** The next link of a chain: the nearest enemy unit within reach of {@code last} that is not in it yet, else the nearest enemy block. */
		protected Posc nextLink(Posc last){
			Unit u = Units.closestEnemy(team, last.getX(), last.getY(), chainRange, e -> !e.dead() && e.checkTarget(targetAir, targetGround) && !links.contains(e));
			if(u != null) return u;
			return targetGround ? indexer.findEnemyTile(team, last.getX(), last.getY(), chainRange, b -> !links.contains(b)) : null;
		}

		@Override
		public void draw(){
			super.draw();
			if(charge <= 0.01f) return;
			Draw.z(Layer.effect);
			Draw.color(color, 0.2f + 0.6f * charge);
			Lines.stroke(1.5f + 2f * charge);
			Lines.circle(x, y, size * tilesize * 0.3f + Mathf.absin(6f, 1.5f * charge));
			if(charge > 0.2f && Mathf.chance(0.08 * charge)){
				Fx.hitLancer.at(x + Mathf.range(size * tilesize * 0.3f), y + Mathf.range(size * tilesize * 0.3f), 0f, color);
			}
			Draw.reset();
		}

		@Override
		public byte version(){
			return (byte)(super.version() + 1);
		}

		@Override
		public void write(Writes write){
			super.write(write);
			write.f(charge);
		}

		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			if(revision > 1) charge = read.f();
		}
	}
}
