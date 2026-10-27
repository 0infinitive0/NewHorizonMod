package newhorizon.expand.units.unitEntity;

import arc.struct.ObjectIntMap;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.gen.UnitWaterMove;
import mindustry.type.Item;
import newhorizon.expand.entities.EntityRegister;

/** A ship that carries several kinds of item at the same time, which the normal single stack of a unit cannot. */
public class MultiCargoEntity extends UnitWaterMove{
	public final ObjectIntMap<Item> hold = new ObjectIntMap<>();
	
	@Override public int classId(){return EntityRegister.getID(MultiCargoEntity.class);}
	
	public int total(){
		int total = 0;
		for(var entry : hold)total += entry.value;
		return total;
	}
	
	protected void writeHold(Writes write){
		write.s(hold.size);
		for(var entry : hold){
			write.s(entry.key.id);
			write.i(entry.value);
		}
	}
	
	protected void readHold(Reads read){
		hold.clear();
		int size = read.s();
		for(int i = 0; i < size; i++){
			Item item = Vars.content.item(read.s());
			int amount = read.i();
			if(item != null && amount > 0)hold.put(item, amount);
		}
	}
	
	@Override
	public void write(Writes write){
		writeHold(write);
		super.write(write);
	}
	
	@Override
	public void read(Reads read){
		readHold(read);
		super.read(read);
	}
	
	@Override
	public void writeSync(Writes write){
		writeHold(write);
		super.writeSync(write);
	}
	
	@Override
	public void readSync(Reads read){
		readHold(read);
		super.readSync(read);
	}
}
