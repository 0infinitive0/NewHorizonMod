package newhorizon.expand.bullets;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Puddles;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.graphics.Layer;
import mindustry.type.Liquid;
import mindustry.world.Tile;
import newhorizon.expand.block.distribution.LiquidMassDriver.LiquidMassDriverBuild;

/**
 * The shot a {@link newhorizon.expand.block.distribution.LiquidMassDriver} fires: a bolt of liquid that flies to the linked driver and
 * is poured into it on arrival. It is the liquid twin of vanilla's mass driver bolt, same flight and arrival maths. Whatever the
 * receiver could not take, and everything of a bolt that missed, is spilled on the ground where it lands.
 */
public class LiquidDriverBolt extends BulletType{
	/** Liquid in the bolt when it is full, used to scale its size. */
	public float fullAmount = 600f;

	public LiquidDriverBolt(){
		super(1f, 0f);
		collidesTiles = false;
		collides = false;
		collidesAir = false;
		scaleLife = true;
		lifetime = 1f;
		ammoMultiplier = 1f;
		despawnEffect = Fx.none;
		hitEffect = Fx.none;
		drawSize = 60f;
	}

	@Override
	public void draw(Bullet b){
		if(!(b.data() instanceof LiquidDriverData)) return;
		LiquidDriverData data = (LiquidDriverData)b.data();

		float fill = Mathf.clamp(data.amount / fullAmount);
		float r = 2.6f + 3.2f * fill;
		Color c = data.liquid.color;

		Draw.z(Layer.bullet);
		Draw.color(c, 0.35f);
		Lines.stroke(r * 0.9f);
		Lines.lineAngle(b.x, b.y, b.rotation() + 180f, 10f + 14f * fill);
		Draw.color(c);
		Fill.circle(b.x, b.y, r);
		Draw.color(Color.white, c, 0.5f);
		Fill.circle(b.x, b.y, r * 0.55f);
		Draw.reset();
	}

	@Override
	public void update(Bullet b){
		//data MUST be a LiquidDriverData
		if(!(b.data() instanceof LiquidDriverData)){
			b.remove();
			return;
		}
		LiquidDriverData data = (LiquidDriverData)b.data();

		float hitDst = 7f;

		//if the target is dead, just keep flying until the bolt splashes
		if(data.to == null || !data.to.isValid()) return;

		float baseDst = data.from.dst(data.to);
		float dst1 = b.dst(data.from);
		float dst2 = b.dst(data.to);

		boolean intersect = false;

		//the bolt has gone past the destination: but did it cross it?
		if(dst1 > baseDst){
			float angleTo = b.angleTo(data.to);
			float baseAngle = data.to.angleTo(data.from);

			//if the angles are close, yes it did
			if(Mathf.within(angleTo, baseAngle, 2f)){
				intersect = true;
				//snap back, for low frame rates
				b.set(data.to.x + Angles.trnsx(baseAngle, hitDst), data.to.y + Angles.trnsy(baseAngle, hitDst));
			}
		}

		//on course and close to the target
		if(Math.abs(dst1 + dst2 - baseDst) < 4f && dst2 <= hitDst){
			intersect = true;
		} //otherwise it is off course and is not received

		if(intersect){
			data.to.handlePayload(b, data);
		}
	}

	@Override
	public void despawned(Bullet b){
		super.despawned(b);

		if(b.data() instanceof LiquidDriverData){
			spill(b.x, b.y, (LiquidDriverData)b.data());
		}
	}

	@Override
	public void hit(Bullet b, float x, float y){
		super.hit(b, x, y);
		despawned(b);
	}

	/** Pours what is left of a bolt on the ground. Server side only, puddles are synced. */
	public static void spill(float x, float y, LiquidDriverData data){
		float left = data.amount;
		data.amount = 0f;
		data.spilled += left;
		if(left < 1f || data.liquid == null || Groups.isClearing || Vars.net.client()) return;

		Tile tile = Vars.world.tileWorld(x, y);
		if(tile != null){
			Puddles.deposit(tile, data.liquid, left);
		}
	}

	/** The cargo of a bolt. */
	public static class LiquidDriverData{
		public LiquidMassDriverBuild from, to;
		public Liquid liquid;
		/** Liquid still in the bolt. */
		public float amount;
		/** Liquid that ended up in the receiver. */
		public float delivered;
		/** Liquid that ended up on the ground. */
		public float spilled;

		public LiquidDriverData(LiquidMassDriverBuild from, LiquidMassDriverBuild to, Liquid liquid, float amount){
			this.from = from;
			this.to = to;
			this.liquid = liquid;
			this.amount = amount;
		}
	}
}
