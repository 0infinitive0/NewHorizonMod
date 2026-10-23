package newhorizon.expand.block.omni;

import arc.Core;
import arc.func.Cons;
import arc.math.Mathf;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.graphics.Pal;
import mindustry.logic.LAccess;
import mindustry.type.Item;
import mindustry.ui.Bar;
import mindustry.world.blocks.defense.BaseShield;
import mindustry.world.meta.Env;
import newhorizon.content.nuke.NukeItems;

/**
 * A shield in the manner of Erekir's, paid for in omni-matter. It never breaks and has no cooldown: it absorbs every enemy bullet that
 * comes into its circle for as long as it has shield points, and pushes enemy ground units that touch its edge back out. The points come
 * from the omni-matter it holds, {@link #pointsPerItem} for each; an item is only used up when the points have run out and something is
 * there to absorb, so it costs nothing while nothing is happening. With no points and no items the shield is down until more arrive.
 * It needs no power. While the shield is up the block itself takes no damage; when it is down it takes damage like anything else.
 */
public class OmniShield extends BaseShield{
	/** Shield points one omni-matter gives. */
	public float pointsPerItem = 400f;

	public OmniShield(String name){
		super(name);
		hasPower = false;
		hasItems = true;
		itemCapacity = 40;
		unloadable = false;
		envEnabled = Env.any;
		buildType = OmniShieldBuild::new;
	}

	@Override
	public void setBars(){
		super.setBars();
		addBar("nh-omni-shield", (OmniShieldBuild b) -> new Bar(
			() -> Core.bundle.format("nh.omni.bar.shield", (int)b.points()),
			() -> Pal.accent,
			() -> Mathf.clamp(b.points() / (itemCapacity * pointsPerItem))
		));
	}

	public class OmniShieldBuild extends BaseShieldBuild{
		/** Points left of the item that is being used up. */
		public float pool;

		private final Cons<Bullet> absorber = this::absorb;

		/** All the shield points there are: what is left of the open item and every item stored. */
		public float points(){
			return pool + items.get(NukeItems.omniMatter) * pointsPerItem;
		}

		public boolean up(){
			return pool > 0.01f || items.has(NukeItems.omniMatter);
		}

		/** Takes points for a hit, opening a new item if the last is used up. Returns whether there were any. */
		protected boolean spend(float cost){
			if(pool <= 0.01f){
				if(!items.has(NukeItems.omniMatter)) return false;
				items.remove(NukeItems.omniMatter, 1);
				pool += pointsPerItem;
			}
			pool = Math.max(0f, pool - cost);
			return true;
		}

		protected void absorb(Bullet bullet){
			if(!bullet.isAdded() || bullet.team == team || !bullet.type.absorbable || !bullet.within(this, radius())) return;
			//Only the server counts the points; the others take it that the shield is up if it holds items.
			if(Vars.net.client()){
				if(!up()) return;
			}else if(!spend(Math.max(1f, Math.max(bullet.damage(), bullet.type.splashDamage)))){
				return;
			}
			bullet.absorb();
			Fx.absorb.at(bullet);
			hit = 1f;
		}

		@Override
		public boolean acceptItem(Building source, Item item){
			return item == NukeItems.omniMatter && items.get(item) < itemCapacity;
		}

		@Override
		public float handleDamage(float amount){
			//Untouchable while the shield is up.
			return up() ? 0f : amount;
		}

		@Override
		public void updateTile(){
			boolean up = up();
			broken = !up;
			smoothRadius = Mathf.lerpDelta(smoothRadius, up ? radiusMax() : 0f, 0.05f);
			hit = Math.max(0f, hit - 0.04f);
			if(smoothRadius < 2f) return;

			float r = radius();
			Groups.bullet.intersect(x - r, y - r, r * 2f, r * 2f, absorber);
			Units.nearbyEnemies(team, x - r - 20f, y - r - 20f, (r + 20f) * 2f, (r + 20f) * 2f, unit -> {
				if(unit.dead() || unit.isFlying()) return;
				float d = unit.dst(this), edge = r + unit.hitSize / 2f;
				if(d >= edge) return;
				Tmp.v1.set(unit).sub(this);
				if(Tmp.v1.isZero(0.01f)) return;
				//Held at the edge: the way in is stopped and it is pushed back out.
				unit.vel.setZero();
				Tmp.v1.setLength(Math.min(edge - d, 4f));
				unit.move(Tmp.v1.x, Tmp.v1.y);
			});
		}

		protected float radiusMax(){
			return OmniShield.this.radius;
		}

		@Override
		public double sense(LAccess sensor){
			if(sensor == LAccess.shield) return points();
			return super.sense(sensor);
		}

		@Override
		public void write(Writes write){
			super.write(write);
			write.f(pool);
		}

		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			pool = read.f();
		}
	}
}
