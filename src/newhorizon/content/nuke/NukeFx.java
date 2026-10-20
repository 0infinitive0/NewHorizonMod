package newhorizon.content.nuke;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Interp;
import arc.math.Mathf;
import mindustry.entities.Effect;
import mindustry.graphics.Layer;

/** Effects of the nukes. The blast effects take their radius as the effect's rotation and their colour as the effect's colour. */
public class NukeFx{
	public static final Effect
		/** A fireball and shockwave of the given radius. */
		blast = new Effect(80f, 1200f, e -> {
			float r = e.rotation;
			Draw.z(Layer.effect + 2f);
			Draw.color(Color.white, e.color, e.fin(Interp.pow2Out));
			Fill.circle(e.x, e.y, r * 0.3f * e.fout(Interp.pow3Out));
			Draw.color(e.color, 0.55f * e.fout());
			Fill.circle(e.x, e.y, r * 0.8f * e.finpow());
			Draw.color(Color.white, e.color, e.fin());
			Lines.stroke(Math.max(1f, r * 0.05f) * e.fout());
			Lines.circle(e.x, e.y, r * e.finpow());
			Lines.stroke(Math.max(1f, r * 0.025f) * e.fout());
			Lines.circle(e.x, e.y, r * 0.6f * e.finpow());
			Draw.color(e.color);
			Angles.randLenVectors(e.id, (int)Mathf.clamp(r / 6f, 6f, 40f), r * 0.2f, r * e.finpow(), (x, y) -> {
				Lines.stroke(2f * e.fout());
				Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 4f + r * 0.08f * e.fout());
			});
			Draw.reset();
		}),

		/** Rings and crackling arcs of an electromagnetic pulse. */
		empPulse = new Effect(70f, 1200f, e -> {
			float r = e.rotation;
			Draw.z(Layer.effect + 2f);
			Draw.color(Color.white, e.color, e.fin());
			for(int i = 0; i < 3; i++){
				float f = Mathf.clamp(e.fin() * 1.4f - i * 0.15f);
				Lines.stroke(Math.max(1f, r * 0.03f) * (1f - f));
				Lines.circle(e.x, e.y, r * f);
			}
			Draw.color(e.color);
			Angles.randLenVectors(e.id, 26, r * 0.1f, r * e.finpow(), (x, y) -> {
				Lines.stroke(1.6f * e.fout());
				float ang = Mathf.angle(x, y) + Mathf.randomSeedRange((int)(e.id + x), 25f);
				Lines.lineAngle(e.x + x, e.y + y, ang, 6f + r * 0.06f * e.fout());
			});
			Draw.reset();
		}),

		/** Rings drawn inwards, for a pull. */
		pullRing = new Effect(60f, 1200f, e -> {
			float r = e.rotation;
			Draw.z(Layer.effect + 2f);
			Draw.color(Color.white, e.color, e.fin());
			for(int i = 0; i < 3; i++){
				float f = Mathf.clamp(e.fin() * 1.3f - i * 0.1f);
				Lines.stroke(Math.max(1f, r * 0.03f) * (1f - f * 0.6f));
				Lines.circle(e.x, e.y, r * (1f - f));
			}
			Draw.reset();
		}),

		/** The cut of a breach nuke: a bright line behind the point where it went off, in the direction it came from (the rotation is the heading). */
		breachBeam = new Effect(45f, 1400f, e -> {
			Draw.z(Layer.effect + 2f);
			float len = 700f;
			Draw.color(Color.white, e.color, e.fin());
			Lines.stroke(14f * e.fout());
			Lines.lineAngle(e.x, e.y, e.rotation + 180f, len * Interp.pow2Out.apply(Math.min(1f, e.fin() * 6f)));
			Draw.color(e.color, 0.6f * e.fout());
			Lines.stroke(30f * e.fout());
			Lines.lineAngle(e.x, e.y, e.rotation + 180f, len * Interp.pow2Out.apply(Math.min(1f, e.fin() * 6f)));
			Draw.reset();
		}),

		/** The flash of a nuke leaving its launcher. */
		launch = new Effect(40f, 300f, e -> {
			Draw.color(Color.white, Color.orange, e.fin());
			Lines.stroke(3f * e.fout());
			Lines.circle(e.x, e.y, 10f + 60f * e.finpow());
			Angles.randLenVectors(e.id, 14, 20f + 70f * e.finpow(), e.rotation, 28f, (x, y) -> Fill.circle(e.x + x, e.y + y, 3.5f * e.fout()));
			Draw.reset();
		}),

		/** Sparks when a nuke is armed. */
		armed = new Effect(24f, 80f, e -> {
			Draw.color(Color.white, Color.scarlet, e.fin());
			Lines.stroke(1.6f * e.fout());
			Lines.circle(e.x, e.y, 6f + 14f * e.finpow());
		});
}
