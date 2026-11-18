package newhorizon.expand.packets;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Unit;
import mindustry.net.NetConnection;
import mindustry.net.Packet;
import newhorizon.expand.units.ablility.DroneBayAbility;

/** A player piloting a Mothership asks its bay for another drone type (the pilot's key). The server checks that the sender really flies one. */
public class DroneTypePacket extends Packet{
	private byte[] DATA = NODATA;
	public int index;

	public DroneTypePacket(){}

	public DroneTypePacket(int index){
		this.index = index;
	}

	@Override
	public void write(Writes WRITE){
		WRITE.i(index);
	}

	@Override
	public void read(Reads READ, int LENGTH){
		DATA = READ.b(LENGTH);
	}

	@Override
	public void handled(){
		BAIS.setBytes(DATA);
		index = READ.i();
	}

	@Override
	public void handleServer(NetConnection con){
		if(con == null || con.player == null) return;
		apply(con.player.unit(), index);
	}

	/** Gives the unit's bay the new type, if it has one. */
	public static boolean apply(Unit unit, int index){
		DroneBayAbility bay = DroneBayAbility.of(unit);
		return bay != null && bay.select(index);
	}
}
