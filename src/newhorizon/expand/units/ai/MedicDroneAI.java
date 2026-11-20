package newhorizon.expand.units.ai;

import arc.math.Angles;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;

/**
 * A drone that mends instead of attacking: its repair beams heal damaged allied units and buildings by themselves, so this only takes it where it is
 * needed: to the most hurt ally (a unit or a building) within {@link #reach} of the mothership, where it hovers next to it. With nobody hurt it docks.
 * It does not matter whether the mothership has a target.
 */
public class MedicDroneAI extends DroneAI{
	/** How far from the mothership it looks for the hurt, and how close to the patient it hovers. */
	public float reach = 600f, hover = 30f;

	protected Teamc patient;
	protected float searchTimer;

	@Override
	protected void engage(Teamc t){
		idle();
	}

	@Override
	protected void idle(){
		searchTimer -= Time.delta;
		if(searchTimer <= 0f || !needsHelp(patient)){
			searchTimer = 30f;
			patient = findPatient();
		}
		if(patient == null){
			dock();
			return;
		}
		float ang = Angles.angle(patient.getX(), patient.getY(), unit.x, unit.y);
		float tx = patient.getX() + Angles.trnsx(ang, hover), ty = patient.getY() + Angles.trnsy(ang, hover);
		float dist = unit.dst(tx, ty);
		if(dist < arrive){
			unit.vel.scl(0.8f);
			unit.rotation = Angles.moveToward(unit.rotation, Angles.angle(unit.x, unit.y, patient.getX(), patient.getY()), unit.type.rotateSpeed * Time.delta);
		}else{
			fly(tx, ty, Math.min(unit.speed(), (float)Math.sqrt(2f * brakeRate * 0.8f * dist) + 0.5f));
		}
	}

	protected boolean needsHelp(Teamc t){
		if(t instanceof Unit u) return u != unit && u.isValid() && u.damaged();
		if(t instanceof Building b) return b.isValid() && b.damaged();
		return false;
	}

	/** The most hurt ally near the mothership (less hurt and farther ones lose to it). Null when nobody is hurt. */
	protected Teamc findPatient(){
		if(owner == null) return null;
		Teamc[] best = {null};
		float[] bestScore = {-1f};
		Units.nearby(unit.team, owner.x - reach, owner.y - reach, reach * 2f, reach * 2f, u -> {
			if(u == unit || !u.damaged() || u.dst(owner) > reach) return;
			float score = (1f - u.healthf()) * 3f - u.dst(unit) / reach;
			if(score > bestScore[0]){ bestScore[0] = score; best[0] = u; }
		});
		Vars.indexer.eachBlock(unit.team, owner.x, owner.y, reach, Building::damaged, b -> {
			float score = (1f - b.healthf()) * 3f - b.dst(unit) / reach;
			if(score > bestScore[0]){ bestScore[0] = score; best[0] = b; }
		});
		return best[0];
	}
}
