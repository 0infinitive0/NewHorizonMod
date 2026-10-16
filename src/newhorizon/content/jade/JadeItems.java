package newhorizon.content.jade;

import mindustry.type.Item;

public class JadeItems{
	public static Item verdantResin, jadeSilk, everbloomCore;

	public static void load(){
		verdantResin = new Item("verdant-resin", JadeColor.resin){{
			cost = 0.8f;
			flammability = 0.7f;
		}};

		jadeSilk = new Item("jade-silk", JadeColor.jade){{
			cost = 1.5f;
			flammability = 0.1f;
		}};

		everbloomCore = new Item("everbloom-core", JadeColor.pollen){{
			cost = 2.5f;
			charge = 0.6f;
			radioactivity = 0.2f;
		}};
	}
}
