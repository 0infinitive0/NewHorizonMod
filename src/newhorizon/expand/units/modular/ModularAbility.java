package newhorizon.expand.units.modular;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Angles;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.entities.abilities.Ability;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.type.Weapon;

import java.util.Arrays;

/**
 * What makes a frame modular: it keeps which part sits in each slot and turns that into the unit's guns, its extra ability and its
 * status effect. The frame has no guns of its own mounted: the {@link ModulePart#left left} and {@link ModulePart#right right} versions of
 * every arm weapon are listed on the unit type (so that their sprites are loaded and outlined), and {@link #apply} replaces the unit's
 * mounts with just the ones the arms carry.
 *
 * <p>Like the Scavenger, the unit's loadout is mirrored into the world rules' tags under the unit's id, so that it survives saving and
 * loading: the first time the ability runs on a unit, it restores whatever the world remembers. Changing a part counts as the first
 * time too, so that an old tag never overwrites a new fit.
 */
public class ModularAbility extends Ability{
	public static final String TAG = "nh-modular-";

	/** The part id in each slot, -1 for an empty slot. */
	public int[] installed = {-1, -1, -1, -1};

	protected boolean restored;
	/** The core part the unit's extra ability belongs to, so that refitting an arm does not reset it. */
	protected int builtCore = -2;
	protected Ability coreAbility;

	/** The modular ability of a unit, or null if it has none. */
	public static ModularAbility of(Unit unit){
		if(unit == null) return null;
		for(Ability a : unit.abilities){
			if(a instanceof ModularAbility) return (ModularAbility)a;
		}
		return null;
	}

	@Override
	public Ability copy(){
		ModularAbility copy = (ModularAbility)super.copy();
		copy.installed = installed.clone();
		copy.coreAbility = null;
		copy.builtCore = -2;
		return copy;
	}

	@Override
	public void update(Unit unit){
		if(!restored) restore(unit);
	}

	public ModulePart part(int slot){
		return slot >= 0 && slot < installed.length ? ModulePart.get(installed[slot]) : null;
	}

	/** Puts the part into the slot (replacing what was there), if it fits. Returns the part that was replaced, or null. */
	public ModulePart install(Unit unit, int slot, ModulePart part){
		if(part == null || !part.fits(slot)) return null;
		restored = true;
		ModulePart old = part(slot);
		installed[slot] = part.id;
		apply(unit);
		save(unit);
		return old;
	}

	/** Empties the slot. Returns the part that was there, or null. */
	public ModulePart remove(Unit unit, int slot){
		if(slot < 0 || slot >= installed.length) return null;
		restored = true;
		ModulePart old = part(slot);
		installed[slot] = -1;
		apply(unit);
		save(unit);
		return old;
	}

	/** Turns the slots into the unit's mounts, ability and status. */
	public void apply(Unit unit){
		//The guns: only the arms' own, each keeping its mount (its aim and reload) if it stays.
		WeaponMount[] before = unit.mounts;
		Seq<WeaponMount> mounts = new Seq<>();
		for(int slot = ModulePart.armLeft; slot <= ModulePart.armRight; slot++){
			ModulePart part = part(slot);
			if(part == null || part.kind != ModulePart.Kind.arm) continue;
			Weapon weapon = slot == ModulePart.armLeft ? part.left : part.right;
			WeaponMount keep = null;
			for(WeaponMount m : before){
				if(m.weapon == weapon){
					keep = m;
					break;
				}
			}
			mounts.add(keep != null ? keep : new WeaponMount(weapon));
		}
		unit.mounts = mounts.toArray(WeaponMount.class);

		//The core: the unit's own abilities, and after them the part's.
		ModulePart core = part(ModulePart.core);
		int coreId = core == null || core.ability == null ? -1 : core.id;
		int base = Math.min(unit.abilities.length, unit.type.abilities.size);
		if(coreId != builtCore){
			coreAbility = coreId < 0 ? null : core.ability.get();
			builtCore = coreId;
		}
		Ability[] abilities = Arrays.copyOf(unit.abilities, base + (coreAbility == null ? 0 : 1));
		if(coreAbility != null) abilities[base] = coreAbility;
		unit.abilities = abilities;

		//The hull: only its own effect stays on.
		ModulePart hull = part(ModulePart.hull);
		for(ModulePart p : ModulePart.all){
			if(p.kind == ModulePart.Kind.hull && p.effect != null && p != hull) unit.unapply(p.effect);
		}
		if(hull != null && hull.effect != null) unit.apply(hull.effect, 1f);
	}

	//---------------------------------------------------------------- persistence

	public String serialize(){
		StringBuilder s = new StringBuilder("v1;");
		for(int i = 0; i < installed.length; i++){
			if(i > 0) s.append(',');
			s.append(installed[i]);
		}
		return s.toString();
	}

	/** Reads the slots from text; anything that is not a part for its slot is an empty slot. Never throws. */
	public void parse(String text){
		Arrays.fill(installed, -1);
		if(text == null || !text.startsWith("v1;")) return;
		String[] p = text.substring(3).split(",");
		for(int i = 0; i < installed.length && i < p.length; i++){
			try{
				ModulePart part = ModulePart.get(Integer.parseInt(p[i].trim()));
				if(part != null && part.fits(i)) installed[i] = part.id;
			}catch(RuntimeException ignored){
			}
		}
	}

	public void save(Unit unit){
		if(Vars.net != null && Vars.net.client()) return;
		if(Vars.state != null) Vars.state.rules.tags.put(TAG + unit.id, serialize());
	}

	/** Called once per unit: picks up the loadout the world remembers for it, if any, and fits the unit out. */
	public void restore(Unit unit){
		restored = true;
		if(Vars.state != null) parse(Vars.state.rules.tags.get(TAG + unit.id));
		apply(unit);
	}

	@Override
	public void death(Unit unit){
		if(Vars.state != null) Vars.state.rules.tags.remove(TAG + unit.id);
	}

	//---------------------------------------------------------------- looks

	/** Where each slot's socket plate sits, as (forward, right) in shares of the unit's size. */
	protected static final float[][] sockets = {{0.18f, -0.46f}, {0.18f, 0.46f}, {-0.3f, 0f}, {0.34f, 0f}};

	@Override
	public void draw(Unit unit){
		Draw.z(Layer.groundUnit + 0.2f);
		for(int slot = 0; slot < ModulePart.slots; slot++){
			ModulePart part = part(slot);
			float f = sockets[slot][0] * unit.hitSize, r = sockets[slot][1] * unit.hitSize;
			float px = unit.x + Angles.trnsx(unit.rotation, f) + Angles.trnsx(unit.rotation - 90f, r);
			float py = unit.y + Angles.trnsy(unit.rotation, f) + Angles.trnsy(unit.rotation - 90f, r);
			Draw.color(Color.black, 0.5f);
			Fill.square(px, py, 3.2f, unit.rotation + 45f);
			Draw.color(part == null ? Color.darkGray : part.color);
			Fill.square(px, py, 2.4f, unit.rotation + 45f);
		}
		Draw.reset();
	}

	@Override
	public String localized(){
		return Core.bundle.get("ability.nh-modular", "Modular");
	}
}
