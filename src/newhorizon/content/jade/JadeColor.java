package newhorizon.content.jade;

import arc.graphics.Color;
import arc.graphics.Colors;

/** Palette of the Jade Accord faction. */
public class JadeColor{
	public static final Color
		jade = Color.valueOf("3fbf8f"),
		jadeLight = Color.valueOf("b4f5d6"),
		jadeDark = Color.valueOf("1d6b53"),
		pollen = Color.valueOf("f6d86b"),
		resin = Color.valueOf("d9a53a"),
		infection = Color.valueOf("9bd94a"),
		outline = Color.valueOf("26352f");

	static{
		Colors.put("jade", jade);
		Colors.put("pollen", pollen);
	}
}
