package newhorizon.expand.units.nuke;

import arc.graphics.Color;
import arc.math.Mathf;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Damage;
import mindustry.entities.Effect;
import mindustry.game.Team;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import newhorizon.content.nuke.NukeFx;

/** What a nuke does when it dies. */
public class NukeBlast{
	/**
	 * Blows the nuke up where it is. The visuals run everywhere, the damage only on the server. The damage is dealt by no team, so it
	 * hits everything in reach: enemies, allies and structures of every side, other nukes included.
	 */
	public static void explode(Unit unit, Warhead warhead, boolean active){
		float x = unit.x, y = unit.y;
		float radius = active ? warhead.radius : Warhead.inactiveRadius;
		float damage = active ? warhead.damage : Warhead.inactiveDamage;
		Color color = active ? warhead.color : Color.valueOf("ffb347");

		NukeFx.blast.at(x, y, radius, color);
		Effect.shake(active ? 14f : 5f, active ? 90f : 30f, x, y);
		Sounds.explosion.at(x, y, active ? 0.7f : 1.1f, active ? 2.6f : 1f);
		if(active) Fx.dynamicExplosion.at(x, y, radius / 24f);

		if(Vars.net.client()) return;

		//An active blast ignores walls, an inactive one is stopped by them like any explosion.
		Damage.damage(Team.derelict, x, y, radius, damage, active, true, true);
		if(active) warhead.special(x, y);
	}
}
