package newhorizon.content.chronos;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Interp;
import arc.math.Mathf;
import mindustry.entities.Effect;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;

/** Visual effects shared by the Chronos content. */
public class ChronosFx{
	public static final Effect
		sparkBurst = sparkBurst(ChronosColor.chronoLight, ChronosColor.chrono, 8, 26f),
		sparkBurstBig = sparkBurst(Color.white, ChronosColor.chrono, 16, 56f),
		goldBurst = sparkBurst(Color.white, ChronosColor.gold, 10, 34f),
		freezeBurst = ring(ChronosColor.chronoLight, 62f),
		freezeBurstBig = ring(ChronosColor.chronoLight, 96f),
		timeRing = ring(ChronosColor.chrono, 38f),
		rewind = ring(ChronosColor.gold, 24f),
		disabled = sparkBurst(Color.white, ChronosColor.violet, 4, 14f),
		teleportOut = ring(ChronosColor.violet, 34f),
		holeCollapse = new Effect(50f, 400f, e -> {
			Draw.z(Layer.effect + 1f);
			Draw.color(ChronosColor.violet, ChronosColor.hole, e.fin());
			Lines.stroke(3f * e.fout());
			Lines.circle(e.x, e.y, e.rotation * e.fout(Interp.pow3Out));
			Draw.color(ChronosColor.hole);
			Fill.circle(e.x, e.y, e.rotation * 0.15f * e.fout());
		});

	public static Effect sparkBurst(Color from, Color to, int count, float spread){
		return new Effect(30f, spread * 2f, e -> {
			Draw.color(from, to, e.fin());
			Angles.randLenVectors(e.id, count, 3f + spread * e.finpow(), (x, y) -> {
				float rot = Mathf.angle(x, y);
				float s = e.fout() * 3f;
				Drawf.tri(e.x + x, e.y + y, s, s * 3.2f, rot);
			});
		});
	}

	public static Effect ring(Color color, float radius){
		return new Effect(38f, radius * 2.5f, e -> {
			Draw.color(color, Color.white, e.fout() * 0.4f);
			Lines.stroke(2.2f * e.fout());
			//A rotation given to the effect overrides its radius.
			Lines.circle(e.x, e.y, (e.rotation > 0f ? e.rotation : radius) * e.finpow());
		});
	}
}
