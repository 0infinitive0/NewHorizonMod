package newhorizon.content.spore;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import mindustry.entities.Effect;
import mindustry.graphics.Drawf;

/** Effects of the Spore Mote: a burst of amber spores and a ring where the blast goes off. */
public class SporeFx{
	public static final Color amber = Color.valueOf("ffb84d"), amberLight = Color.valueOf("ffe9a8"), amberDark = Color.valueOf("c4691f");

	public static final Effect
		moteBurst = burst(14, 44f),
		moteBurstSmall = burst(6, 20f),
		moteRing = new Effect(36f, 120f, e -> {
			float r = 46f * e.finpow();
			Draw.color(amber);
			Draw.alpha(0.2f * e.fout());
			Fill.circle(e.x, e.y, r);
			Draw.alpha(0.85f * e.fout());
			Lines.stroke(2f * e.fout() + 0.4f);
			Lines.circle(e.x, e.y, r);
			Draw.reset();
		});

	public static Effect burst(int count, float spread){
		return new Effect(34f, spread * 2f, e -> {
			Draw.color(amberLight, amber, e.fin());
			Angles.randLenVectors(e.id, count, 3f + spread * e.finpow(), (x, y) -> {
				float rot = Mathf.angle(x, y);
				float s = e.fout() * 3f;
				Fill.circle(e.x + x, e.y + y, s * 0.8f);
				Drawf.tri(e.x + x, e.y + y, s * 0.6f, s * 2.2f, rot);
			});
			Draw.reset();
		});
	}
}
