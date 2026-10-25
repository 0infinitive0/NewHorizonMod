package newhorizon.expand.units.serpent;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.Rand;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.entities.abilities.Ability;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import newhorizon.expand.bullets.nuke.ArcStrikeBulletType;

/**
 * Looks after the head's lightning arc: it draws the charge-up before the shot and the cooldown after it, and lets the body's stored
 * charge shorten the cooldown. A shot starts with a charge ({@code shoot.firstShotDelay}), then the bolt, then the weapon reloads, and
 * the time to its next shot is what the weapon's {@code reload} counts down from the start of the charge.
 *
 * <p>While the weapon is cooling, the body sections hand over what they have stored (see {@link OmniArmorAbility}): {@link #chargePerTick}
 * of it buys one tick off the cooldown, and at most {@link #maxPerTick} is taken in a tick, shared among the sections in proportion to
 * what each has. Only the server takes the charge, because only the server decides when the weapon is ready; the drawing is on every side.
 */
public class SerpentArcAbility extends Ability{
	/** How much stored charge buys one tick off the cooldown, and the most that can be taken in one tick. */
	public float chargePerTick = 10f, maxPerTick = 100f;
	public Color color = Color.valueOf("9c3fe8"), light = Color.valueOf("c885ff");

	/** What was seen this tick: how far the charge has got (0 to 1, or -1 when it is not charging), how much of the cooldown is left (1 to 0, -1 when none), and when charge last went to the cooldown. */
	public float charge = -1f, cooldown = -1f;
	public float lastTaken = -1000f;
	private final Seq<Unit> givers = new Seq<>();

	/** The head's arc weapon's mount, or null. */
	public static WeaponMount mount(Unit unit){
		for(WeaponMount m : unit.mounts){
			if(m.weapon.bullet instanceof ArcStrikeBulletType) return m;
		}
		return null;
	}

	@Override
	public void update(Unit unit){
		WeaponMount m = mount(unit);
		charge = cooldown = -1f;
		if(m == null || unit.dead || m.reload <= 0.0001f) return;

		float full = m.weapon.reload, delay = m.weapon.shoot.firstShotDelay;
		float elapsed = full - m.reload;
		if(elapsed < delay){
			charge = Mathf.clamp(elapsed / Math.max(1f, delay));
			return;
		}
		cooldown = Mathf.clamp(m.reload / Math.max(1f, full - delay));

		if(Vars.net.client()) return;
		float want = Math.min(m.reload, (maxPerTick / chargePerTick) * Time.delta);
		givers.clear();
		float total = 0f;
		for(Unit s : SerpentChain.sectionList(unit.team)){
			OmniArmorAbility oa = armor(s);
			if(oa != null && oa.charge() > 0f){
				givers.add(s);
				total += oa.charge();
			}
		}
		if(total <= 0f) return;
		float cut = Math.min(want, total / chargePerTick);
		if(cut <= 0f) return;
		float cost = cut * chargePerTick;
		for(Unit s : givers){
			OmniArmorAbility oa = armor(s);
			oa.take(cost * oa.charge() / total);
		}
		m.reload -= cut;
		lastTaken = Time.time;
		//Ready this tick: the cooldown is over.
		if(m.reload < 0f) m.reload = 0f;
	}

	private static OmniArmorAbility armor(Unit unit){
		for(Ability a : unit.abilities) if(a instanceof OmniArmorAbility) return (OmniArmorAbility)a;
		return null;
	}

	@Override
	public void draw(Unit unit){
		if(charge < 0f && cooldown < 0f) return;
		Tmp.v1.trns(unit.rotation, 8f).add(unit);
		float mx = Tmp.v1.x, my = Tmp.v1.y;
		long frame = (long)(Time.time / 3f);

		if(charge >= 0f){
			//Charging: an orb that swells and brightens, rings closing on it and little bolts jumping in from round the head.
			float c = charge, pulse = 1f + 0.12f * Mathf.absin(Time.time, 5f, 1f);
			Draw.z(Layer.effect + 1f);
			Drawf.light(mx, my, 40f + 90f * c, light, 0.7f * c);
			Draw.color(color, 0.3f + 0.3f * c);
			Fill.circle(mx, my, (3f + 15f * c) * pulse);
			Draw.color(light, 0.5f + 0.5f * c);
			Fill.circle(mx, my, (2f + 9f * c) * pulse);
			Draw.color(Color.white, c);
			Fill.circle(mx, my, (1f + 4.5f * c) * pulse);
			Lines.stroke(1.2f + 1.8f * c);
			for(int i = 0; i < 3; i++){
				float phase = (c * 3f + i / 3f) % 1f;
				Draw.color(light, c * (1f - phase));
				Lines.circle(mx, my, 6f + 44f * (1f - phase));
			}
			Rand rand = new Rand(unit.id * 17L + frame);
			int bolts = 2 + (int)(4 * c);
			for(int i = 0; i < bolts; i++){
				float angle = rand.random(360f), dst = rand.random(26f, 52f) * (0.6f + 0.4f * c);
				ArcStrikeBulletType.drawBolt(mx + Angles.trnsx(angle, dst), my + Angles.trnsy(angle, dst), mx, my, rand.nextLong(), 6f, light, 1.6f, 0.4f + 0.5f * c);
			}
		}

		if(cooldown >= 0f){
			float f = cooldown;
			Draw.z(Layer.effect + 1f);
			//Hot after the shot, and cooling: a glow at the muzzle, sparks and a wisp of steam coming off it.
			Draw.color(light, Color.white, f * f);
			Draw.alpha(0.25f + 0.6f * f);
			Fill.circle(mx, my, 3f + 7f * f);
			Draw.color(color, 0.25f * f);
			Fill.circle(mx, my, 6f + 12f * f);
			Draw.color(light, f);
			Lines.stroke(1.4f);
			Angles.randLenVectors(unit.id * 7L + frame, 3, 6f + 14f * (1f - f), (x, y) ->
				Lines.lineAngle(mx + x, my + y, Mathf.angle(x, y), 2f + 4f * f));
			for(int i = 0; i < 3; i++){
				float phase = (Time.time * 0.012f + i / 3f + unit.id * 0.13f) % 1f;
				Draw.color(light, 0.22f * f * (1f - phase));
				Fill.circle(mx + Angles.trnsx(unit.rotation, phase * 18f) + Mathf.sin(phase * 9f + i, 1f, 3f) * Angles.trnsx(unit.rotation + 90f, 1f), my + Angles.trnsy(unit.rotation, phase * 18f) + Mathf.sin(phase * 9f + i, 1f, 3f) * Angles.trnsy(unit.rotation + 90f, 1f), 2f + 4f * phase);
			}
			//The ring round the head fills up as the weapon recovers.
			Draw.color(color, 0.25f + 0.2f * (1f - f));
			Lines.stroke(1.6f);
			Lines.arc(unit.x, unit.y, unit.hitSize * 0.62f, 1f - f, unit.rotation);

			//Charge being spent on the cooldown: faint links from the sections that are giving it, with a spark running along each.
			if(Time.time - lastTaken < 6f){
				Draw.z(Layer.effect);
				for(int i = 0; i < givers.size; i++){
					Unit s = givers.get(i);
					if(s == null || s.dead || !s.isAdded()) continue;
					Draw.color(light, 0.22f);
					Lines.stroke(1.4f);
					Lines.line(s.x, s.y, unit.x, unit.y);
					float p = (Time.time * 0.06f + i * 0.37f) % 1f;
					Draw.color(Color.white, 0.8f);
					Fill.circle(Mathf.lerp(s.x, unit.x, p), Mathf.lerp(s.y, unit.y, p), 2.2f);
				}
			}
		}
		Draw.reset();
	}

	@Override
	public String localized(){
		return arc.Core.bundle.get("ability.nh-serpent-arc");
	}
}
