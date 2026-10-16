package newhorizon.expand.units.scavenger;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.math.Rand;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.ctype.ContentType;
import mindustry.entities.abilities.Ability;
import mindustry.entities.bullet.BulletType;
import mindustry.type.Liquid;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.world.Block;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.LiquidTurret;
import mindustry.world.blocks.defense.turrets.PowerTurret;
import mindustry.world.blocks.defense.turrets.Turret;
import newhorizon.expand.units.ablility.HealSeekAbility;
import newhorizon.expand.units.ai.gardener.GardenerAbility;

/**
 * What a Scavenger can steal, described as small text <i>recipes</i> instead of objects, so that a unit's loot can be saved with
 * the world and rebuilt later:
 * <ul>
 *     <li>{@code wu:<unit>:<weapon index>}: a weapon of a unit type</li>
 *     <li>{@code wb:<turret>:<ammo>}: a turret's gun, with the named ammo ({@code shoot} for power turrets)</li>
 *     <li>{@code au:<unit>:<ability index>}: an ability of a unit type</li>
 * </ul>
 * Suicide weapons are never offered.
 */
public final class ScavengeRecipes{
	private ScavengeRecipes(){}

	/** Suicide weapons (they kill their owner), and ones that cannot attack, are left behind. */
	public static boolean isSuicide(Weapon weapon){
		return weapon.bullet == null || weapon.shootOnDeath || weapon.bullet.killShooter || weapon.noAttack;
	}

	/** Abilities that belong to a unit's own machinery rather than to its enemies. */
	public static boolean isStealable(Ability ability){
		return !(ability instanceof ScavengerAbility || ability instanceof HealSeekAbility || ability instanceof GardenerAbility);
	}

	/**
	 * One recipe per distinct gun. A mirrored gun is two weapons in the type (left and right), but it is one gun: both halves
	 * resolve to the recipe of the first, so stealing either half is stealing the same weapon.
	 */
	public static Seq<String> weaponRecipes(UnitType type){
		Seq<String> out = new Seq<>();
		for(int i = 0; i < type.weapons.size; i++){
			if(isSuicide(type.weapons.get(i))) continue;
			String recipe = "wu:" + type.name + ":" + canonicalIndex(type, i);
			if(!out.contains(recipe)) out.add(recipe);
		}
		return out;
	}

	/** Index of the first weapon of the type that is the same gun as weapon {@code i}. */
	private static int canonicalIndex(UnitType type, int i){
		Weapon w = type.weapons.get(i);
		for(int j = 0; j < i; j++){
			Weapon other = type.weapons.get(j);
			if(java.util.Objects.equals(other.name, w.name) && other.bullet == w.bullet) return j;
		}
		return i;
	}

	/** The recipe in its one canonical spelling, so that two spellings of the same gun are recognised as equal. */
	public static String canonical(String recipe){
		String[] p = recipe.split(":", 3);
		if(p.length != 3 || !p[0].equals("wu")) return recipe;
		try{
			UnitType type = (UnitType)Vars.content.getByName(ContentType.unit, p[1]);
			int idx = Integer.parseInt(p[2]);
			if(type == null || idx < 0 || idx >= type.weapons.size) return recipe;
			return "wu:" + type.name + ":" + canonicalIndex(type, idx);
		}catch(RuntimeException e){
			return recipe;
		}
	}

	public static Seq<String> weaponRecipes(Block block){
		Seq<String> out = new Seq<>();
		if(block instanceof ItemTurret){
			((ItemTurret)block).ammoTypes.keys().toSeq().each(item -> out.add("wb:" + block.name + ":" + item.name));
		}else if(block instanceof LiquidTurret){
			((LiquidTurret)block).ammoTypes.keys().toSeq().each((Liquid liquid) -> out.add("wb:" + block.name + ":" + liquid.name));
		}else if(block instanceof PowerTurret){
			out.add("wb:" + block.name + ":shoot");
		}
		return out;
	}

	public static Seq<String> abilityRecipes(UnitType type){
		Seq<String> out = new Seq<>();
		for(int i = 0; i < type.abilities.size; i++){
			if(isStealable(type.abilities.get(i))) out.add("au:" + type.name + ":" + i);
		}
		return out;
	}

	/** A fresh weapon for the recipe, or null if it does not resolve (content removed, bad text, suicide weapon). */
	public static Weapon weapon(String recipe){
		String[] p = recipe.split(":", 3);
		if(p.length != 3) return null;

		try{
			Weapon w = null;
			if(p[0].equals("wu")){
				UnitType type = (UnitType)Vars.content.getByName(ContentType.unit, p[1]);
				if(type == null) return null;
				int idx = Integer.parseInt(p[2]);
				if(idx < 0 || idx >= type.weapons.size || isSuicide(type.weapons.get(idx))) return null;
				w = type.weapons.get(idx).copy();
				w.mirror = false;
				w.otherSide = -1;
				//A beam weapon fires a pulse and then waits out its reload. Stolen, it holds the beam for as long as it has a target.
				if(w.continuous) w.alwaysContinuous = true;
				hideSprite(w);
			}else if(p[0].equals("wb")){
				Block block = (Block)Vars.content.getByName(ContentType.block, p[1]);
				if(!(block instanceof Turret)) return null;
				BulletType bullet = turretBullet((Turret)block, p[2]);
				if(bullet == null || bullet.killShooter) return null;
				w = fromTurret((Turret)block, bullet);
				hideSprite(w);
			}
			if(w == null) return null;

			//Everything is bunched up around the middle of the body, deterministically per recipe.
			Rand r = new Rand(recipe.hashCode());
			w.x = r.range(5f);
			w.y = -2f + r.random(6f);
			w.init();
			return w;
		}catch(RuntimeException e){
			return null;
		}
	}

	/** A fresh copy of the ability for the recipe, or null. */
	public static Ability ability(String recipe, UnitType owner){
		String[] p = recipe.split(":", 3);
		if(p.length != 3 || !p[0].equals("au")) return null;

		try{
			UnitType type = (UnitType)Vars.content.getByName(ContentType.unit, p[1]);
			if(type == null) return null;
			int idx = Integer.parseInt(p[2]);
			if(idx < 0 || idx >= type.abilities.size || !isStealable(type.abilities.get(idx))) return null;
			Ability a = type.abilities.get(idx).copy();
			a.init(owner);
			return a;
		}catch(RuntimeException e){
			return null;
		}
	}

	/**
	 * The original bullet of a weapon recipe: the very object the victim's own weapon uses, not a copy. Upgrades are counted
	 * from its values, and it must never be modified.
	 */
	public static BulletType baseBullet(String recipe){
		String[] p = recipe.split(":", 3);
		if(p.length != 3) return null;
		try{
			if(p[0].equals("wu")){
				UnitType type = (UnitType)Vars.content.getByName(ContentType.unit, p[1]);
				int idx = Integer.parseInt(p[2]);
				return type == null || idx < 0 || idx >= type.weapons.size ? null : type.weapons.get(idx).bullet;
			}
			if(p[0].equals("wb")){
				Block block = (Block)Vars.content.getByName(ContentType.block, p[1]);
				return block instanceof Turret ? turretBullet((Turret)block, p[2]) : null;
			}
		}catch(RuntimeException e){
			return null;
		}
		return null;
	}

	/** The unit type's own ability a recipe refers to, with its base stats. It must never be modified. */
	public static Ability abilityTemplate(String recipe){
		String[] p = recipe.split(":", 3);
		if(p.length != 3 || !p[0].equals("au")) return null;
		try{
			UnitType type = (UnitType)Vars.content.getByName(ContentType.unit, p[1]);
			int idx = Integer.parseInt(p[2]);
			return type == null || idx < 0 || idx >= type.abilities.size ? null : type.abilities.get(idx);
		}catch(RuntimeException e){
			return null;
		}
	}

	private static BulletType turretBullet(Turret turret, String key){
		if(turret instanceof ItemTurret){
			for(mindustry.type.Item item : ((ItemTurret)turret).ammoTypes.keys()){
				if(item.name.equals(key)) return ((ItemTurret)turret).ammoTypes.get(item);
			}
		}else if(turret instanceof LiquidTurret){
			for(Liquid liquid : ((LiquidTurret)turret).ammoTypes.keys()){
				if(liquid.name.equals(key)) return ((LiquidTurret)turret).ammoTypes.get(liquid);
			}
		}else if(turret instanceof PowerTurret && key.equals("shoot")){
			return ((PowerTurret)turret).shootType;
		}
		return null;
	}

	/** A turret's gun as a unit weapon: same bullet, fire rate, spread and sound. */
	private static Weapon fromTurret(Turret turret, BulletType bullet){
		Weapon w = new Weapon();
		w.bullet = bullet;
		w.reload = turret.reload;
		w.shoot = turret.shoot.copy();
		w.inaccuracy = turret.inaccuracy;
		w.velocityRnd = turret.velocityRnd;
		w.shootCone = turret.shootCone;
		w.shootSound = turret.shootSound;
		w.recoil = Math.min(turret.recoil, 3f);
		w.shake = turret.shake;
		w.rotate = true;
		w.rotateSpeed = 14f;
		w.top = true;
		w.mirror = false;
		w.alternate = false;
		w.ejectEffect = Fx.none;
		return w;
	}

	/** Stolen unit weapons keep their owner's art, which is far too big on the Scavenger: do not draw it. */
	private static void hideSprite(Weapon w){
		//A weapon that was never loaded has no regions at all, and drawing it dies on the first null one.
		w.parts = new arc.struct.Seq<>();
		if(Core.atlas == null) return;
		TextureRegion clear = Core.atlas.find("clear");
		w.region = clear;
		w.outlineRegion = clear;
		w.heatRegion = clear;
		w.cellRegion = clear;
	}
}
