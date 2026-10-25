package newhorizon.expand.block.omni;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Damage;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.gen.Bullet;
import mindustry.gen.Sounds;
import mindustry.graphics.Layer;
import mindustry.ui.Bar;
import mindustry.world.blocks.defense.Wall;
import newhorizon.content.nuke.NukeFx;

import static mindustry.Vars.tilesize;

/**
 * A wall with a great deal of health that keeps the damage it takes. The wall loses health as usual, but a copy of every point of damage
 * is stored as charge, and when charge above {@link #threshold} has built up and an enemy comes within {@link #triggerRange} it is let out
 * as a blast around the wall that hurts everything of the enemy's within {@link #blastRadius}, for as much as was stored. Lasers stop at
 * it, and so do bullets that would pierce through buildings.
 */
public class OmniWall extends Wall{
	/** The most charge it keeps, the least it needs to blast, how close an enemy has to come, and the size of the blast. */
	public float maxCharge = 3000f, threshold = 100f, triggerRange = 4f * tilesize, blastRadius = 5f * tilesize;
	/** Ticks before it can blast again. */
	public float blastCooldown = 20f;
	public Color color = Color.valueOf("c885ff");

	public OmniWall(String name){
		super(name);
		update = true;
		absorbLasers = true;
		insulated = true;
		buildType = OmniWallBuild::new;
	}

	@Override
	public void setBars(){
		super.setBars();
		addBar("nh-omni-charge", (OmniWallBuild b) -> new Bar(
			() -> Core.bundle.format("nh.omni.bar.charge", (int)b.charge),
			() -> color,
			() -> Mathf.clamp(b.charge / maxCharge)
		));
	}

	public class OmniWallBuild extends WallBuild{
		/** Damage taken and kept, waiting to be let out. */
		public float charge;
		public float cooldown;

		@Override
		public boolean collision(Bullet bullet){
			boolean result = super.collision(bullet);
			//Nothing goes on through it that was meant to pierce buildings.
			if(bullet.type.pierceBuilding) bullet.remove();
			return result;
		}

		@Override
		public float handleDamage(float amount){
			charge = Math.min(maxCharge, charge + amount);
			return amount;
		}

		@Override
		public void updateTile(){
			cooldown = Math.max(0f, cooldown - Time.delta);
			if(charge <= threshold || cooldown > 0f || !timer(0, 6f) || Vars.net.client()) return;

			boolean[] near = {false};
			float r = triggerRange;
			Units.nearbyEnemies(team, x - r - 12f, y - r - 12f, (r + 12f) * 2f, (r + 12f) * 2f, unit -> {
				if(!unit.dead() && unit.within(this, r + unit.hitSize / 2f)) near[0] = true;
			});
			if(near[0]) release();
		}

		/** Lets out what was stored, in a blast around the wall. */
		public void release(){
			float damage = charge;
			charge = 0f;
			cooldown = blastCooldown;
			Damage.damage(team, x, y, blastRadius, damage, false, true, true);
			NukeFx.shockRing.at(x, y, blastRadius, color);
			Effect.shake(Math.min(6f, damage / 300f), 12f, x, y);
			Sounds.explosion.at(x, y, 0.8f, 1.4f);
			Fx.hitLaserBlast.at(x, y, 0f, color);
		}

		@Override
		public void draw(){
			super.draw();
			if(charge > 1f){
				Draw.z(Layer.blockOver);
				Draw.color(color, Mathf.clamp(charge / 1500f) * (0.4f + 0.15f * Mathf.absin(Time.time, 10f, 1f)));
				Fill.square(x, y, tilesize / 2f - 1f);
				Draw.color();
			}
		}

		@Override
		public void write(Writes write){
			super.write(write);
			write.f(charge);
		}

		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			charge = read.f();
		}
	}
}
