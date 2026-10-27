package newhorizon.expand.block.production.ice;

import arc.math.Mathf;
import arc.struct.IntSet;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import newhorizon.content.NHItems;
import newhorizon.expand.block.env.IceFloor;
import newhorizon.expand.units.ai.IceMinerAI;

import static mindustry.Vars.tilesize;

/** Spawns drones that mine {@link IceFloor} in a radius around the station and bring the ice back. */
public class IceMiningStation extends Block{
	/** Every ice mining station block, filled by the constructor. */
	public static final arc.struct.Seq<IceMiningStation> all = new arc.struct.Seq<>();
	
	public UnitType unitType;
	public int maxUnits = 3;
	/** Mining radius in world units. */
	public float radius = 12 * tilesize;
	public float spawnTime = 8f * 60f;
	public Effect spawnEffect = Fx.spawn;
	
	protected final int timerCount = timers++;
	
	public IceMiningStation(String name){
		super(name);
		
		update = true;
		solid = true;
		hasItems = true;
		hasPower = true;
		itemCapacity = 60;
		ambientSound = mindustry.gen.Sounds.none;
		
		all.add(this);
	}
	
	@Override
	public void setStats(){
		super.setStats();
		
		stats.add(Stat.range, radius / tilesize, StatUnit.blocks);
		stats.add(Stat.maxUnits, maxUnits);
		if(unitType != null)stats.add(Stat.output, table -> table.add(unitType.localizedName));
	}
	
	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		super.drawPlace(x, y, rotation, valid);
		
		Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, radius, Pal.accent);
	}
	
	public class IceMiningStationBuild extends Building{
		/** Tiles that drones are about to mine. */
		public IntSet claimed = new IntSet();
		public float spawnProgress;
		public float warmup;
		public int unitCount;
		
		@Override
		public boolean acceptItem(Building source, Item item){
			//Only the drones of this station hand over their cargo.
			return source == this && item == NHItems.ice && items.get(item) < getMaximumAccepted(item);
		}
		
		@Override
		public boolean shouldConsume(){
			return unitCount < maxUnits;
		}
		
		@Override
		public void updateTile(){
			warmup = Mathf.lerpDelta(warmup, efficiency > 0 ? 1f : 0f, 0.03f);
			
			if(timer(timerDump, dumpTime)){
				dump(NHItems.ice);
			}
			
			if(timer(timerCount, 60f)){
				countUnits();
			}
			
			if(unitType == null || Vars.net.client())return;
			
			if(unitCount < maxUnits && efficiency > 0){
				spawnProgress += edelta();
				
				if(spawnProgress >= spawnTime){
					spawnProgress %= spawnTime;
					
					Unit unit = unitType.spawn(team, x, y);
					spawnEffect.at(unit.x, unit.y, 0f, team.color);
					unitCount++;
				}
			}
		}
		
		protected void countUnits(){
			int count = 0;
			for(Unit unit : Groups.unit){
				if(unit.team == team && unit.type == unitType && unit.controller() instanceof IceMinerAI && ((IceMinerAI)unit.controller()).home == this)count++;
			}
			unitCount = count;
		}
		
		/** Finds and claims the closest free ice tile in range for a drone. */
		public Tile claimIce(Unit unit){
			int range = (int)(radius / tilesize);
			Tile best = null;
			float bestDst = Float.MAX_VALUE;
			
			for(int dx = -range; dx <= range; dx++){
				for(int dy = -range; dy <= range; dy++){
					Tile other = Vars.world.tile(tile.x + dx, tile.y + dy);
					if(other == null || !(other.floor() instanceof IceFloor) || other.block() != Blocks.air)continue;
					if(!within(other.worldx(), other.worldy(), radius) || claimed.contains(other.pos()))continue;
					
					float dst = unit.dst2(other.worldx(), other.worldy());
					if(dst < bestDst){
						bestDst = dst;
						best = other;
					}
				}
			}
			
			if(best != null)claimed.add(best.pos());
			return best;
		}
		
		public void release(Tile tile){
			if(tile != null)claimed.remove(tile.pos());
		}
		
		@Override
		public void drawSelect(){
			Drawf.dashCircle(x, y, radius, team.color);
		}
		
		@Override
		public float progress(){
			return Mathf.clamp(spawnProgress / spawnTime);
		}
		
		@Override
		public float warmup(){
			return warmup;
		}
	}
}
