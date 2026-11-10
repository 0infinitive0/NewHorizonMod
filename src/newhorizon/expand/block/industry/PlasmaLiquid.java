package newhorizon.expand.block.industry;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Rect;
import arc.util.Time;
import mindustry.entities.Fires;
import mindustry.entities.Units;
import mindustry.gen.Puddle;
import mindustry.type.Liquid;

/**
 * Plasma: so hot that even a small puddle of it is dangerous. The base game only lets a hot puddle light fires and affect units when it is
 * large (35 units or more) and only on a building; a plasma puddle of any size sets fires on its tile and melts the ground units that stand
 * in it, and scorches the building under it.
 */
public class PlasmaLiquid extends Liquid{
	/** Ticks of the status it gives, the damage a tick to the building under it, and how near a unit has to be (px). */
	public float statusTicks = 120f, buildingBurn = 0.4f, reach = 12f;

	private static final Rect rect = new Rect();

	public PlasmaLiquid(String name, Color color){
		super(name, color);
	}

	@Override
	public void update(Puddle puddle){
		if(puddle == null || puddle.tile == null) return;
		//Fires, at random.
		if(Mathf.chance(0.08 * Time.delta)) Fires.create(puddle.tile);
		//The building under it.
		if(puddle.tile.build != null) puddle.tile.build.damage(buildingBurn * Time.delta);
		//Ground units in it.
		if(effect != null){
			float r = reach;
			rect.setSize(r * 2f).setCenter(puddle.x, puddle.y);
			Units.nearby(rect, u -> {
				if(u.isGrounded() && !u.type.hovering && u.within(puddle.x, puddle.y, r + u.hitSize / 2f)) u.apply(effect, statusTicks);
			});
		}
	}
}
