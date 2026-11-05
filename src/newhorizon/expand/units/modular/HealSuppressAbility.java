package newhorizon.expand.units.modular;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.game.Team;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import newhorizon.content.nuke.NukeStatus;
import newhorizon.expand.status.NoHeal;

/**
 * Heal suppression field: every {@link #interval} ticks, enemy units within {@link #range} get the unhealable status and enemy structures
 * within it are held (see {@link NoHeal}), for a little longer than that, so nothing in the field heals, whatever heals it. A ring shows its
 * edge.
 */
public class HealSuppressAbility extends Ability{
	public float range = 160f, interval = 15f, hold = 45f;
	public Color color = Color.valueOf("ff5f7a");

	protected float timer;

	@Override
	public Ability copy(){
		HealSuppressAbility copy = (HealSuppressAbility)super.copy();
		copy.timer = 0f;
		return copy;
	}

	@Override
	public void update(Unit unit){
		if(unit.dead || Vars.net.client()) return;
		timer += Time.delta;
		if(timer < interval) return;
		timer = 0f;

		if(NukeStatus.unhealable != null){
			Units.nearbyEnemies(unit.team, unit.x - range, unit.y - range, range * 2f, range * 2f, u -> {
				if(!u.dead() && u.isAdded() && u.within(unit, range + u.hitSize / 2f)) u.apply(NukeStatus.unhealable, hold);
			});
		}
		Units.nearbyBuildings(unit.x, unit.y, range, build -> {
			if(build.team != unit.team && build.team != Team.derelict && build.isValid()) NoHeal.mark(build, hold);
		});
	}

	@Override
	public void draw(Unit unit){
		Draw.z(Layer.bullet - 1f);
		Draw.color(color, 0.12f + 0.08f * Mathf.absin(Time.time, 24f, 1f));
		Lines.stroke(1.5f);
		Lines.circle(unit.x, unit.y, range);
		Draw.color(color, 0.2f);
		Lines.stroke(1f);
		Lines.dashCircle(unit.x, unit.y, range * 0.96f);
		Draw.reset();
	}

	@Override
	public String localized(){
		return Core.bundle.get("ability.nh-heal-suppress", "Heal suppression field");
	}
}
