package newhorizon.expand.units.crab;

import arc.graphics.Color;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;

/** Every {@link #reload} ticks, heals every ally unit and structure within {@link #range} by {@link #percent} of its maximum health. */
public class AreaHealAbility extends Ability{
	public float range = 300f, reload = 60f, percent = 0.06f;
	public Color color = Pal.heal;
	private float timer;

	@Override
	public void update(Unit unit){
		if(unit.dead) return;
		timer += Time.delta;
		if(timer < reload) return;
		timer = 0f;
		Fx.healWaveDynamic.at(unit.x, unit.y, range, color);
		if(Vars.net.client()) return;
		Units.nearby(unit.team, unit.x - range, unit.y - range, range * 2f, range * 2f, u -> {
			if(u.dead() || !u.damaged() || !u.within(unit, range + u.hitSize / 2f)) return;
			u.heal(u.maxHealth * percent);
			Fx.healWaveMend.at(u.x, u.y, u.hitSize, color);
		});
		Units.nearbyBuildings(unit.x, unit.y, range, b -> {
			if(b.team != unit.team || !b.damaged() || b.dst(unit) > range) return;
			b.heal(b.maxHealth * percent);
			Fx.healBlockFull.at(b.x, b.y, b.block.size, color, b.block);
		});
	}

	@Override
	public String localized(){
		return arc.Core.bundle.get("ability.nh-area-heal");
	}
}
