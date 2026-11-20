package newhorizon.expand.packets;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Unit;
import mindustry.net.NetConnection;
import mindustry.net.Packet;
import newhorizon.expand.units.ablility.DroneBayAbility;

/** A player piloting a Mothership turns Stand down on or off (the pilot's key). The server checks that the sender really flies one. */
public class DroneStandDownPacket extends Packet{
	private byte[] DATA = NODATA;
	public boolean on;

	public DroneStandDownPacket(){}

	public DroneStandDownPacket(boolean on){
		this.on = on;
	}

	@Override
	public void write(Writes WRITE){
		WRITE.bool(on);
	}

	@Override
	public void read(Reads READ, int LENGTH){
		DATA = READ.b(LENGTH);
	}

	@Override
	public void handled(){
		BAIS.setBytes(DATA);
		on = READ.bool();
	}

	@Override
	public void handleServer(NetConnection con){
		if(con == null || con.player == null) return;
		apply(con.player.unit(), on);
	}

	/** Sets Stand down on the unit's bay, if it has one. */
	public static boolean apply(Unit unit, boolean on){
		DroneBayAbility bay = DroneBayAbility.of(unit);
		if(bay == null) return false;
		bay.setStandDown(on);
		return true;
	}
}
