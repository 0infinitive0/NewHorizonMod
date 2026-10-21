package newhorizon.expand.units.serpent;

import arc.math.Mathf;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Damage;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import newhorizon.content.nuke.NukeFx;

import static mindustry.Vars.tilesize;

/**
 * What a Serpent's body sections have of the Omni Wall: the damage they take is kept as charge (the section is hurt as usual), and when
 * more than {@link #threshold} has built up and an enemy comes within {@link #triggerRange} it is let out as a blast around the section
 * that hurts everything of the enemy's in reach. The charge is kept in {@code data}, so it is saved with the unit.
 */
public class OmniArmorAbility extends Ability{
	public float maxCharge = 3000f, threshold = 100f, triggerRange = 4f * tilesize, blastRadius = 5f * tilesize;
	public float blastCooldown = 20f;
	public arc.graphics.Color color = arc.graphics.Color.valueOf("c885ff");

	private float last = -1f, cooldown, scan;

	public float charge(){
		return data;
	}

	@Override
	public void update(Unit unit){
		if(unit.dead) return;
		if(last < 0f) last = unit.health;
		float loss = last - unit.health;
		if(loss > 0f) data = Math.min(maxCharge, data + loss);
		last = unit.health;

		cooldown = Math.max(0f, cooldown - Time.delta);
		if(data <= threshold || cooldown > 0f || Vars.net.client()) return;
		scan += Time.delta;
		if(scan < 6f) return;
		scan = 0f;

		boolean[] near = {false};
		float r = triggerRange;
		Units.nearbyEnemies(unit.team, unit.x - r - 12f, unit.y - r - 12f, (r + 12f) * 2f, (r + 12f) * 2f, enemy -> {
			if(!enemy.dead() && enemy.within(unit, r + enemy.hitSize / 2f)) near[0] = true;
		});
		if(near[0]) release(unit);
	}

	/** Lets out what was kept, in a blast around the unit. */
	public void release(Unit unit){
		float damage = data;
		data = 0f;
		cooldown = blastCooldown;
		Damage.damage(unit.team, unit.x, unit.y, blastRadius, damage, false, true, true);
		NukeFx.shockRing.at(unit.x, unit.y, blastRadius, color);
		Effect.shake(Math.min(6f, damage / 300f), 12f, unit.x, unit.y);
		Sounds.explosion.at(unit.x, unit.y, 0.8f, 1.4f);
		Fx.hitLaserBlast.at(unit.x, unit.y, 0f, color);
	}

	@Override
	public String localized(){
		return arc.Core.bundle.get("ability.nh-omni-armor");
	}
}
