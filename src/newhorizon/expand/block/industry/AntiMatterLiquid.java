package newhorizon.expand.block.industry;

import arc.graphics.Color;
import arc.math.Mathf;
import mindustry.content.Fx;
import mindustry.entities.Damage;
import mindustry.entities.Effect;
import mindustry.game.Team;
import mindustry.gen.Puddle;
import mindustry.gen.Sounds;
import mindustry.type.Liquid;

/**
 * Anti-matter. It must never touch the world: the moment a puddle of it exists (a pipe or tank that leaks it out, a spill) the puddle detonates
 * and is gone. The blast is bigger with more liquid in the puddle, and hurts everything in it, of every team, buildings and units alike. (A
 * building that holds a lot of it and is destroyed explodes in proportion to what it held, because of the liquid's explosiveness.)
 */
public class AntiMatterLiquid extends Liquid{
	/** Blast radius (px) and damage: a base, plus so much for every unit of liquid in the puddle, and the limit of the radius. */
	public float blastRadius = 40f, blastRadiusPerUnit = 2f, maxBlastRadius = 160f;
	public float blastDamage = 80f, blastDamagePerUnit = 15f;

	public AntiMatterLiquid(String name, Color color){
		super(name, color);
	}

	/** Radius of the blast of a puddle with this much liquid. */
	public float radiusFor(float amount){
		return Mathf.clamp(blastRadius + amount * blastRadiusPerUnit, blastRadius, maxBlastRadius);
	}

	/** Damage of the blast of a puddle with this much liquid. */
	public float damageFor(float amount){
		return blastDamage + amount * blastDamagePerUnit;
	}

	@Override
	public void update(Puddle puddle){
		if(puddle == null || !puddle.isAdded()) return;
		explode(puddle.x, puddle.y, puddle.amount);
		puddle.remove();
	}

	public void explode(float x, float y, float amount){
		float radius = radiusFor(amount);
		Damage.damage(Team.derelict, x, y, radius, damageFor(amount), true, true, true);
		Fx.massiveExplosion.at(x, y, radius / 8f);
		Fx.bigShockwave.at(x, y, 0f, color);
		Effect.shake(6f + radius / 20f, 20f, x, y);
		Sounds.explosion.at(x, y, 1f, 0.6f);
	}
}
