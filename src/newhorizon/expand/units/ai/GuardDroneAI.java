package newhorizon.expand.units.ai;

import arc.math.Angles;
import mindustry.gen.Teamc;

/**
 * A drone that guards instead of attacking (the Interceptor, which shoots down bullets, and the Aegis, which carries a shield bubble). With
 * {@link #forward} 0 it stays docked at its slot beside the mothership; otherwise it takes a post that far (plus half the mothership's size) from the
 * mothership toward the mothership's target, so what it carries screens the front. With no target it docks.
 */
public class GuardDroneAI extends DroneAI{
	/** How far in front of the mothership's hull toward its target the post is (0: stay docked). */
	public float forward = 0f;

	@Override
	protected void engage(Teamc t){
		if(forward <= 0f || owner == null){
			dock();
			return;
		}
		float ang = Angles.angle(owner.x, owner.y, t.getX(), t.getY());
		float r = owner.hitSize / 2f + forward;
		station(owner.x + Angles.trnsx(ang, r), owner.y + Angles.trnsy(ang, r));
	}
}
