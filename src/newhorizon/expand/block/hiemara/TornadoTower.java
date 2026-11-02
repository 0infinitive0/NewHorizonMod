package newhorizon.expand.block.hiemara;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import newhorizon.expand.bullets.nuke.TornadoBulletType;

import static mindustry.Vars.tilesize;

/**
 * Raises a tornado on a spot the player picks inside its range, as soon as an enemy is near that spot.
 * Select the tower and tap the map to move the spot. The tornado is the one of the storm nuke, and like that one it hurts
 * every team: the units and walls of the owner too.
 */
public class TornadoTower extends Block{
	/** Range in tiles in which the spot can be placed. */
	public float range = 30f;
	/** Ticks between two tornadoes. */
	public float reload = 20f * 60f;
	/** The tornado raised. */
	public BulletType tornado;
	/** An enemy this close (in tiles) to the spot, beyond the radius of the tornado, makes it rise. */
	public float trigger = 4f;
	/** The tornado tears at everything in reach, the tower too, so the spot has to be at least its radius and this many tiles away. */
	public float safeMargin = 3f;
	
	/** @return the shortest distance in world units from the tower to the spot. */
	public float minSpotDistance(){
		float radius = tornado instanceof TornadoBulletType ? ((TornadoBulletType)tornado).radius : 0f;
		return radius + (size / 2f + safeMargin) * tilesize;
	}
	
	public TornadoTower(String name){
		super(name);
		
		update = true;
		solid = true;
		hasPower = true;
		configurable = true;
		saveConfig = true;
		placeableLiquid = true;
		sync = true;
		
		config(Point2.class, (TornadoTowerBuild build, Point2 point) -> build.spot.set(point));
		configClear((TornadoTowerBuild build) -> build.spot.set(-1, -1));
	}
	
	@Override
	public void setStats(){
		super.setStats();
		stats.add(Stat.range, range, StatUnit.blocks);
		stats.add(Stat.reload, 60f / reload, StatUnit.perSecond);
		stats.add(Stat.abilities, Core.bundle.get("stat.tornado-tower"));
	}
	
	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		super.drawPlace(x, y, rotation, valid);
		Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range * tilesize, Pal.accent);
	}
	
	public class TornadoTowerBuild extends Building{
		/** The tile the tornado is raised on, -1 if none has been picked. */
		public final Point2 spot = new Point2(-1, -1);
		protected float charge;
		
		public boolean hasSpot(){
			return spot.x >= 0 && spot.y >= 0;
		}
		
		public float spotX(){
			return spot.x * tilesize;
		}
		
		public float spotY(){
			return spot.y * tilesize;
		}
		
		@Override
		public boolean shouldConsume(){
			return hasSpot() && charge < reload;
		}
		
		@Override
		public Point2 config(){
			return spot.cpy();
		}
		
		@Override
		public boolean onConfigureTapped(float tx, float ty){
			if(within(tx, ty, range * tilesize) && !within(tx, ty, minSpotDistance())){
				configure(new Point2(mindustry.Vars.world.toTile(tx), mindustry.Vars.world.toTile(ty)));
				return true;
			}
			return false;
		}
		
		@Override
		public void buildConfiguration(Table table){
			table.add(Core.bundle.format("tornado-tower.hint", Strings.autoFixed(minSpotDistance() / tilesize, 0), Strings.autoFixed(range, 0))).pad(6f).width(220f).wrap().left();
		}
		
		@Override
		public void updateTile(){
			if(!hasSpot())return;
			
			charge = Math.min(charge + delta() * efficiency, reload);
			if(charge < reload || Vars.net.client() || tornado == null)return;
			
			float radius = tornado instanceof TornadoBulletType ? ((TornadoBulletType)tornado).radius : 0f;
			float reach = radius + trigger * tilesize;
			boolean enemy = Units.anyEntities(spotX() - reach, spotY() - reach, reach * 2f, reach * 2f, u -> u.team != team && u.team != mindustry.game.Team.derelict && u.checkTarget(true, true) && u.within(spotX(), spotY(), reach + u.hitSize / 2f));
			if(!enemy)return;
			
			charge = 0f;
			tornado.create(this, team, spotX(), spotY(), Mathf.random(360f));
		}
		
		@Override
		public void drawSelect(){
			Drawf.dashCircle(x, y, range * tilesize, team.color);
			Drawf.dashCircle(x, y, minSpotDistance(), Pal.remove);
			if(hasSpot()){
				float radius = tornado instanceof TornadoBulletType ? ((TornadoBulletType)tornado).radius : 60f;
				Draw.z(Layer.overlayUI);
				Lines.stroke(2f, Pal.accent);
				Lines.circle(spotX(), spotY(), radius * 0.6f);
				Lines.stroke(1f, Color.white);
				Lines.circle(spotX(), spotY(), 6f + Mathf.absin(Time.time, 6f, 3f));
				Lines.line(x, y, spotX(), spotY());
				Draw.reset();
			}
		}
		
		@Override
		public void write(Writes write){
			super.write(write);
			write.i(spot.x);
			write.i(spot.y);
			write.f(charge);
		}
		
		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			spot.set(read.i(), read.i());
			charge = read.f();
		}
	}
}
