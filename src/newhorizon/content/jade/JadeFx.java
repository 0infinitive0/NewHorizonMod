package newhorizon.content.jade;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import mindustry.entities.Effect;
import mindustry.graphics.Drawf;

/** Visual effects shared by the Jade Accord content. */
public class JadeFx{
	public static final Effect
		leafBurst = leafBurst(JadeColor.jadeLight, JadeColor.jade, 6, 22f),
		leafBurstBig = leafBurst(JadeColor.jadeLight, JadeColor.jade, 14, 48f),
		pollenBurst = leafBurst(Color.white, JadeColor.pollen, 10, 34f),
		bloomRing = bloomRing(JadeColor.jade, 42f),
		bloomRingBig = bloomRing(JadeColor.jade, 80f),
		thornShoot = shoot(JadeColor.jade),
		pollenShoot = shoot(JadeColor.pollen),
		sporeTrail = new Effect(26f, e -> {
			Draw.color(JadeColor.jadeLight, JadeColor.jade, e.fin());
			Fill.circle(e.x, e.y, 1.8f * e.fout());
		});

	public static Effect leafBurst(Color from, Color to, int count, float spread){
		return new Effect(34f, spread * 2f, e -> {
			Draw.color(from, to, e.fin());
			Angles.randLenVectors(e.id, count, 3f + spread * e.finpow(), (x, y) -> {
				float rot = Mathf.angle(x, y);
				float s = e.fout() * 3f;
				Drawf.tri(e.x + x, e.y + y, s, s * 2.6f, rot);
				Drawf.tri(e.x + x, e.y + y, s, s * 1.2f, rot + 180f);
			});
		});
	}

	public static Effect bloomRing(Color color, float radius){
		return new Effect(40f, radius * 2.5f, e -> {
			float r = radius * e.finpow();
			Draw.color(color);
			Draw.alpha(0.2f * e.fout());
			Fill.circle(e.x, e.y, r);
			Draw.alpha(0.8f * e.fout());
			Lines.stroke(2f * e.fout() + 0.4f);
			Lines.circle(e.x, e.y, r);
		});
	}

	public static Effect shoot(Color color){
		return new Effect(18f, e -> {
			Draw.color(Color.white, color, e.fin());
			Drawf.tri(e.x, e.y, 5f * e.fout(), 18f * e.fout(), e.rotation);
			Drawf.tri(e.x, e.y, 3f * e.fout(), 8f * e.fout(), e.rotation + 180f);
		});
	}
}
