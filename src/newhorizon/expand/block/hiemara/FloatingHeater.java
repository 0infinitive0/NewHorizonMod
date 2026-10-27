package newhorizon.expand.block.hiemara;

import arc.math.Mathf;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import newhorizon.expand.block.env.IceFloor;
import newhorizon.expand.game.IceManager;

import static mindustry.Vars.tilesize;

/** Keeps water around it from freezing and thaws the ice that is already there. */
public class FloatingHeater extends Block{
	/** Radius in tiles. */
	public float radius = 8f;
	/** Ice tiles thawed per second, while powered. */
	public float thawPerSecond = 3f;
	
	public FloatingHeater(String name){
		super(name);
		
		update = true;
		solid = true;
		hasPower = true;
		placeableLiquid = true;
		configurable = false;
	}
	
	@Override
	public void init(){
		IceManager.heaterRadius.put(this, radius * tilesize);
		super.init();
	}
	
	@Override
	public void setStats(){
		super.setStats();
		
		stats.add(Stat.range, radius, StatUnit.blocks);
	}
	
	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		super.drawPlace(x, y, rotation, valid);
		
		Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, radius * tilesize, Pal.accent);
	}
	
	public class FloatingHeaterBuild extends Building{
		public float thawBudget;
		public float warmup;
		
		@Override
		public void updateTile(){
			warmup = Mathf.lerpDelta(warmup, efficiency > 0f ? 1f : 0f, 0.04f);
			
			if(!IceManager.heaters.contains(this, true))IceManager.heaters.add(this);
			
			if(Vars.net.client() || efficiency <= 0f)return;
			
			thawBudget += thawPerSecond / 60f * efficiency * Time.delta;
			while(thawBudget >= 1f){
				thawBudget -= 1f;
				thawRandom();
			}
		}
		
		protected void thawRandom(){
			int r = (int)radius;
			for(int tries = 0; tries < 12; tries++){
				Tile other = Vars.world.tile(tile.x + Mathf.random(-r, r), tile.y + Mathf.random(-r, r));
				if(other == null || !within(other.worldx(), other.worldy(), radius * tilesize))continue;
				if(other.floor() instanceof IceFloor && other.build == null){
					((IceFloor)other.floor()).melt(other);
					return;
				}
			}
		}
		
		@Override
		public void onRemoved(){
			IceManager.heaters.remove(this, true);
			super.onRemoved();
		}
		
		@Override
		public void drawSelect(){
			Drawf.dashCircle(x, y, radius * tilesize, team.color);
		}
		
		@Override
		public float warmup(){
			return warmup;
		}
	}
}
