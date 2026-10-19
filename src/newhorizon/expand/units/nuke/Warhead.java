package newhorizon.expand.units.nuke;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.content.StatusEffects;
import mindustry.entities.Damage;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.world.Tile;
import mindustry.Vars;
import mindustry.entities.Fires;
import newhorizon.content.nuke.NukeBullets;
import newhorizon.content.nuke.NukeFx;
import newhorizon.content.nuke.NukeStatus;
import newhorizon.expand.block.chronos.TemporalDisable;
import newhorizon.expand.bullets.nuke.ZoneBulletType;

import static mindustry.Vars.tilesize;

/**
 * One kind of nuke. An inactive nuke of any kind makes the same plain blast; an active one makes a huge blast of its kind's size and
 * then its {@link #special}: the extra that sets the kinds apart. Every blast hurts everyone, the nuke's own side included.
 */
public abstract class Warhead{
	/** The blast of an inactive nuke: the same for every kind, with no special effect. */
	public static final float inactiveRadius = 70f, inactiveDamage = 340f;

	public static Warhead explosive, incendiary, radiation, emp;
	public static final Seq<Warhead> all = new Seq<>();

	public final String name;
	public final Color color;
	/** Radius and damage of the active blast. */
	public final float radius, damage;

	protected Warhead(String name, Color color, float radius, float damage){
		this.name = name;
		this.color = color;
		this.radius = radius;
		this.damage = damage;
		all.add(this);
	}

	/** What the active blast leaves or does beyond its damage. Server side only. */
	public abstract void special(float x, float y);

	public static void load(){
		all.clear();

		explosive = new Warhead("explosive", Color.valueOf("ffb347"), 230f, 2800f){
			@Override
			public void special(float x, float y){
				//Pure force: the second, smaller blast is the shockwave that follows the first.
				Damage.damage(Team.derelict, x, y, radius * 0.45f, damage * 0.5f, true, true, true);
			}
		};

		incendiary = new Warhead("incendiary", Color.valueOf("ff6a3d"), 190f, 900f){
			@Override
			public void special(float x, float y){
				Damage.createIncend(x, y, radius * 0.9f, 60);
				Units.nearby(x - radius, y - radius, radius * 2f, radius * 2f, u -> {
					if(u.within(x, y, radius + u.hitSize / 2f)) u.apply(StatusEffects.burning, 60f * 8f);
				});
				burnGround(x, y, radius * 0.85f);
				zone(NukeBullets.incendiaryZone, x, y);
			}
		};

		radiation = new Warhead("radiation", Color.valueOf("9bff4a"), 170f, 650f){
			@Override
			public void special(float x, float y){
				zone(NukeBullets.radiationZone, x, y);
			}
		};

		emp = new Warhead("emp", Color.valueOf("7fd8ff"), 260f, 260f){
			@Override
			public void special(float x, float y){
				float r2 = radius * radius;
				Units.nearby(x - radius, y - radius, radius * 2f, radius * 2f, u -> {
					if(u.within(x, y, radius + u.hitSize / 2f)) u.apply(NukeStatus.empShock, 60f * 15f);
				});
				Units.nearbyBuildings(x, y, radius, b -> {
					if(b.dst2(x, y) > r2 || b.power == null) return;
					//Whatever is stored is gone, and whatever runs on power stops for a while.
					if(b.power.graph != null) b.power.graph.useBatteries(b.power.graph.getBatteryStored());
					TemporalDisable.disable(b, 60f * 15f, true);
				});
				NukeFx.empPulse.at(x, y, radius, color);
			}
		};
	}

	/** Leaves a lingering patch of this kind. */
	protected static void zone(ZoneBulletType type, float x, float y){
		type.create(null, Team.derelict, x, y, 0f);
	}

	/** Sets fires on the ground, a few at random places within the radius. */
	protected static void burnGround(float x, float y, float range){
		int tr = (int)(range / tilesize);
		for(int i = 0; i < 40; i++){
			float a = arc.math.Mathf.random(360f), d = arc.math.Mathf.sqrt(arc.math.Mathf.random()) * tr;
			Tile tile = Vars.world.tileWorld(x + arc.math.Mathf.cosDeg(a) * d * tilesize, y + arc.math.Mathf.sinDeg(a) * d * tilesize);
			if(tile != null) Fires.create(tile);
		}
	}
}
