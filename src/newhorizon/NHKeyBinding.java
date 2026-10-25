package newhorizon;

import arc.input.KeyBind;
import arc.input.KeyCode;

/** Key bindings of the mod. Registered through {@link KeyBind#add}, which is all the game needs to list and save them. */
public class NHKeyBinding{
	public static KeyBind teleport, unitInfo;
	
	public static void load(){
		if(teleport != null) return;
		teleport = KeyBind.add("teleport", new KeyBind.Axis(KeyCode.j), "new-horizon");
		unitInfo = KeyBind.add("unit_info", new KeyBind.Axis(KeyCode.i), "new-horizon");
	}
}
