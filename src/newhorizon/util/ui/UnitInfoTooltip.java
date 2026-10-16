package newhorizon.util.ui;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.event.Touchable;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Log;
import arc.util.Scaling;
import arc.util.Strings;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.entities.abilities.StatusFieldAbility;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.Icon;
import mindustry.gen.Legsc;
import mindustry.gen.Tex;
import mindustry.gen.Unit;
import mindustry.gen.WaterMovec;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.ui.Styles;
import newhorizon.NHKeyBinding;
import newhorizon.NHSetting;
import newhorizon.expand.units.scavenger.AbilityStats;
import newhorizon.expand.units.scavenger.ScavengerAbility;

import static mindustry.Vars.tilesize;

/**
 * Hold the unit info key ({@link NHKeyBinding#unitInfo}, I by default) and click a unit to pin a panel for it: its health, armor and
 * speed, every weapon it carries and every ability it has. The panel is docked at the edge of the screen (the arrow button moves it
 * to the other side), scrolls when it is long, keeps following the unit's numbers live, and stays until it is closed with the cross
 * or another unit is picked.
 *
 * <p>It reads the unit itself rather than its type, so what a Scavenger has stolen or a Mimic has become is what shows up. Anything from
 * any mod is listed the same way. The key can be changed in the controls, and the whole thing turned off in the New Horizon settings.
 *
 * <p>{@link #describe} builds the contents as plain lines and is independent of the interface.
 */
public class UnitInfoTooltip{
	private static final String DOCK_SETTING = "nh_unit_info_dock_right";
	/** Width of the panel's contents. */
	private static final float contentWidth = 330f;

	private static Table panel;
	private static Unit pinned;
	/** The scrolling part of the panel, and where it was scrolled to before the panel was last rebuilt. */
	private static ScrollPane bodyPane;
	private static float restoreScroll = -1f;
	private static int signature;
	private static float describedAt = -1000f;
	private static boolean broken, dockRight = true;

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

		dockRight = Core.settings.getBool(DOCK_SETTING, true);
		panel = new Table();
		panel.touchable = Touchable.enabled;
		panel.visible = false;
		Core.scene.add(panel);
		//Driven from the frame update rather than from the panel itself, so it does not depend on the panel being drawn.
		Events.run(EventType.Trigger.update, UnitInfoTooltip::tick);
		Log.info("Unit info panel ready: hold the Unit info key (I by default) and click a unit.");
	}

	private static void tick(){
		if(panel == null || broken) return;
		try{
			update();
		}catch(Throwable t){
			//A bug in a purely informative panel must never take the game down: report it once and switch the panel off.
			broken = true;
			close();
			Log.err("Unit info panel failed and was switched off", t);
		}
	}

	private static void update(){
		if(!Core.settings.getBool(NHSetting.UNIT_INFO, true) || !Vars.state.isGame() || Vars.player == null){
			close();
			return;
		}

		if(NHKeyBinding.unitInfo != null && Core.input.keyDown(NHKeyBinding.unitInfo) && Core.input.keyTap(KeyCode.mouseLeft) && !Core.scene.hasField()){
			Unit picked = hovered();
			if(picked != null && picked != pinned) open(picked);
		}

		if(pinned == null) return;

		//The lists only change when something is stolen, copied or found, so ten times a second is plenty.
		if(Time.time - describedAt > 6f && alive(pinned)){
			describedAt = Time.time;
			Info info = describe(pinned);
			if(info.signature != signature || panel.getChildren().isEmpty()){
				signature = info.signature;
				rebuild(pinned, info);
			}
		}

		panel.visible = true;
		panel.toFront();
		panel.pack();
		if(restoreScroll >= 0f && bodyPane != null){
			//A rebuilt panel starts at the top: put it back where the reader was.
			bodyPane.layout();
			bodyPane.setScrollYForce(restoreScroll);
			bodyPane.updateVisualScroll();
			restoreScroll = -1f;
		}
		float w = Core.scene.getWidth(), h = Core.scene.getHeight();
		//Below the minimap on the right, below the wave and event panels on the left.
		float top = h - h * (dockRight ? 0.20f : 0.24f);
		panel.setPosition(dockRight ? w - 4f : 4f, top, dockRight ? Align.topRight : Align.topLeft);
	}

	private static boolean alive(Unit unit){
		return unit.isValid() && !unit.dead();
	}

	/** Pins the panel to a unit. */
	public static void open(Unit unit){
		pinned = unit;
		bodyPane = null;
		signature = Integer.MIN_VALUE;
		describedAt = -1000f;
	}

	/** The pinned unit turned into another one (a Scavenger that gained flight, say): keep following it, and keep the scroll position. */
	public static void replaced(Unit old, Unit now){
		if(pinned == old && now != null) pinned = now;
	}

	/** Closes the panel. */
	public static void close(){
		pinned = null;
		bodyPane = null;
		if(panel != null) panel.visible = false;
	}

	private static void toggleDock(){
		dockRight = !dockRight;
		Core.settings.put(DOCK_SETTING, dockRight);
		signature = Integer.MIN_VALUE;
		describedAt = -1000f;
	}

	/** The unit under the cursor, if any: the closest one whose body is within a few pixels of it. Units hidden by fog do not count. */
	private static Unit hovered(){
		Vec2 world = Core.input.mouseWorld();
		float mx = world.x, my = world.y;
		Unit[] best = {null};
		float[] bestDst = {Float.MAX_VALUE};
		Units.nearby(mx - 64f, my - 64f, 128f, 128f, u -> {
			if(!u.isValid() || u.dead() || u.inFogTo(Vars.player.team())) return;
			float reach = u.hitSize / 2f + 14f;
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

		//The panel is rebuilt whenever the unit's lists change (a kill, a steal): remember how far down the reader was.
		restoreScroll = bodyPane == null ? -1f : bodyPane.getScrollY();
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
				names.label(() -> info.kind + "  [lightgray]" + info.team + (alive(unit) ? "" : "  [scarlet]" + Core.bundle.get("nh.unitinfo.destroyed"))).left();
			}).growX();
			head.button(dockRight ? Icon.left : Icon.right, Styles.cleari, UnitInfoTooltip::toggleDock).size(30f).padLeft(8f);
			head.button(Icon.cancel, Styles.cleari, UnitInfoTooltip::close).size(30f).padLeft(2f);
		}).width(contentWidth).left().row();

		panel.image().color(Color.gray).height(2f).growX().padTop(4f).padBottom(4f).row();

		Table body = new Table();
		body.left().defaults().left();

		//Values that change while you look at them are read every frame.
		body.label(() -> Core.bundle.format("nh.unitinfo.health", fixed(unit.health), fixed(unit.maxHealth), (int)Mathf.clamp(unit.healthf() * 100f, 0f, 999f))).left().row();
		body.label(() -> unit.shield > 0.5f ? Core.bundle.format("nh.unitinfo.shield", fixed(unit.shield)) : "").left().row();
		for(String line : info.stats){
			body.add(line).left().row();
		}
		body.label(() -> statusLine(unit)).left().wrap().width(contentWidth - 20f).row();

		section(body, "nh.unitinfo.weapons", info.weapons);
		section(body, "nh.unitinfo.abilities", info.abilities);

		ScrollPane pane = new ScrollPane(body, Styles.noBarPane);
		pane.setScrollingDisabled(true, false);
		bodyPane = pane;
		panel.add(pane).width(contentWidth).maxHeight(Math.max(160f, Core.scene.getHeight() * 0.5f - 60f)).left().row();
	}

	private static void section(Table body, String titleKey, Seq<String> lines){
		body.add(Core.bundle.get(titleKey)).color(mindustry.graphics.Pal.accent).left().padTop(6f).row();
		if(lines.isEmpty()){
			body.add("[lightgray]" + Core.bundle.get("nh.unitinfo.none")).left().row();
			return;
		}
		for(String line : lines){
			body.add(line).left().wrap().width(contentWidth - 20f).padBottom(3f).row();
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
