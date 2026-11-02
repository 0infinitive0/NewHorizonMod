package newhorizon.expand.units.crab;

import arc.graphics.Color;
import mindustry.Vars;
import mindustry.entities.Damage;
import mindustry.entities.Effect;
import mindustry.entities.abilities.Ability;
import mindustry.game.Team;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import newhorizon.content.nuke.NukeBullets;
import newhorizon.content.nuke.NukeFx;

/**
 * Goes nuclear when it dies: a flash, a blast of {@link #damage} within {@link #radius} that hurts everything of every team (the unit's
 * own included), fire over the ground and a radiation fallout zone that lasts for a while. The damage and fire are done on the server.
 */
public class NuclearDeathAbility extends Ability{
	public float radius = 600f, damage = 9000f;
	public int fire = 90;
	public Color color = Color.valueOf("ff8c3a");

	@Override
	public void death(Unit unit){
		float x = unit.x, y = unit.y;
		NukeFx.blast.at(x, y, radius, color);
		NukeFx.aftermath.at(x, y, radius, color);
		Effect.shake(40f, 220f, x, y);
		Sounds.explosion.at(x, y, 1f, 0.5f);
		if(Vars.net.client()) return;
		Damage.damage(Team.derelict, x, y, radius, damage, true, true, true);
		Damage.createIncend(x, y, radius * 0.8f, fire);
		if(NukeBullets.radiationZone != null) NukeBullets.radiationZone.create(null, Team.derelict, x, y, 0f);
	}

	@Override
	public String localized(){
		return arc.Core.bundle.get("ability.nh-nuclear-death");
	}
}
