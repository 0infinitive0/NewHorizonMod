package newhorizon.expand.units.serpent;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.ai.UnitStance;
import mindustry.ai.types.CommandAI;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.ui.Bar;

/**
 * On the head: the state of the Spread damage toggle. While it is on, {@link SerpentSpreadSystem} shares every hit on a body section out
 * among all the sections. It lasts {@link #duration} ticks and then, or when the button is switched off, has to cool down for
 * {@link #cooldownTime} ticks before it can be used again. The ticks left are kept in {@code data}, so they are saved with the unit.
 * Only a player team's head, which has the button, can use it: enemy heads never do.
 */
public class SerpentSpreadAbility extends Ability{
	public static final float duration = 60f * 60f * 5f, cooldownTime = 60f * 30f;
	public final UnitStance stance;
	public float cooldown;
	public Color color = Color.valueOf("c885ff");

	public SerpentSpreadAbility(UnitStance stance){
		this.stance = stance;
	}

	/** Whether this head is spreading now. */
	public static boolean spreading(Unit head){
		for(Ability ability : head.abilities) if(ability instanceof SerpentSpreadAbility) return ((SerpentSpreadAbility)ability).data > 0f;
		return false;
	}

	public boolean switchedOn(Unit unit){
		return unit.controller() instanceof CommandAI && ((CommandAI)unit.controller()).hasStance(stance);
	}

	private void switchOff(Unit unit){
		if(unit.controller() instanceof CommandAI) ((CommandAI)unit.controller()).setStance(stance, false);
	}

	@Override
	public void update(Unit unit){
		if(Vars.net.client()) return;
		boolean want = !unit.dead && switchedOn(unit);

		if(data > 0f){
			data -= Time.delta;
			//It ends when its time is up, when it is switched off, or when there is nothing left to share the damage between.
			if(data <= 0f || !want || SerpentChain.sections(unit.team) == 0){
				data = 0f;
				cooldown = cooldownTime;
				switchOff(unit);
			}
		}else{
			cooldown = Math.max(0f, cooldown - Time.delta);
			if(want){
				//On during the cooldown, or with no body, does nothing: the button goes back off.
				if(cooldown <= 0f && SerpentChain.sections(unit.team) > 0) data = duration;
				else switchOff(unit);
			}
		}
	}

	@Override
	public void draw(Unit unit){
		if(data <= 0f || SerpentChain.head(unit.team) != unit) return;
		Seq<Unit> sections = SerpentChain.sectionList(unit.team);
		float z = Draw.z();
		Draw.z(Layer.effect);
		float pulse = 0.55f + 0.3f * Mathf.absin(Time.time, 12f, 1f);
		Draw.color(color, pulse);
		Lines.stroke(2f);
		Unit front = unit;
		for(Unit section : sections){
			Lines.line(front.x, front.y, section.x, section.y);
			Lines.circle(section.x, section.y, section.hitSize * 0.62f);
			front = section;
		}
		Draw.reset();
		Draw.z(z);
	}

	@Override
	public void displayBars(Unit unit, Table bars){
		bars.add(new Bar(
			() -> data > 0f ? Core.bundle.format("nh.serpent.spread.on", (int)(data / 60f)) : cooldown > 0f ? Core.bundle.format("nh.serpent.spread.cool", (int)(cooldown / 60f)) : Core.bundle.get("nh.serpent.spread.ready"),
			() -> color,
			() -> data > 0f ? data / duration : 1f - cooldown / cooldownTime
		)).row();
	}

	@Override
	public String localized(){
		return Core.bundle.get("ability.nh-serpent-spread");
	}
}
