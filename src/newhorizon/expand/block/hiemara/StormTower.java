package newhorizon.expand.block.hiemara;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Strings;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.Lightning;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

import static mindustry.Vars.tilesize;

/**
 * Does not aim: for as long as there are enemies in range it keeps calling lightning down on top of them, on several at a time.
 */
public class StormTower extends Block{
	/** Range in tiles. */
	public float range = 24f;
	/** Ticks between two volleys. */
	public float strikeTime = 24f;
	/** Targets struck in one volley. */
	public int targets = 4;
	public float damage = 38f;
	public int lightningLength = 20;
	public Color color = Color.valueOf("b9e6ff");
	
	/** A bolt out of the sky onto the target. */
	public static final Effect skyBolt = new Effect(16f, 400f, e -> {
		Draw.z(Layer.effect + 1f);
		Draw.color(Color.white, Color.valueOf("b9e6ff"), e.fin());
		Lines.stroke(3f * e.fout());
		float top = 260f;
		float x = e.x, y = e.y;
		float px = x, py = y;
		for(int i = 1; i <= 7; i++){
			float f = i / 7f;
			float nx = x + (i == 7 ? 0f : Mathf.randomSeed(e.id + i * 31, -9f, 9f)), ny = y + top * f;
			Lines.line(px, py, nx, ny);
			px = nx;
			py = ny;
		}
		Fill.light(x, y, 8, 16f * e.fout(), Color.white, Color.clear);
	});
	
	public StormTower(String name){
		super(name);
		
		update = true;
		solid = true;
		hasPower = true;
		placeableLiquid = true;
		sync = true;
	}
	
	@Override
	public void setStats(){
		super.setStats();
		stats.add(Stat.range, range, StatUnit.blocks);
		stats.add(Stat.targetsAir, true);
		stats.add(Stat.targetsGround, true);
		stats.add(Stat.damage, damage, StatUnit.none);
		stats.add(Stat.abilities, Core.bundle.format("stat.storm-tower", targets, Strings.autoFixed(60f / strikeTime, 1)));
	}
	
	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		super.drawPlace(x, y, rotation, valid);
		Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range * tilesize, Pal.accent);
	}
	
	public class StormTowerBuild extends Building{
		protected float timer = 0f, scanTimer = 0f;
		protected boolean hasTarget;
		protected float glow;
		protected final Seq<Unit> found = new Seq<>();
		
		@Override
		public boolean shouldConsume(){
			return hasTarget;
		}
		
		@Override
		public void updateTile(){
			glow = Mathf.lerpDelta(glow, hasTarget && efficiency > 0f ? 1f : 0f, 0.08f);
			
			timer += delta() * efficiency;
			if(hasTarget){
				if(timer < strikeTime)return;
			}else{
				//nothing to strike: look again now and then
				scanTimer += delta();
				if(scanTimer < 15f)return;
				scanTimer = 0f;
			}
			
			found.clear();
			float r = range * tilesize;
			Units.nearbyEnemies(team, x - r, y - r, r * 2f, r * 2f, u -> {
				if(u.isValid() && u.within(this, r + u.hitSize / 2f) && u.checkTarget(true, true))found.add(u);
			});
			hasTarget = found.size > 0;
			if(!hasTarget || efficiency <= 0f)return;
			
			timer = 0f;
			//the closest ones first, so a crowd is thinned from the front
			found.sort(u -> u.dst2(this));
			
			int count = Math.min(targets, found.size);
			for(int i = 0; i < count; i++){
				Unit u = found.get(i);
				skyBolt.at(u.x, u.y, 0f, color);
				Lightning.create(team, color, damage, u.x, u.y, Mathf.random(360f), lightningLength);
				Fx.hitLancer.at(u.x, u.y, 0f, color);
			}
			Sounds.shootArc.at(this, Mathf.random(0.9f, 1.1f));
		}
		
		@Override
		public void drawSelect(){
			Drawf.dashCircle(x, y, range * tilesize, team.color);
		}
		
		@Override
		public void draw(){
			super.draw();
			if(glow > 0.01f){
				Draw.z(Layer.blockAdditive);
				Draw.blend(arc.graphics.Blending.additive);
				Draw.color(color, glow * (0.6f + 0.4f * Mathf.absin(Time.time, 8f, 1f)));
				Fill.circle(x, y, size * tilesize * 0.28f);
				Draw.blend();
				Draw.reset();
			}
		}
	}
}
