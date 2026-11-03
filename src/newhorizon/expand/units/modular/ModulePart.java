package newhorizon.expand.units.modular;

import arc.Core;
import arc.func.Prov;
import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.entities.abilities.Ability;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.type.Weapon;

/**
 * One part of the modular frame (the Chimera). A part is carried by a small pod unit: the pod fights on its own, and docked into a frame
 * at a Refit Bay it lends the frame what it carries. There are three kinds, each with its own slot(s):
 * <ul>
 * <li>{@link Kind#arm}: a gun for one of the two arm slots (a left and a right version of the same weapon);</li>
 * <li>{@link Kind#core}: an ability for the core slot;</li>
 * <li>{@link Kind#hull}: a permanent status effect (health, speed, damage, reload) for the hull slot.</li>
 * </ul>
 * The registry is filled when the content loads, before any block that refers to it is built. A part's id is its place in {@link #all}.
 */
public class ModulePart{
	public enum Kind{arm, core, hull}

	/** The slots of a frame: the left and the right arm, the core and the hull. */
	public static final int armLeft = 0, armRight = 1, core = 2, hull = 3, slots = 4;

	public static final Seq<ModulePart> all = new Seq<>();

	public final int id;
	public final String name;
	public final Kind kind;
	public final Color color;

	/** An arm part's gun, as mounted on the left and on the right. */
	public Weapon left, right;
	/** A core part's ability: a new one each time. */
	public Prov<Ability> ability;
	/** A hull part's effect. */
	public StatusEffect effect;
	/** A hull part's share of the frame's armor (1 keeps it, 0 takes it all away, 2 doubles it): armor has no status field, so it is set when the part is fitted. */
	public float armorScale = 1f;
	/** Whether the part lets the frame build (a core part): the frame builds only with it. */
	public boolean builds;
	/** The pod that carries the part. */
	public UnitType pod;

	public ModulePart(String name, Kind kind, Color color){
		this.name = name;
		this.kind = kind;
		this.color = color;
		this.id = all.size;
		all.add(this);
	}

	public static ModulePart get(int id){
		return id >= 0 && id < all.size ? all.get(id) : null;
	}

	/** The part a pod unit carries, or null if the type is not a pod. */
	public static ModulePart ofPod(UnitType type){
		for(ModulePart part : all){
			if(part.pod == type) return part;
		}
		return null;
	}

	/** The kind of part a slot takes. */
	public static Kind kindOf(int slot){
		return slot == core ? Kind.core : slot == hull ? Kind.hull : Kind.arm;
	}

	/** Whether the part goes into the slot. */
	public boolean fits(int slot){
		return slot >= 0 && slot < slots && kindOf(slot) == kind;
	}

	public String localized(){
		return Core.bundle.get("module.nh-" + name, name);
	}
}
