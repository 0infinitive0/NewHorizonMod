package newhorizon.content.chronos;

import mindustry.type.Item;

public class ChronosItems{
	/** Made from silicon and graphite, which every planet has: that is what lets Chronos work everywhere. */
	public static Item chronite, paradoxCore;

	public static void load(){
		chronite = new Item("chronite", ChronosColor.chrono){{
			cost = 1.1f;
			charge = 0.3f;
		}};

		paradoxCore = new Item("paradox-core", ChronosColor.gold){{
			cost = 2.4f;
			charge = 0.7f;
			radioactivity = 0.1f;
		}};
	}
}
