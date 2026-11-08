package newhorizon.content.heavy;

import arc.graphics.g2d.TextureRegion;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.entities.bullet.*;
import mindustry.entities.part.DrawPart;
import mindustry.entities.part.RegionPart;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.consumers.ConsumeCoolant;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawTurret;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

/**
 * Heavy turrets: a more powerful version of each of the base game's 18 Serpulo turrets, with the same block size, the same ammo and
 * coolant, about 1.5 times the damage and range, a faster reload and twice the health. Each is <b>derived in code from the original</b> (the
 * block's own fields, every ammo bullet copied and boosted, its drawer and animated parts copied), so every kind of turret (item, liquid,
 * power, laser, tractor, point defence) keeps working exactly as its original and any ammo is covered. Their sprites are the originals'
 * images with a new colour family and added armour (see the art script), named {@code heavy-<turret>}.
 */
public class HeavyTurrets{
	/** How much stronger. */
	public static final float damage = 1.5f, range = 1.5f, reload = 0.7f, health = 2f, ammo = 1.5f, rotate = 1.25f, power = 1.5f, cost = 4f;
	public static final float armorAdded = 4f;

	/** Each original and its heavy version, in the order they are made. */
	public static final ObjectMap<Block, Block> heavyOf = new ObjectMap<>();
	public static final Seq<Block> all = new Seq<>();
	/** The ones whose drawer could not be copied (they use the default drawer), for the tests to report. */
	public static final Seq<Block> plainDrawer = new Seq<>();

	public static void load(){
		for(Block original : new Block[]{Blocks.duo, Blocks.scatter, Blocks.scorch, Blocks.hail, Blocks.wave, Blocks.lancer, Blocks.arc, Blocks.parallax,
			Blocks.swarmer, Blocks.salvo, Blocks.segment, Blocks.tsunami, Blocks.fuse, Blocks.ripple, Blocks.cyclone, Blocks.foreshadow, Blocks.spectre,
			Blocks.meltdown}){
			Block heavy = derive(original);
			heavyOf.put(original, heavy);
			all.add(heavy);
		}
	}

	/** The heavy version of a base-game turret, or null. */
	public static Block of(Block original){
		return heavyOf.get(original);
	}

	private static Block derive(Block o){
		String name = "heavy-" + o.name;
		Class<?> base;
		Block h;
		if(o instanceof LaserTurret){
			h = new LaserTurret(name);
			base = LaserTurret.class;
		}else if(o instanceof PowerTurret){
			h = new PowerTurret(name);
			base = PowerTurret.class;
		}else if(o instanceof ItemTurret){
			h = new ItemTurret(name);
			base = ItemTurret.class;
		}else if(o instanceof LiquidTurret){
			h = new LiquidTurret(name);
			base = LiquidTurret.class;
		}else if(o instanceof TractorBeamTurret){
			h = new TractorBeamTurret(name);
			base = TractorBeamTurret.class;
		}else if(o instanceof PointDefenseTurret){
			h = new PointDefenseTurret(name);
			base = PointDefenseTurret.class;
		}else{
			throw new IllegalArgumentException(o.name + " is not a turret kind that can be made heavy");
		}

		//Everything the turret classes declare (range, reload, shoot pattern, sounds, effects, targeting...) is copied as it is...
		copyFields(o, h, base);
		//...and the block's own settings that matter.
		h.size = o.size;
		h.hasPower = o.hasPower;
		h.hasLiquids = o.hasLiquids;
		h.liquidCapacity = o.liquidCapacity;
		h.envEnabled = o.envEnabled;
		if(o.flags != null) h.flags = o.flags;

		//Tougher.
		h.scaledHealth = o.scaledHealth * health;
		h.health = o.health > 0 ? Math.round(o.health * health) : o.health;
		h.armor = o.armor + armorAdded;

		//Longer, quicker.
		BaseTurret bt = (BaseTurret)h;
		bt.range = ((BaseTurret)o).range * range;
		bt.rotateSpeed = ((BaseTurret)o).rotateSpeed * rotate;
		if(h instanceof ReloadTurret) ((ReloadTurret)h).reload = ((ReloadTurret)o).reload * reload;
		if(h instanceof Turret){
			Turret ht = (Turret)h, ot = (Turret)o;
			ht.minRange = ot.minRange * range;
			ht.trackingRange = ot.trackingRange * range;
			ht.maxAmmo = Math.round(ot.maxAmmo * ammo);
		}

		//Same coolant, more power.
		if(((BaseTurret)o).coolant instanceof ConsumeCoolant){
			bt.coolant = h.consumeCoolant(((ConsumeCoolant)((BaseTurret)o).coolant).amount);
		}
		if(o.consPower != null && !o.consPower.buffered){
			h.consumePower(o.consPower.usage * power);
		}

		//Same ammo, every bullet boosted.
		if(o instanceof ItemTurret){
			((ItemTurret)h).ammo(boosted(((ItemTurret)o).ammoTypes));
		}else if(o instanceof LiquidTurret){
			((LiquidTurret)h).ammo(boosted(((LiquidTurret)o).ammoTypes));
		}
		if(o instanceof PowerTurret){
			((PowerTurret)h).shootType = boost(((PowerTurret)o).shootType, 0);
		}

		//The special turrets.
		if(o instanceof TractorBeamTurret){
			TractorBeamTurret ot = (TractorBeamTurret)o, ht = (TractorBeamTurret)h;
			ht.force = ot.force * damage;
			ht.scaledForce = ot.scaledForce * damage;
			ht.damage = ot.damage * damage;
		}
		if(o instanceof PointDefenseTurret){
			((PointDefenseTurret)h).bulletDamage = ((PointDefenseTurret)o).bulletDamage * damage;
		}

		//The same items as the original, `cost` times over (no new item).
		h.requirements(Category.turret, price(o));

		//The original's drawer, with its animated parts, copied: the images it asks for are the heavy ones.
		if(o instanceof Turret){
			DrawBlock d = copyDrawer(((Turret)o).drawer);
			if(d == null){
				d = new DrawTurret();
				plainDrawer.add(h);
			}
			((Turret)h).drawer = d;
		}
		return h;
	}

	//---------------------------------------------------------------- stats

	/** The original's own items, {@link #cost} times over. */
	private static ItemStack[] price(Block o){
		Seq<ItemStack> out = new Seq<>();
		for(ItemStack s : o.requirements){
			out.add(new ItemStack(s.item, Math.round(s.amount * cost)));
		}
		return out.toArray(ItemStack.class);
	}

	private static Object[] boosted(ObjectMap<? extends mindustry.ctype.Content, BulletType> map){
		Seq<Object> out = new Seq<>();
		for(ObjectMap.Entry<? extends mindustry.ctype.Content, BulletType> e : map){
			out.add(e.key, boost(e.value, 0));
		}
		return out.toArray(Object.class);
	}

	/** A copy of the bullet that hits harder and, to match the longer turret range, reaches farther. */
	public static BulletType boost(BulletType b, int depth){
		if(b == null) return null;
		BulletType c = b.copy();
		c.damage *= damage;
		c.splashDamage *= damage;
		if(c.lightningDamage > 0f) c.lightningDamage *= damage;
		c.rangeChange *= range;
		if(c.rangeOverride > 0f) c.rangeOverride *= range;
		if(c.lightning > 0) c.lightningLength = Math.round(c.lightningLength * range);
		if(c instanceof LiquidBulletType) c.knockback *= damage;

		//Reach: the length of lasers and rails, the speed of a point shot, the flight time of everything else that flies.
		if(c instanceof LaserBulletType) ((LaserBulletType)c).length *= range;
		else if(c instanceof ContinuousBulletType) ((ContinuousBulletType)c).length *= range;
		else if(c instanceof RailBulletType) ((RailBulletType)c).length *= range;
		else if(c instanceof ShrapnelBulletType) ((ShrapnelBulletType)c).length *= range;
		else if(c instanceof SapBulletType) ((SapBulletType)c).length *= range;
		else if(c instanceof PointBulletType) c.speed *= range;
		else if(c.speed > 0f) c.lifetime *= range;

		if(depth < 4){
			if(c.fragBullet != null) c.fragBullet = boost(c.fragBullet, depth + 1);
			if(c.intervalBullet != null) c.intervalBullet = boost(c.intervalBullet, depth + 1);
			if(c.spawnBullets != null && c.spawnBullets.size > 0){
				Seq<BulletType> spawn = new Seq<>();
				for(BulletType s : c.spawnBullets) spawn.add(boost(s, depth + 1));
				c.spawnBullets = spawn;
			}
		}
		return c;
	}

	//---------------------------------------------------------------- copying

	private static final Set<String> skip = Set.of("coolant", "ammoTypes", "drawer", "shootType", "childParam");

	/** Copies the fields declared by the turret classes (from {@code base} up to Block, not including it) from one block to another. */
	private static void copyFields(Object from, Object to, Class<?> base){
		for(Class<?> c = base; c != null && c != Block.class; c = c.getSuperclass()){
			for(Field f : c.getDeclaredFields()){
				int m = f.getModifiers();
				if(Modifier.isStatic(m) || Modifier.isFinal(m) || skip.contains(f.getName()) || TextureRegion.class.isAssignableFrom(f.getType())) continue;
				try{
					f.setAccessible(true);
					f.set(to, f.get(from));
				}catch(ReflectiveOperationException | RuntimeException e){
					Log.warn("Heavy turret: could not copy @.@: @", c.getSimpleName(), f.getName(), e.toString());
				}
			}
		}
	}

	/** A new drawer with the original's parts copied, or null if some part cannot be copied. */
	private static DrawBlock copyDrawer(DrawBlock d){
		if(!(d instanceof DrawTurret)) return null;
		DrawTurret t = (DrawTurret)d, n = new DrawTurret(t.basePrefix);
		n.liquidDraw = t.liquidDraw;
		n.turretLayer = t.turretLayer;
		n.shadowLayer = t.shadowLayer;
		n.heatLayer = t.heatLayer;
		if(t.ammoParts != null && t.ammoParts.size > 0) return null;
		for(DrawPart p : t.parts){
			DrawPart c = copyPart(p);
			if(c == null) return null;
			n.parts.add(c);
		}
		return n;
	}

	private static DrawPart copyPart(DrawPart p){
		if(!(p instanceof RegionPart)) return null;
		RegionPart c = new RegionPart();
		copyFields(p, c, RegionPart.class);
		for(Class<?> k = DrawPart.class; k != null && k != Object.class; k = k.getSuperclass()){
			for(Field f : k.getDeclaredFields()){
				int m = f.getModifiers();
				if(Modifier.isStatic(m) || Modifier.isFinal(m)) continue;
				try{
					f.setAccessible(true);
					f.set(c, f.get(p));
				}catch(ReflectiveOperationException | RuntimeException ignored){
				}
			}
		}
		//The children are parts too.
		RegionPart source = (RegionPart)p;
		if(source.children != null){
			c.children = new Seq<>();
			for(DrawPart child : source.children){
				DrawPart cc = copyPart(child);
				if(cc == null) return null;
				c.children.add(cc);
			}
		}
		return c;
	}
}
