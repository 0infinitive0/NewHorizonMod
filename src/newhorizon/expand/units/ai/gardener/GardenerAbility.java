package newhorizon.expand.units.ai.gardener;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.Effect;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import newhorizon.content.jade.JadeColor;
import newhorizon.content.jade.JadeFx;

/** Shows how far the Gardeners have evolved as pips over the unit, and lets Elders mend nearby blocks. */
public class GardenerAbility extends Ability{
	public float healRadius = 72f;
	public float healPercent = 6f;
	public float reload = 180f;
	public Effect healEffect = JadeFx.leafBurst;

	@Override
	public void update(Unit unit){
		if(GardenerMind.instance.stage() < 3) return;

		data += Time.delta;
		if(data < reload) return;
		data = 0f;

		Vars.indexer.eachBlock(unit.team, unit.x, unit.y, healRadius, Building::damaged, b -> {
			b.heal(b.maxHealth() * healPercent / 100f);
			healEffect.at(b.x, b.y, b.block.size);
		});
	}

	@Override
	public void draw(Unit unit){
		int stage = GardenerMind.instance.stage();
		if(stage <= 0) return;

		float y = unit.y + unit.hitSize / 2f + 7f;
		float x0 = unit.x - (stage - 1) * 2.6f;
		Draw.z(Layer.flyingUnit + 1f);
		for(int i = 0; i < stage; i++){
			Draw.color(Color.black, 0.45f);
			Fill.circle(x0 + i * 5.2f, y - 0.6f, 2f);
			Draw.color(stage >= 3 ? JadeColor.pollen : JadeColor.jade);
			Fill.circle(x0 + i * 5.2f, y, 1.6f);
		}
		Draw.reset();
	}

	@Override
	public String localized(){
		return Core.bundle.get("ability.nh-gardener", "Gardener Mind");
	}
}
