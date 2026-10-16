package newhorizon.expand.units.scavenger;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Rand;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.entities.abilities.Ability;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.type.Weapon;
import newhorizon.content.jade.JadeColor;
import newhorizon.content.jade.JadeFx;

import java.util.Arrays;

/**
 * A Scavenger's loot: bonus health and armor stolen from enemies it helped destroy (no limit), plus enemy weapons and abilities,
 * which stack. Each unit has its own copy of this ability, and so its own loot.
 *
 * <p>The loot is kept as numbers and {@link ScavengeRecipes recipes} and mirrored into the world rules' tags under the unit's id,
 * so it survives saving and loading: the first time the ability runs on a unit it restores whatever the world remembers.
 */
public class ScavengerAbility extends Ability{
	/** Weapons and abilities pile up, but not without end: every mount costs time on every frame. */
	public static final int maxWeapons = 32, maxAbilities = 16;
	public static final String TAG = "nh-scavenger-";
	/** A weapon can never fire faster than this (ticks between shots), however often it is upgraded. */
	public static final float minReload = 4f;

	public float hpBonus, armorBonus;
	public int kills;
	/** Stolen weapons, unique by recipe, in the order they were taken. */
	public Seq<String> weapons = new Seq<>();
	public Seq<String> abilities = new Seq<>();
	/** Upgrades of stolen weapons, in order: weapon index followed by d (damage) or f (fire rate). */
	public Seq<String> upgrades = new Seq<>();

	protected boolean restored;

	/** Outcome of one reward, for tests and effects. */
	public static class Loot{
		public float hp, armor;
		/** Recipe of a weapon newly added, of one that was upgraded, and which stat it was. */
		public String weapon, upgradedWeapon, upgradedStat, ability;
	}

	/**
	 * Every unit gets its ability by cloning the type's template, and a plain clone would share these lists between all
	 * Scavengers: they must not share their loot.
	 */
	@Override
	public Ability copy(){
		ScavengerAbility copy = (ScavengerAbility)super.copy();
		copy.weapons = new Seq<>(weapons);
		copy.abilities = new Seq<>(abilities);
		copy.upgrades = new Seq<>(upgrades);
		return copy;
	}

	@Override
	public void update(Unit unit){
		if(!restored) restore(unit);
	}

	/** Pays out for one enemy this unit helped to destroy. */
	public Loot reward(Unit unit, float victimMaxHealth, float victimArmor, Seq<String> weaponRecipes, Seq<String> abilityRecipes, Rand rand){
		if(!restored) restore(unit);
		Loot loot = new Loot();
		kills++;

		loot.hp = Math.max(0f, victimMaxHealth) * rand.random(Scavenging.minSteal, Scavenging.maxSteal);
		loot.armor = Math.max(0f, victimArmor) * rand.random(Scavenging.minSteal, Scavenging.maxSteal);
		boost(unit, loot.hp, loot.armor);

		if(!weaponRecipes.isEmpty() && rand.chance(Scavenging.weaponChance)){
			String recipe = ScavengeRecipes.canonical(weaponRecipes.get(rand.random(weaponRecipes.size - 1)));
			int owned = weapons.indexOf(recipe);
			if(owned >= 0){
				//Weapons are unique: taking the same gun again makes the one it already has better.
				String stat = upgradeWeapon(unit, owned, rand);
				if(stat != null){
					loot.upgradedWeapon = recipe;
					loot.upgradedStat = stat;
				}
			}else if(addWeapon(unit, recipe)){
				loot.weapon = recipe;
			}
		}
		if(!abilityRecipes.isEmpty() && rand.chance(Scavenging.abilityChance)){
			String recipe = abilityRecipes.get(rand.random(abilityRecipes.size - 1));
			if(addAbility(unit, recipe)) loot.ability = recipe;
		}

		save(unit);
		JadeFx.leafBurst.at(unit.x, unit.y);
		if(loot.weapon != null || loot.ability != null || loot.upgradedWeapon != null) JadeFx.pollenBurst.at(unit.x, unit.y);
		return loot;
	}

	/** Steals health and armor. Max health grows by the same amount the unit is healed by. */
	protected void boost(Unit unit, float hp, float armor){
		hpBonus += hp;
		armorBonus += armor;
		unit.maxHealth += hp;
		unit.health += hp;
		unit.armor += armor;
	}

	/** Adds a weapon the unit does not have yet. Refuses one it already owns (see {@link #upgradeWeapon}). */
	public boolean addWeapon(Unit unit, String recipe){
		recipe = ScavengeRecipes.canonical(recipe);
		if(weapons.size >= maxWeapons || weapons.contains(recipe)) return false;
		Weapon weapon = ScavengeRecipes.weapon(recipe);
		if(weapon == null) return false;

		WeaponMount[] mounts = Arrays.copyOf(unit.mounts, unit.mounts.length + 1);
		mounts[mounts.length - 1] = new WeaponMount(weapon);
		unit.mounts = mounts;
		weapons.add(recipe);
		return true;
	}

	/**
	 * Improves one random stat of an owned weapon: its damage or its fire rate, by {@link Scavenging#upgradeStep}.
	 * Fire rate stops at {@link #minReload}; once it has, only damage is left to improve.
	 *
	 * @return which stat was improved, or null if nothing could be
	 */
	public String upgradeWeapon(Unit unit, int index, Rand rand){
		int mount = unit.type.weapons.size + index;
		if(index < 0 || index >= weapons.size || mount >= unit.mounts.length) return null;

		boolean canSpeedUp = unit.mounts[mount].weapon.reload > minReload + 0.001f;
		boolean damage = !canSpeedUp || rand.chance(0.5f);
		applyUpgrade(unit, index, damage);
		upgrades.add(index + (damage ? "d" : "f"));
		return damage ? "damage" : "fire rate";
	}

	/**
	 * The stolen weapon's bullet is the very object the victim's own weapon uses. Raising its damage in place would strengthen
	 * every such unit and turret in the game, so the upgrade works on a private copy.
	 */
	protected void applyUpgrade(Unit unit, int index, boolean damage){
		Weapon weapon = unit.mounts[unit.type.weapons.size + index].weapon;
		float k = 1f + Scavenging.upgradeStep;
		if(damage){
			BulletType bullet = weapon.bullet.copy();
			bullet.damage *= k;
			bullet.splashDamage *= k;
			if(bullet.lightningDamage > 0f) bullet.lightningDamage *= k;
			weapon.bullet = bullet;
		}else{
			weapon.reload = Math.max(minReload, weapon.reload / k);
		}
	}

	public boolean addAbility(Unit unit, String recipe){
		if(abilities.size >= maxAbilities) return false;
		Ability ability = ScavengeRecipes.ability(recipe, unit.type);
		if(ability == null) return false;

		Ability[] all = Arrays.copyOf(unit.abilities, unit.abilities.length + 1);
		all[all.length - 1] = ability;
		unit.abilities = all;
		abilities.add(recipe);
		return true;
	}

	//---------------------------------------------------------------- persistence

	public String serialize(){
		return "v1;hp=" + hpBonus + ";armor=" + armorBonus + ";kills=" + kills + ";w=" + weapons.toString(",") + ";u=" + upgrades.toString(",") + ";a=" + abilities.toString(",");
	}

	/** Rebuilds the loot from text. Anything that no longer resolves is simply dropped. Never throws. */
	public void apply(Unit unit, String text){
		if(text == null || !text.startsWith("v1;")) return;
		for(String part : text.split(";")){
			int eq = part.indexOf('=');
			if(eq < 0) continue;
			String key = part.substring(0, eq), val = part.substring(eq + 1);
			try{
				switch(key){
					case "hp": boost(unit, Math.max(0f, Float.parseFloat(val)), 0f); break;
					case "armor": boost(unit, 0f, Math.max(0f, Float.parseFloat(val))); break;
					case "kills": kills = Math.max(0, Integer.parseInt(val)); break;
					case "w": for(String r : val.split(",")) if(!r.isEmpty()) addWeapon(unit, r); break;
					case "u":
						for(String r : val.split(",")){
							//One bad entry must not cost the good ones after it.
							try{
								if(r.length() < 2) continue;
								int idx = Integer.parseInt(r.substring(0, r.length() - 1));
								char stat = r.charAt(r.length() - 1);
								if(idx < 0 || idx >= weapons.size || (stat != 'd' && stat != 'f')) continue;
								applyUpgrade(unit, idx, stat == 'd');
								upgrades.add(r);
							}catch(RuntimeException ignored){
							}
						}
						break;
					case "a": for(String r : val.split(",")) if(!r.isEmpty()) addAbility(unit, r); break;
					default: break;
				}
			}catch(RuntimeException ignored){
			}
		}
	}

	public void save(Unit unit){
		if(Vars.net != null && Vars.net.client()) return;
		Vars.state.rules.tags.put(TAG + unit.id, serialize());
	}

	/** Called once per unit: picks up the loot the world remembers for it, if any. */
	public void restore(Unit unit){
		restored = true;
		if(Vars.state == null) return;
		apply(unit, Vars.state.rules.tags.get(TAG + unit.id));
	}

	@Override
	public void death(Unit unit){
		if(Vars.state != null) Vars.state.rules.tags.remove(TAG + unit.id);
	}

	@Override
	public void draw(Unit unit){
		int n = Math.min(8, weapons.size + abilities.size);
		if(n <= 0) return;

		float y = unit.y + unit.hitSize / 2f + 7f;
		float x0 = unit.x - (n - 1) * 2.4f;
		Draw.z(Layer.flyingUnit + 1f);
		for(int i = 0; i < n; i++){
			Draw.color(Color.black, 0.45f);
			Fill.square(x0 + i * 4.8f, y - 0.5f, 1.9f, 45f);
			Draw.color(i < weapons.size ? JadeColor.resin : JadeColor.jadeLight);
			Fill.square(x0 + i * 4.8f, y, 1.5f, 45f);
		}
		Draw.reset();
	}

	@Override
	public String localized(){
		return Core.bundle.get("ability.nh-scavenger", "Scavenger");
	}
}
