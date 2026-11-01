package newhorizon.expand.block.hiemara;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import newhorizon.expand.block.env.RichWater;

import static mindustry.Vars.tilesize;

/** Marks every rich-water tile in its range with the icon of the ore, also the ones that are under ice. */
public class Sonar extends Block{
	/** Range in tiles. */
	public float range = 28f;
	/** Ticks between two scans. */
	public float scanTime = 90f;
	
	public Sonar(String name){
		super(name);
		
		update = true;
		solid = true;
		hasPower = true;
		placeableLiquid = true;
	}
	
	@Override
	public void setStats(){
		super.setStats();
		
		stats.add(Stat.range, range, StatUnit.blocks);
	}
	
	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		super.drawPlace(x, y, rotation, valid);
		
		Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range * tilesize, Pal.accent);
	}
	
	public class SonarBuild extends Building{
		/** The rich-water tiles found at the last scan. */
		public final Seq<Tile> found = new Seq<>();
		protected float scanTimer = Mathf.random(scanTime);
		protected float sweep;
		
		@Override
		public void updateTile(){
			sweep += efficiency * Time.delta;
			scanTimer += Time.delta;
			if(efficiency <= 0f){
				found.clear();
				return;
			}
			if(scanTimer >= scanTime || (found.isEmpty() && scanTimer >= 20f)){
				scanTimer = 0f;
				scan();
			}
		}
		
		/** Looks for rich water in range, under ice too. */
		public void scan(){
			found.clear();
			int r = (int)range;
			for(int dx = -r; dx <= r; dx++){
				for(int dy = -r; dy <= r; dy++){
					Tile other = Vars.world.tile(tile.x + dx, tile.y + dy);
					if(other == null || !within(other.worldx(), other.worldy(), range * tilesize))continue;
					if(RichWater.richItemOf(other) != null)found.add(other);
				}
			}
		}
		
		@Override
		public void draw(){
			super.draw();
			if(efficiency <= 0f)return;
			
			Draw.z(Layer.overlayUI - 1f);
			float pulse = 0.7f + 0.3f * Mathf.absin(Time.time, 8f, 1f);
			for(Tile other : found){
				Item item = RichWater.richItemOf(other);
				if(item == null)continue;
				Draw.color(item.color, 0.45f);
				Lines.stroke(1f);
				Lines.circle(other.worldx(), other.worldy(), 4.5f * pulse);
				Draw.color();
				Draw.rect(item.uiIcon, other.worldx(), other.worldy(), 5f, 5f);
			}
			
			Draw.color(Pal.accent, 0.35f);
			Lines.stroke(1.2f);
			Lines.circle(x, y, (sweep % 120f) / 120f * range * tilesize);
			Draw.reset();
		}
		
		@Override
		public void drawSelect(){
			Drawf.dashCircle(x, y, range * tilesize, team.color);
		}
	}
}
