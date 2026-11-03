package newhorizon.expand.units.modular;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;

/**
 * Cloaking: a unit that has neither fired nor been hurt for {@link #delay} ticks cannot be targeted by the enemy (the unit's type asks
 * {@link #hidden} from {@code targetable}, see {@code ModularUnitType}). Firing or taking damage breaks the cloak at once, and the delay
 * starts again. Bullets already in the air still hit it. A faint shimmer shows where it is, on every screen.
 */
public class CloakAbility extends Ability{
	public float delay = 180f;
	public Color color = Color.valueOf("8f9bb3");

	/** How long it has been quiet, up to {@link #delay}. */
	public float quiet;
	protected float lastHealth = -1f;

	/** Whether the unit is cloaked now. */
	public static boolean hidden(Unit unit){
		for(Ability a : unit.abilities){
			if(a instanceof CloakAbility) return ((CloakAbility)a).hidden();
		}
		return false;
	}

	public boolean hidden(){
		return quiet >= delay;
	}

	@Override
	public Ability copy(){
		CloakAbility copy = (CloakAbility)super.copy();
		copy.quiet = 0f;
		copy.lastHealth = -1f;
		return copy;
	}

	@Override
	public void update(Unit unit){
		float hp = unit.health + unit.shield;
		if(lastHealth < 0f) lastHealth = hp;
		boolean hurt = hp < lastHealth - 0.001f;
		lastHealth = hp;
		if(hurt || unit.isShooting()) quiet = 0f;
		else quiet = Math.min(delay, quiet + Time.delta);
	}

	@Override
	public void draw(Unit unit){
		if(!hidden()) return;
		Draw.z(Layer.flyingUnit + 0.5f);
		float a = 0.16f + 0.1f * Mathf.absin(Time.time, 20f, 1f);
		Draw.color(color, a);
		Lines.stroke(2f);
		Lines.circle(unit.x, unit.y, unit.hitSize * 0.62f);
		Lines.stroke(1f);
		Lines.circle(unit.x, unit.y, unit.hitSize * 0.46f);
		Draw.reset();
	}

	@Override
	public String localized(){
		return Core.bundle.get("ability.nh-cloak", "Cloak");
	}
}
