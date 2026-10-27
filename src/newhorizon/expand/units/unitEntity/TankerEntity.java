package newhorizon.expand.units.unitEntity;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.gen.UnitWaterMove;
import mindustry.type.Liquid;
import newhorizon.expand.entities.EntityRegister;

/** A ship that carries one kind of liquid. */
public class TankerEntity extends UnitWaterMove{
	public Liquid liquid;
	public float liquidAmount;
	
	@Override public int classId(){return EntityRegister.getID(TankerEntity.class);}
	
	protected void writeLiquid(Writes write){
		write.s(liquid == null || liquidAmount <= 0f ? -1 : liquid.id);
		write.f(liquidAmount);
	}
	
	protected void readLiquid(Reads read){
		int id = read.s();
		liquidAmount = read.f();
		liquid = id == -1 ? null : Vars.content.liquid(id);
		if(liquid == null)liquidAmount = 0f;
	}
	
	@Override
	public void write(Writes write){
		writeLiquid(write);
		super.write(write);
	}
	
	@Override
	public void read(Reads read){
		readLiquid(read);
		super.read(read);
	}
	
	@Override
	public void writeSync(Writes write){
		writeLiquid(write);
		super.writeSync(write);
	}
	
	@Override
	public void readSync(Reads read){
		readLiquid(read);
		super.readSync(read);
	}
}
