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

	public static Seq<String> weaponRecipes(UnitType type){
		Seq<String> out = new Seq<>();
		for(int i = 0; i < type.weapons.size; i++){
			if(!isSuicide(type.weapons.get(i))) out.add("wu:" + type.name + ":" + i);
		}
		return out;
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
				hideSprite(w);
			}else if(p[0].equals("wb")){
				Block block = (Block)Vars.content.getByName(ContentType.block, p[1]);
				if(!(block instanceof Turret)) return null;
				BulletType bullet = turretBullet((Turret)block, p[2]);
				if(bullet == null || bullet.killShooter) return null;
				w = fromTurret((Turret)block, bullet);
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
		if(Core.atlas == null) return;
		TextureRegion clear = Core.atlas.find("clear");
		w.region = clear;
		w.outlineRegion = clear;
		w.heatRegion = clear;
		w.cellRegion = clear;
	}
}
