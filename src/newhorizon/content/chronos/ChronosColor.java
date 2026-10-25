package newhorizon.content.chronos;

import arc.graphics.Color;
import arc.graphics.Colors;

/** Palette of the Chronos faction: cold cyan clockwork over deep indigo, with gold for the moving parts. */
public class ChronosColor{
	public static final Color
		chrono = Color.valueOf("6fd8ff"),
		chronoLight = Color.valueOf("c8f4ff"),
		chronoDark = Color.valueOf("2b4a9e"),
		violet = Color.valueOf("9b7bff"),
		gold = Color.valueOf("ffd36b"),
		hole = Color.valueOf("0b0716"),
		outline = Color.valueOf("1c1d38");

	static{
		Colors.put("chrono", chrono);
	}
}
