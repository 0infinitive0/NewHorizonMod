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

		/** The flash, ring, flame jet, sparks and smoke cloud of a nuke leaving its launcher (the rotation is the heading). */
		launch = new Effect(80f, 500f, e -> {
			Draw.z(Layer.effect + 1f);
			Draw.color(Color.white, Color.orange, e.fin());
			Fill.circle(e.x, e.y, 14f * e.fout(Interp.pow3Out));
			Lines.stroke(3.2f * e.fout());
			Lines.circle(e.x, e.y, 10f + 80f * e.finpow());
			Lines.stroke(1.6f * e.fout());
			Lines.circle(e.x, e.y, 6f + 46f * e.finpow());
			//The flame jet along the heading.
			Angles.randLenVectors(e.id, 20, 12f + 120f * e.finpow(), e.rotation, 20f, (x, y) -> {
				Draw.color(Color.white, Color.orange, Color.valueOf("5c3a2a"), e.fin());
				Fill.circle(e.x + x, e.y + y, 3.8f * e.fout());
			});
			//Sparks.
			Draw.color(Color.valueOf("ffe9a8"));
			Lines.stroke(1.5f * e.fout());
			Angles.randLenVectors(e.id + 3, 12, 20f + 100f * e.finpow(), e.rotation, 45f, (x, y) -> Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 3f + 5f * e.fout()));
			//The smoke cloud, drifting back and slowly thinning out.
			Angles.randLenVectors(e.id + 7, 14, 6f + 64f * e.finpow(), (x, y) -> {
				Draw.color(Color.gray, 0.5f * e.fout(Interp.pow2Out));
				Fill.circle(e.x + x, e.y + y, 5f + 9f * e.fin());
			});
			Draw.reset();
		}),

		/** Smoke a flying nuke leaves behind: puffs that swell and fade slowly. */
		trail = new Effect(130f, e -> {
			Draw.z(Layer.effect - 1f);
			Draw.color(Color.lightGray, Color.darkGray, e.fin());
			Draw.alpha(0.55f * e.fout(Interp.pow2Out));
			Angles.randLenVectors(e.id, 2, 3f + 12f * e.finpow(), (x, y) -> Fill.circle(e.x + x, e.y + y, 1.8f + 4.8f * e.fin()));
			Draw.reset();
		}),

		/** The hot flame at the tail of a flying nuke: short and bright, in the colour of its warhead. */
		flame = new Effect(14f, e -> {
			Draw.z(Layer.effect);
			Draw.color(Color.white, e.color, Color.orange, e.fin());
			Fill.circle(e.x, e.y, 3f * e.fout());
			Draw.color(e.color, 0.5f * e.fout());
			Fill.circle(e.x, e.y, 5.5f * e.fout());
			Draw.reset();
		}),

		/** What a big blast leaves: a cloud of smoke spreading out and thinning, and embers drifting off. The rotation is the radius. */
		aftermath = new Effect(180f, 1800f, e -> {
			float r = e.rotation;
			Draw.z(Layer.effect + 1f);
			Angles.randLenVectors(e.id, (int)Mathf.clamp(r / 8f, 8f, 34f), r * 0.12f, r * (0.5f + 0.5f * e.finpow()), (x, y) -> {
				Draw.color(Color.darkGray, Color.gray, e.fin());
				Draw.alpha(0.5f * e.fout(Interp.pow2Out));
				Fill.circle(e.x + x, e.y + y, r * 0.085f * (0.7f + e.fin()));
			});
			Draw.color(Color.white, e.color, e.fin());
			Angles.randLenVectors(e.id + 5, 26, r * 0.25f, r * 0.95f * e.finpow(), (x, y) -> Fill.circle(e.x + x, e.y + y, 1.8f * e.fout()));
			Draw.reset();
		}),

		/** A green speck drifting up out of radioactive fallout. */
		fallout = new Effect(80f, e -> {
			Draw.z(Layer.effect);
			Draw.color(e.color, Color.white, e.fout() * 0.4f);
			Draw.alpha(Mathf.clamp(e.fout() * 1.5f) * 0.9f);
			Fill.circle(e.x + Mathf.sinDeg(e.id * 37f + e.time * 4f) * 2.5f, e.y + 14f * e.finpow(), 0.6f + 1.6f * e.fout());
			Draw.reset();
		}),

		/** An ember flying up off burning ground, with a wisp of smoke after it. */
		ember = new Effect(60f, e -> {
			Draw.z(Layer.effect);
			Draw.color(Color.white, Color.orange, Color.valueOf("661f12"), e.fin());
			Fill.circle(e.x + Mathf.sinDeg(e.id * 53f + e.time * 6f) * 2f, e.y + 18f * e.finpow(), 0.5f + 1.5f * e.fout());
			Draw.color(Color.gray, 0.3f * e.fin() * e.fout());
			Fill.circle(e.x, e.y + 24f * e.finpow(), 2f + 3f * e.fin());
			Draw.reset();
		}),

		/** A ring of force going out, for a pulse of a shockwave nuke. The rotation is the radius. */
		shockRing = new Effect(34f, 1500f, e -> {
			Draw.z(Layer.effect + 2f);
			Draw.color(Color.white, e.color, e.fin());
			Lines.stroke(7f * e.fout());
			Lines.circle(e.x, e.y, e.rotation * e.finpow());
			Draw.color(e.color, 0.35f * e.fout());
			Lines.stroke(16f * e.fout());
			Lines.circle(e.x, e.y, e.rotation * e.finpow() * 0.96f);
			Draw.reset();
		}),

		/** A meteor landing: a flash, a ring, dust and flung rock. The rotation is the size of the blast. */
		meteorImpact = new Effect(46f, 300f, e -> {
			Draw.z(Layer.effect + 1f);
			Draw.color(Color.white, e.color, e.fin() * 2f);
			Fill.circle(e.x, e.y, e.rotation * 0.7f * e.fout() * e.fout());
			Draw.color(e.color, 0.9f * e.fout());
			Lines.stroke(3.2f * e.fout());
			Lines.circle(e.x, e.y, e.rotation * (0.35f + 1.05f * e.finpow()));
			Draw.color(Color.valueOf("5a4636"), e.fout());
			arc.math.Angles.randLenVectors(e.id, 9, e.rotation * 1.5f * e.finpow(), (x, y) -> Fill.circle(e.x + x, e.y + y, 1.2f + 2.6f * e.fout()));
			Draw.color(Color.gray, 0.45f * e.fout());
			arc.math.Angles.randLenVectors(e.id + 1, 7, e.rotation * 0.9f * e.finpow(), (x, y) -> Fill.circle(e.x + x, e.y + y, 3f + 7f * e.fin()));
			Draw.reset();
		}),

		/** Sparks when a nuke is armed. */
		armed = new Effect(24f, 80f, e -> {
			Draw.color(Color.white, Color.scarlet, e.fin());
			Lines.stroke(1.6f * e.fout());
			Lines.circle(e.x, e.y, 6f + 14f * e.finpow());
		});
}
