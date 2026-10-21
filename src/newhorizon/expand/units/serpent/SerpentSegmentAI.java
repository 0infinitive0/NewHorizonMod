package newhorizon.expand.units.serpent;

import arc.math.Angles;
import arc.math.Mathf;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.entities.units.AIController;
import mindustry.gen.Unit;

/**
 * Drives a body section: it keeps a fixed distance behind the section (or the head) in front of it, on the line towards it, and turns
 * to face it. It does not aim at anything itself: its cannons pick their own targets. With the head gone, the sections are lost.
 */
public class SerpentSegmentAI extends AIController{
	@Override
	public void updateMovement(){
		if(unit == null || unit.dead) return;
		Unit head = SerpentChain.head(unit.team);
		if(head == null){
			if(!Vars.net.client()) unit.kill();
			return;
		}
		Unit front = SerpentChain.predecessor(unit);
		if(front == null || front == unit) return;

		float spacing = SerpentChain.spacing(unit, front);
		Tmp.v1.set(unit).sub(front);
		if(Tmp.v1.isZero(0.01f)) Tmp.v1.trns(front.rotation + 180f, spacing);
		float dst = Tmp.v1.len();
		//Where it should be: the same line, at the right distance.
		Tmp.v2.set(front).add(Tmp.v1.setLength(spacing));
		Tmp.v3.set(Tmp.v2).sub(unit);
		float gap = Tmp.v3.len();
		//Faster the further behind it is, so that it can keep up with a head that is moving.
		float speed = unit.type.speed * Mathf.clamp(1f + (dst - spacing) / spacing * 2f, 0.4f, 5f);
		unit.moveAt(Tmp.v3.setLength(Math.min(gap, speed)));
		unit.lookAt(front);
	}

	/** Nothing to aim: the cannons choose their own targets. */
	@Override
	public void updateTargeting(){
	}
}
