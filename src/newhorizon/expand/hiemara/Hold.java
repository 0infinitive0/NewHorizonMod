package newhorizon.expand.hiemara;

import arc.struct.Seq;
import mindustry.gen.Unit;
import mindustry.type.Item;
import newhorizon.expand.units.hiemara.CargoUnitType;
import newhorizon.expand.units.unitEntity.MultiCargoEntity;

/** What a cargo unit carries. Most hold a single stack, the big ships hold several kinds at once. */
public class Hold{
	public static boolean multi(Unit unit){
		return unit instanceof MultiCargoEntity;
	}
	
	public static int total(Unit unit){
		return unit instanceof MultiCargoEntity ? ((MultiCargoEntity)unit).total() : unit.stack.amount;
	}
	
	public static boolean empty(Unit unit){
		return total(unit) <= 0;
	}
	
	public static int get(Unit unit, Item item){
		if(unit instanceof MultiCargoEntity)return ((MultiCargoEntity)unit).hold.get(item, 0);
		return unit.stack.item == item ? unit.stack.amount : 0;
	}
	
	public static Seq<Item> items(Unit unit){
		Seq<Item> items = new Seq<>();
		if(unit instanceof MultiCargoEntity){
			for(var entry : ((MultiCargoEntity)unit).hold)items.add(entry.key);
		}else if(unit.stack.amount > 0 && unit.stack.item != null){
			items.add(unit.stack.item);
		}
		return items;
	}
	
	/** How much more of the item the unit can take. */
	public static int room(Unit unit, Item item){
		if(unit instanceof MultiCargoEntity){
			MultiCargoEntity ship = (MultiCargoEntity)unit;
			int types = ((CargoUnitType)ship.type).holdTypes;
			boolean has = ship.hold.get(item, 0) > 0;
			if(!has && ship.hold.size >= types)return 0;
			return Math.max(0, ship.type.itemCapacity - ship.hold.get(item, 0));
		}
		if(unit.stack.amount <= 0)return unit.type.itemCapacity;
		return unit.stack.item == item ? Math.max(0, unit.type.itemCapacity - unit.stack.amount) : 0;
	}
	
	public static void add(Unit unit, Item item, int amount){
		if(amount <= 0)return;
		if(unit instanceof MultiCargoEntity){
			((MultiCargoEntity)unit).hold.increment(item, 0, amount);
		}else{
			unit.addItem(item, amount);
		}
	}
	
	public static void remove(Unit unit, Item item, int amount){
		if(amount <= 0)return;
		if(unit instanceof MultiCargoEntity){
			MultiCargoEntity ship = (MultiCargoEntity)unit;
			int left = ship.hold.get(item, 0) - amount;
			if(left <= 0)ship.hold.remove(item, 0);else ship.hold.put(item, left);
		}else if(unit.stack.item == item){
			unit.stack.amount -= amount;
			if(unit.stack.amount <= 0)unit.clearItem();
		}
	}
	
	public static void clear(Unit unit){
		if(unit instanceof MultiCargoEntity)((MultiCargoEntity)unit).hold.clear();
		else unit.clearItem();
	}
}
