package newhorizon.expand.units.scavenger;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.entities.abilities.*;

import java.lang.reflect.Field;

/**
 * The tunable stats of the abilities a Scavenger can steal, so that stealing one it already has can make it better.
 *
 * <p>An ability is any class, so which fields are its "power" has to be listed. For each there is the direction that is better
 * (a lower reload or cooldown is better, a higher range or amount is better) and a limit where one makes sense. Abilities
 * that are not listed have nothing to improve, and a repeat steal of one of them does nothing.
 *
 * <p>Like weapon damage, an upgrade is additive from the ability's <i>base</i> value: after n upgrades of a stat it is
 * {@code base * (1 + step * n)}, or {@code base / (1 + step * n)} for the "lower is better" ones.
 */
public final class AbilityStats{
	private AbilityStats(){}

	public static final class Stat{
		public final Class<? extends Ability> type;
		public final String field;
		public final boolean lowerIsBetter, integer;
		/** The value never goes below (lower is better) or above (higher is better) this. NaN for no limit. */
		public final float limit;

		Stat(Class<? extends Ability> type, String field, boolean lowerIsBetter, boolean integer, float limit){
			this.type = type;
			this.field = field;
			this.lowerIsBetter = lowerIsBetter;
			this.integer = integer;
			this.limit = limit;
		}

		/** The value after {@code n} upgrades counted from {@code base}. */
		public float value(float base, int n, float step){
			if(integer){
				return base + n * Math.max(1f, Math.round(base * step));
			}
			float k = 1f + step * n;
			float v = lowerIsBetter ? base / k : base * k;
			if(!Float.isNaN(limit)) v = lowerIsBetter ? Math.max(limit, v) : Math.min(limit, v);
			return v;
		}
	}

	private static final Seq<Stat> all = new Seq<>();
	private static final ObjectMap<String, Field> fields = new ObjectMap<>();

	private static void up(Class<? extends Ability> type, String field){
		all.add(new Stat(type, field, false, false, Float.NaN));
	}

	private static void down(Class<? extends Ability> type, String field, float floor){
		all.add(new Stat(type, field, true, false, floor));
	}

	private static void count(Class<? extends Ability> type, String field){
		all.add(new Stat(type, field, false, true, Float.NaN));
	}

	static{
		up(RepairFieldAbility.class, "amount");
		down(RepairFieldAbility.class, "reload", 5f);
		up(RepairFieldAbility.class, "range");

		up(StatusFieldAbility.class, "duration");
		down(StatusFieldAbility.class, "reload", 5f);
		up(StatusFieldAbility.class, "range");

		up(EnergyFieldAbility.class, "damage");
		down(EnergyFieldAbility.class, "reload", 5f);
		up(EnergyFieldAbility.class, "range");
		up(EnergyFieldAbility.class, "healPercent");
		count(EnergyFieldAbility.class, "maxTargets");

		up(ShieldRegenFieldAbility.class, "amount");
		up(ShieldRegenFieldAbility.class, "max");
		down(ShieldRegenFieldAbility.class, "reload", 5f);
		up(ShieldRegenFieldAbility.class, "range");

		up(ForceFieldAbility.class, "radius");
		up(ForceFieldAbility.class, "regen");
		up(ForceFieldAbility.class, "max");
		down(ForceFieldAbility.class, "cooldown", 10f);

		up(ShieldArcAbility.class, "radius");
		up(ShieldArcAbility.class, "regen");
		up(ShieldArcAbility.class, "max");
		down(ShieldArcAbility.class, "cooldown", 10f);

		up(ArmorPlateAbility.class, "healthMultiplier");

		down(SuppressionFieldAbility.class, "reload", 5f);
		up(SuppressionFieldAbility.class, "range");

		down(UnitSpawnAbility.class, "spawnTime", 60f);

		up(LiquidRegenAbility.class, "slurpSpeed");
		up(LiquidRegenAbility.class, "regenPerSlurp");

		up(RegenAbility.class, "percentAmount");
		up(RegenAbility.class, "amount");

		up(MoveLightningAbility.class, "damage");
		all.add(new Stat(MoveLightningAbility.class, "chance", false, false, 1f));
		count(MoveLightningAbility.class, "length");

		count(SpawnDeathAbility.class, "amount");
	}

	/** The stats of this ability that can be upgraded. Empty for abilities that have none. */
	public static Seq<Stat> of(Ability ability){
		Seq<Stat> out = new Seq<>();
		for(Stat s : all){
			if(s.type.isInstance(ability)) out.add(s);
		}
		return out;
	}

	public static Stat find(Ability ability, String field){
		for(Stat s : all){
			if(s.type.isInstance(ability) && s.field.equals(field)) return s;
		}
		return null;
	}

	public static float get(Ability ability, String field){
		try{
			Field f = field(ability, field);
			return f.getType() == int.class ? f.getInt(ability) : f.getFloat(ability);
		}catch(ReflectiveOperationException e){
			throw new IllegalStateException(e);
		}
	}

	public static void set(Ability ability, String field, float value){
		try{
			Field f = field(ability, field);
			if(f.getType() == int.class) f.setInt(ability, Math.round(value));
			else f.setFloat(ability, value);
		}catch(ReflectiveOperationException e){
			throw new IllegalStateException(e);
		}
	}

	private static Field field(Ability ability, String name) throws NoSuchFieldException{
		String key = ability.getClass().getName() + "#" + name;
		Field f = fields.get(key);
		if(f == null){
			f = ability.getClass().getField(name);
			fields.put(key, f);
		}
		return f;
	}
}
