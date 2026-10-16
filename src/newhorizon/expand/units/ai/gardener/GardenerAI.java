package newhorizon.expand.units.ai.gardener;

import arc.math.geom.Vec2;
import arc.struct.Queue;
import arc.util.Time;
import mindustry.ai.types.BuilderAI;
import mindustry.entities.Units;
import mindustry.game.Teams;
import mindustry.gen.Building;
import mindustry.gen.Unit;

/**
 * A builder that gets smarter with {@link GardenerMind}'s experience. Each stage fixes a flaw of the one before:
 * <ol start="0">
 *     <li><b>Seedling</b>: exactly a vanilla {@link BuilderAI}. It already watches and remembers, but acts on none of it.</li>
 *     <li><b>Sprout</b> ({@link #AVOID}): flees enemies and areas that have proven dangerous.</li>
 *     <li><b>Sapling</b> ({@link #PRIORITIZE}): rebuilds the most worthwhile things first instead of in destruction order, using what
 *     it learned about which blocks survive and where fights happen.</li>
 *     <li><b>Elder</b> ({@link #ANTICIPATE}): leaves earlier, parks in a calm spot when idle instead of loitering at the front,
 *     and its {@link GardenerAbility} mends nearby blocks.</li>
 * </ol>
 */
public class GardenerAI extends BuilderAI{
	public static final int AVOID = 1, PRIORITIZE = 2, ANTICIPATE = 4;

	/** Behaviours unlocked at a stage. */
	public static int skills(int stage){
		if(stage >= 3) return AVOID | PRIORITIZE | ANTICIPATE;
		if(stage >= 2) return AVOID | PRIORITIZE;
		if(stage >= 1) return AVOID;
		return 0;
	}

	protected float lastHealth = -1f, calmSeconds, aliveTicks, observeTicks, replanTicks, parkTicks;
	protected boolean retreating;
	protected final Vec2 refuge = new Vec2();

	@Override
	public void updateMovement(){
		GardenerMind mind = GardenerMind.instance;
		int stage = mind.stage();
		int skills = skills(stage);

		observe(mind);

		if((skills & AVOID) != 0 && avoid(mind, stage)) return;
		if((skills & PRIORITIZE) != 0) prioritize(mind);

		super.updateMovement();

		if((skills & ANTICIPATE) != 0) park(mind);
	}

	public boolean isRetreating(){
		return retreating;
	}

	/** Learning. Runs at every stage, including Seedling. */
	protected void observe(GardenerMind mind){
		float dt = Time.delta;

		if(lastHealth < 0f) lastHealth = unit.health;
		if(unit.health < lastHealth){
			mind.reinforce(unit.team, unit.x, unit.y, (lastHealth - unit.health) / unit.maxHealth * 30f);
			calmSeconds = 0f;
		}
		lastHealth = unit.health;

		calmSeconds += dt / 60f;
		if(calmSeconds >= 300f){
			calmSeconds = 0f;
			mind.calmPeriod();
		}

		aliveTicks += dt;
		if(aliveTicks >= 600f){
			aliveTicks = 0f;
			mind.addXp(0.5f);
		}

		observeTicks += dt;
		if(observeTicks >= 60f){
			observeTicks = 0f;
			int[] seen = {0};
			Units.nearbyEnemies(unit.team, unit.x - 280f, unit.y - 280f, 560f, 560f, enemy -> {
				if(seen[0]++ < 16 && enemy.hasWeapons()) mind.reinforce(unit.team, enemy.x, enemy.y, 0.3f);
			});
		}
	}

	/** Returns true while the Gardener is running away, in which case nothing else should happen this frame. */
	protected boolean avoid(GardenerMind mind, int stage){
		float threshold = mind.fleeThreshold(stage);
		float here = mind.danger(unit.team, unit.x, unit.y);
		Unit threat = Units.closestEnemy(unit.team, unit.x, unit.y, mind.senseRange(stage), Unit::hasWeapons);
		boolean unsafe = here >= threshold || threat != null;

		if(retreating){
			if(!unsafe && here < threshold * 0.5f) retreating = false;
		}else if(unsafe){
			retreating = true;
			refuge.setZero();
		}
		if(!retreating) return false;

		unit.clearBuilding();
		replanTicks += Time.delta;
		if(refuge.isZero() || replanTicks >= 20f){
			replanTicks = 0f;
			mind.safestNear(unit.team, unit.x, unit.y, 7, threat, refuge);
		}
		moveTo(refuge, 8f);
		return true;
	}

	/** Brings the most worthwhile rebuild to the head of the team's queue, where {@link BuilderAI} picks its next job. */
	protected void prioritize(GardenerMind mind){
		if(!timer.get(timerTarget4, 30f)) return;

		Queue<Teams.BlockPlan> plans = unit.team.data().plans;
		if(plans.size < 2) return;

		int bestIndex = 0;
		float best = -Float.MAX_VALUE;
		int n = Math.min(plans.size, 64);
		for(int i = 0; i < n; i++){
			float score = mind.planScore(unit.team, plans.get(i), unit.x, unit.y);
			if(score > best){
				best = score;
				bestIndex = i;
			}
		}
		//Rotating keeps the relative order of everything else.
		for(int i = 0; i < bestIndex; i++) plans.addLast(plans.removeFirst());
	}

	/** Elders do not loiter at the front line when there is nothing to do. */
	protected void park(GardenerMind mind){
		if(!unit.plans.isEmpty() || following != null || assistFollowing != null || !unit.team.data().plans.isEmpty()) return;

		Building core = unit.closestCore();
		if(core == null) return;

		parkTicks += Time.delta;
		if(refuge.isZero() || parkTicks >= 60f){
			parkTicks = 0f;
			mind.safestNear(unit.team, core.x, core.y, 5, null, refuge);
		}
		moveTo(refuge, 16f);
	}
}
