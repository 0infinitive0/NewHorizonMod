package newhorizon.expand.units.ai;

import arc.math.Angles;
import arc.math.Mathf;
import mindustry.gen.Teamc;

/**
 * Flies a drone in <b>attack runs</b> at whatever its mothership is attacking: the Rammer, which rams, and the Bomber, which drops its bombs on
 * the way over. A run is: fly at the target, straight through it, keep going to {@link #runOut} px past it (so that the turn back has room),
 * turn round and go again. The rest (binding, healing, recall, wasting away) is {@link DroneAI}.
 */
public class RammerAI extends DroneAI{
	/** How far past the target a run goes before the turn. */
	public float runOut = 150f;
	public boolean running;

	@Override
	protected void engage(Teamc t){
		float dx = t.getX() - unit.x, dy = t.getY() - unit.y;
		float dist = Mathf.len(dx, dy);
		float speed = unit.vel.len();
		boolean behind = speed > 0.5f && (dx * unit.vel.x + dy * unit.vel.y) < 0f;

		//through it: keep flying straight until there is room to turn round in
		if(!running && behind && dist < runOut) running = true;
		if(running && dist >= runOut) running = false;

		if(running){
			float h = speed > 0.1f ? unit.vel.angle() : unit.rotation;
			fly(unit.x + Angles.trnsx(h, 100f), unit.y + Angles.trnsy(h, 100f), unit.speed());
		}else{
			fly(t.getX(), t.getY(), unit.speed());
		}
	}

	@Override
	protected void dock(){
		running = false;
		super.dock();
	}
}
