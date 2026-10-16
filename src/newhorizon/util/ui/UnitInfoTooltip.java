package newhorizon.util.ui;

import arc.Core;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Scaling;
import arc.util.Strings;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.entities.abilities.StatusFieldAbility;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.Legsc;
import mindustry.gen.Tex;
import mindustry.gen.Unit;
import mindustry.gen.WaterMovec;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.ui.Styles;
import newhorizon.NHSetting;
import newhorizon.expand.units.scavenger.AbilityStats;
import newhorizon.expand.units.scavenger.ScavengerAbility;

import static mindustry.Vars.tilesize;

/**
 * Hover a unit for a moment and a panel shows what it is: health, armor and speed, every weapon it carries and every ability it has.
 * It reads the unit itself rather than its type, so what a Scavenger has stolen or a Mimic has become is what shows up. Anything from
 * any mod is listed the same way. Turn it off in the New Horizon settings.
 *
 * <p>{@link #describe} builds the contents as plain lines and is independent of the interface.
 */
public class UnitInfoTooltip{
	/** Time the cursor has to rest on a unit before the panel appears, in ticks. */
	public static final float hoverDelay = 12f;

	private static Table panel;
	private static Unit shown;
	private static float hoverTime;
	private static int signature;
	private static boolean visible;

	/** Everything the panel says about a unit. */
	public static class Info{
		public String name, team;
		/** What the unit is: the movement kind and size. */
		public String kind;
		public final Seq<String> stats = new Seq<>(), weapons = new Seq<>(), abilities = new Seq<>();
		public Color teamColor = Color.white;
		/** Changes whenever the lists above would change, so the panel knows when to rebuild. */
		public int signature;
	}

	public static void init(){
		if(Vars.headless || Vars.mobile || panel != null) return;

		panel = new Table();
		panel.touchable = Touchable.disabled;
		panel.visible(() -> visible);
		panel.update(UnitInfoTooltip::update);
		Core.scene.add(panel);
	}

	private static void update(){
		if(!Core.settings.getBool(NHSetting.UNIT_INFO, true) || !Vars.state.isGame() || Vars.player == null
			|| Core.scene.hasMouse() || Vars.control.input.isPlacing()){
			hide();
			return;
		}

		Unit unit = hovered();
		if(unit == null){
			hide();
			return;
		}

		if(unit != shown){
			shown = unit;
			hoverTime = 0f;
			visible = false;
		}
		hoverTime += Time.delta;
		if(hoverTime < hoverDelay) return;

		Info info = describe(unit);
		if(!visible || info.signature != signature){
			signature = info.signature;
			rebuild(unit, info);
		}
		visible = true;

		Vec2 at = Core.scene.screenToStageCoordinates(Tmp.v1.set(Core.input.mouse()));
		panel.pack();
		float x = Math.min(at.x + 22f, Core.scene.getWidth() - panel.getWidth() - 4f);
		float y = Math.max(at.y - 22f, panel.getHeight() + 4f);
		panel.setPosition(x, y, Align.topLeft);
	}

	private static void hide(){
		visible = false;
		shown = null;
		hoverTime = 0f;
	}

	/** The unit under the cursor, if any: the closest one whose body the cursor is over. Units hidden by fog do not count. */
	private static Unit hovered(){
		Vec2 world = Core.input.mouseWorld();
		float mx = world.x, my = world.y;
		Unit[] best = {null};
		float[] bestDst = {Float.MAX_VALUE};
		Units.nearby(mx - 48f, my - 48f, 96f, 96f, u -> {
			if(!u.isValid() || u.dead() || u.inFogTo(Vars.player.team())) return;
			float reach = u.hitSize / 2f + 4f;
			float dst = u.dst2(mx, my);
			if(dst <= reach * reach && dst < bestDst[0]){
				best[0] = u;
				bestDst[0] = dst;
			}
		});
		return best[0];
	}

	private static void rebuild(Unit unit, Info info){
		UnitType type = unit.type;

		panel.clear();
		panel.background(Tex.pane);
		panel.margin(8f);
		panel.left().defaults().left();

		panel.table(head -> {
			head.left();
			head.image(type.uiIcon).size(36f).scaling(Scaling.fit).padRight(8f);
			head.table(names -> {
				names.left();
				names.add(info.name).color(info.teamColor).left().row();
				names.add(info.kind + "  [lightgray]" + info.team).left();
			});
		}).left().row();

		panel.image().color(Color.gray).height(2f).growX().padTop(4f).padBottom(4f).row();

		//Values that change while you look at them are read every frame.
		panel.label(() -> Core.bundle.format("nh.unitinfo.health", fixed(unit.health), fixed(unit.maxHealth), (int)Mathf.clamp(unit.healthf() * 100f, 0f, 999f))).left().row();
		panel.label(() -> unit.shield > 0.5f ? Core.bundle.format("nh.unitinfo.shield", fixed(unit.shield)) : "").left().row();
		for(String line : info.stats){
			panel.add(line).left().row();
		}
		panel.label(() -> statusLine(unit)).left().wrap().width(260f).row();

		section("nh.unitinfo.weapons", info.weapons);
		section("nh.unitinfo.abilities", info.abilities);
	}

	private static void section(String titleKey, Seq<String> lines){
		panel.add(Core.bundle.get(titleKey)).color(mindustry.graphics.Pal.accent).left().padTop(6f).row();
		if(lines.isEmpty()){
			panel.add("[lightgray]" + Core.bundle.get("nh.unitinfo.none")).left().row();
			return;
		}
		for(String line : lines){
			panel.add(line).left().wrap().width(280f).row();
		}
	}

	/** Names of the statuses the unit has now, with the time left on those that run out. */
	public static String statusLine(Unit unit){
		StringBuilder sb = new StringBuilder();
		for(StatusEffect effect : Vars.content.statusEffects()){
			if(effect == Vars.content.statusEffects().first() || !effect.show) continue;
			float left = unit.getDuration(effect);
			if(left <= 0f && !unit.hasEffect(effect)) continue;
			if(sb.length() > 0) sb.append(", ");
			sb.append(effect.localizedName);
			if(!effect.permanent && left > 0f && left < 60f * 3600f) sb.append(" ").append((int)Math.ceil(left / 60f)).append("s");
		}
		return sb.length() == 0 ? "" : Core.bundle.get("nh.unitinfo.status") + " " + sb;
	}

	/** Describes a unit: from the unit rather than its type, so changed weapons and abilities are included. */
	public static Info describe(Unit unit){
		UnitType type = unit.type;
		Info info = new Info();
		info.name = type.localizedName;
		info.team = unit.team.localized();
		info.teamColor = unit.team.color;

		String movement = type.flying ? "nh.unitinfo.air" : unit instanceof WaterMovec ? "nh.unitinfo.naval" : unit instanceof Legsc ? "nh.unitinfo.legs" : type.hovering ? "nh.unitinfo.hover" : "nh.unitinfo.ground";
		info.kind = Core.bundle.get(movement);

		info.stats.add(Core.bundle.format("nh.unitinfo.armor", fixed(unit.armor)));
		info.stats.add(Core.bundle.format("nh.unitinfo.speed", fixed(type.speed * unit.speedMultiplier * 60f / tilesize)));
		info.stats.add(Core.bundle.format("nh.unitinfo.size", fixed(unit.hitSize / tilesize)));

		//Mirrored weapons are the same weapon twice: list each kind once with a count.
		ObjectIntMap<String> counts = new ObjectIntMap<>();
		ObjectMap<String, Weapon> first = new ObjectMap<>();
		Seq<String> order = new Seq<>();
		float totalDps = 0f;
		for(WeaponMount mount : unit.mounts){
			Weapon weapon = mount.weapon;
			if(weapon == null || weapon.bullet == null) continue;
			String key = weapon.name + "|" + System.identityHashCode(weapon.bullet) + "|" + weapon.reload;
			if(!counts.containsKey(key)){
				first.put(key, weapon);
				order.add(key);
			}
			counts.increment(key, 0, 1);
		}
		for(String key : order){
			Weapon weapon = first.get(key);
			int n = counts.get(key, 1);
			totalDps += safeDps(weapon) * n;
			info.weapons.add(weaponLine(weapon, n));
		}
		if(info.weapons.size > 0) info.stats.add(Core.bundle.format("nh.unitinfo.dps", fixed(totalDps)));

		int abilityHash = 0;
		for(Ability ability : unit.abilities){
			info.abilities.add(abilityLine(ability));
			abilityHash = abilityHash * 31 + ability.getClass().hashCode();
			if(ability instanceof ScavengerAbility){
				ScavengerAbility s = (ScavengerAbility)ability;
				abilityHash = abilityHash * 31 + s.weapons.size * 7 + s.abilities.size * 13 + s.kills;
			}
		}

		info.signature = unit.id * 31 + unit.mounts.length * 7 + abilityHash;
		return info;
	}

	private static float safeDps(Weapon weapon){
		try{
			float dps = weapon.dps();
			return Float.isNaN(dps) || Float.isInfinite(dps) ? 0f : dps;
		}catch(RuntimeException e){
			return 0f;
		}
	}

	private static String weaponLine(Weapon weapon, int count){
		BulletType bullet = weapon.bullet;
		StringBuilder sb = new StringBuilder();
		sb.append(weapon.name == null || weapon.name.isEmpty() ? Core.bundle.get("nh.unitinfo.weapon") : prettify(weapon.name));
		if(count > 1) sb.append(" [lightgray]x").append(count).append("[]");
		sb.append("\n[lightgray]");

		if(weapon.shootOnDeath || bullet.killShooter){
			sb.append(Core.bundle.get("nh.unitinfo.selfdestruct")).append("  ");
		}
		float damage = bullet.damage;
		if(damage > 0f && !(damage <= 1f && bullet.splashDamage > 0f)) sb.append(Core.bundle.format("nh.unitinfo.damage", fixed(damage))).append("  ");
		if(bullet.splashDamage > 0f) sb.append(Core.bundle.format("nh.unitinfo.splash", fixed(bullet.splashDamage))).append("  ");
		float shots = Math.max(1, weapon.shoot == null ? 1 : weapon.shoot.shots);
		if(shots > 1f) sb.append(Core.bundle.format("nh.unitinfo.shots", fixed(shots))).append("  ");
		if(weapon.reload > 0f) sb.append(Core.bundle.format("nh.unitinfo.rate", fixed(60f / weapon.reload))).append("  ");
		float range = rangeOf(weapon);
		if(range > 0f) sb.append(Core.bundle.format("nh.unitinfo.range", fixed(range / tilesize))).append("  ");
		if(bullet.status != null && bullet.status.show && bullet.status.localizedName != null && bullet.status != Vars.content.statusEffects().first()){
			sb.append(bullet.status.localizedName).append("  ");
		}
		if(bullet.buildingDamageMultiplier <= 0f) sb.append(Core.bundle.get("nh.unitinfo.units-only")).append("  ");
		float dps = safeDps(weapon);
		if(dps > 0f) sb.append(Core.bundle.format("nh.unitinfo.dpsone", fixed(dps)));
		sb.append("[]");
		return sb.toString();
	}

	private static float rangeOf(Weapon weapon){
		try{
			return weapon.range();
		}catch(RuntimeException e){
			return 0f;
		}
	}

	private static String abilityLine(Ability ability){
		StringBuilder sb = new StringBuilder();
		String name;
		try{
			name = ability.localized();
		}catch(RuntimeException e){
			name = null;
		}
		if(name == null || name.isEmpty() || name.startsWith("???")) name = prettify(ability.getClass().getSimpleName().replace("Ability", ""));
		sb.append(name);

		StringBuilder detail = new StringBuilder();
		if(ability instanceof StatusFieldAbility){
			StatusEffect effect = ((StatusFieldAbility)ability).effect;
			if(effect != null) detail.append(effect.localizedName).append("  ");
		}
		for(AbilityStats.Stat stat : AbilityStats.of(ability)){
			try{
				float value = AbilityStats.get(ability, stat.field);
				detail.append(statText(stat.field, value)).append("  ");
			}catch(RuntimeException ignored){
			}
		}
		if(detail.length() > 0) sb.append("\n[lightgray]").append(detail.toString().trim()).append("[]");

		if(ability instanceof ScavengerAbility){
			ScavengerAbility s = (ScavengerAbility)ability;
			sb.append("\n[lightgray]").append(Core.bundle.format("nh.unitinfo.loot", fixed(s.hpBonus), fixed(s.armorBonus), s.kills, s.weapons.size, s.abilities.size)).append("[]");
		}
		return sb.toString();
	}

	/** One tunable value of an ability in words and units: times in seconds, distances in tiles. */
	private static String statText(String field, float value){
		switch(field){
			case "reload": case "cooldown": case "spawnTime": case "duration":
				return prettify(field) + " " + fixed(value / 60f) + "s";
			case "range": case "radius":
				return prettify(field) + " " + fixed(value / tilesize);
			default:
				return prettify(field) + " " + fixed(value);
		}
	}

	/** "new-horizon-sprig-needler" or "healSeek" as readable words. */
	public static String prettify(String name){
		if(name == null) return "";
		String s = name.replace("new-horizon-", "");
		s = s.replaceAll("([a-z])([A-Z])", "$1 $2").replace('-', ' ').replace('_', ' ').trim();
		return s.isEmpty() ? name : Strings.capitalize(s);
	}

	private static String fixed(float value){
		return Strings.autoFixed(value, value >= 100f ? 0 : 1);
	}
}
