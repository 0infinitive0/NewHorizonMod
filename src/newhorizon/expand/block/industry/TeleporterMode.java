package newhorizon.expand.block.industry;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.math.Angles;
import arc.math.geom.Point2;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.ui.Styles;

/**
 * What the item and liquid teleporters share about being an input or an output: the two buttons in the config panel, the config value
 * that carries channel and mode together (a {@link Point2}, which the network and schematics already carry), and the marks round the
 * block that show the mode.
 */
public final class TeleporterMode{
	public static final Color inputColor = Color.valueOf("ffbe5a"), outputColor = Color.valueOf("42e0c8");

	private TeleporterMode(){
	}

	/** The config for a channel and a mode. */
	public static Point2 pack(int channel, boolean output){
		return new Point2(channel, output ? 1 : 0);
	}

	/** Adds the Input / Output buttons under the channel buttons. {@code current} reads the mode and the channel, {@code set} configures. */
	public static void buttons(Table table, java.util.function.BooleanSupplier isOutput, java.util.function.IntSupplier channel, Building build){
		table.row();
		table.table(Styles.black6, t -> {
			t.margin(4f);
			for(int i = 0; i < 2; i++){
				boolean out = i == 1;
				TextButton b = new TextButton(Core.bundle.get(out ? "config.nh-teleporter-output" : "config.nh-teleporter-input", out ? "Output" : "Input"), Styles.togglet);
				b.clicked(() -> build.configure(pack(channel.getAsInt(), out)));
				b.update(() -> b.setChecked(isOutput.getAsBoolean() == out));
				t.add(b).size(104f, 32f).pad(1f);
			}
		});
	}

	/** Four arrows round the edge: pointing in and amber for an input, pointing out and teal for an output. */
	public static void marks(Building b, boolean output){
		float edge = b.block.size * 4f - 2.2f;
		Draw.z(Layer.block + 0.4f);
		Draw.color(output ? outputColor : inputColor);
		for(int i = 0; i < 4; i++){
			float ang = i * 90f;
			Drawf.tri(b.x + Angles.trnsx(ang, edge), b.y + Angles.trnsy(ang, edge), 3.6f, 4.6f, ang + (output ? 0f : 180f));
		}
		Draw.reset();
	}

	public static String describe(boolean output){
		return Strings.capitalize(output ? "output" : "input");
	}
}
