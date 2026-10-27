package newhorizon.expand.block.production.filter;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.struct.ObjectFloatMap;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import newhorizon.expand.block.env.RichWater;

/**
 * Takes ore out of the water. On plain water it slowly filters out every ore its tier can handle. As soon as a
 * rich-water tile is under it, it locks to that ore, produces nothing else, and every rich tile counts five times.
 */
public class FilterBlock extends Block{
	/** Highest item hardness this filter can handle. */
	public int tier = 1;
	/** Items per second a single plain water tile yields, for each ore. */
	public float baseRate = 0.01f;
	/** How much more a rich-water tile gives of its own ore. */
	public float richMultiplier = 5f;
	/** Every ore that can be filtered out of the water. */
	public static final Seq<Item> ores = new Seq<>();
	
	protected static final Seq<Tile> tmpTiles = new Seq<>();
	
	public FilterBlock(String name){
		super(name);
		
		update = true;
		solid = true;
		hasItems = true;
		itemCapacity = 20;
		placeableLiquid = true;
		group = mindustry.world.meta.BlockGroup.none;
	}
	
	/** What a filter at a position would produce. */
	public static class Result{
		/** The ore it locks to, or null on plain water. */
		public Item locked;
		public int rich, plain;
		/** Items per second per ore. */
		public final ObjectFloatMap<Item> rates = new ObjectFloatMap<>();
		public boolean tooLow;
	}
	
	public Result analyze(Tile origin, Result result){
		result.locked = null;
		result.rich = result.plain = 0;
		result.tooLow = false;
		result.rates.clear();
		
		ObjectFloatMap<Item> counts = new ObjectFloatMap<>();
		for(Tile tile : origin.getLinkedTilesAs(this, tmpTiles)){
			Item rich = RichWater.richItemOf(tile);
			if(rich != null){
				counts.increment(rich, 0f, 1f);
			}else if(RichWater.isWater(tile)){
				result.plain++;
			}
		}
		
		if(counts.size > 0){
			float best = -1f;
			for(var entry : counts){
				if(entry.value > best || (entry.value == best && entry.key.id < result.locked.id)){
					best = entry.value;
					result.locked = entry.key;
				}
			}
			result.rich = (int)best;
			
			if(result.locked.hardness > tier){
				result.tooLow = true;
			}else{
				result.rates.put(result.locked, baseRate * (richMultiplier * result.rich + result.plain));
			}
		}else if(result.plain > 0){
			for(Item ore : ores){
				if(ore.hardness <= tier)result.rates.put(ore, baseRate * result.plain);
			}
		}
		
		return result;
	}
	
	@Override
	public boolean canPlaceOn(Tile tile, mindustry.game.Team team, int rotation){
		for(Tile other : tile.getLinkedTilesAs(this, tmpTiles)){
			if(RichWater.isWater(other))return true;
		}
		return false;
	}
	
	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		super.drawPlace(x, y, rotation, valid);
		
		Tile tile = mindustry.Vars.world.tile(x, y);
		if(tile == null)return;
		
		Result result = analyze(tile, new Result());
		String text;
		if(result.tooLow){
			text = mindustry.Vars.ui != null ? arc.Core.bundle.format("filter.too-low", result.locked.localizedName) : "";
		}else if(result.locked != null){
			text = result.locked.localizedName + " " + Strings.fixed(result.rates.get(result.locked, 0f) * 60f, 1) + "/min";
		}else{
			text = arc.Core.bundle.get("filter.plain");
		}
		drawPlaceText(text, x, y, valid);
	}
	
	@Override
	public void setStats(){
		super.setStats();
		
		stats.add(Stat.drillSpeed, baseRate, StatUnit.itemsSecond);
		stats.add(Stat.drillTier, table -> {
			table.row();
			for(Item ore : ores){
				if(ore.hardness <= tier)table.image(ore.uiIcon).size(24f).pad(2f);
			}
		});
	}
	
	@Override
	public void setBars(){
		super.setBars();
		
		addBar("filter", (FilterBuild build) -> new Bar(
			() -> build.statusText(),
			() -> build.locked != null ? build.locked.color : Pal.accent,
			() -> build.rate > 0f ? 1f : 0f
		));
	}
	
	public class FilterBuild extends Building{
		public final Result result = new Result();
		public final ObjectFloatMap<Item> progress = new ObjectFloatMap<>();
		public Item locked;
		public float rate;
		public float warmup;
		
		public String statusText(){
			if(result.tooLow)return arc.Core.bundle.format("filter.too-low", result.locked.localizedName);
			if(locked != null)return locked.localizedName + " " + Strings.fixed(rate * 60f, 1) + "/min";
			return arc.Core.bundle.get(rate > 0f ? "filter.plain" : "filter.nothing");
		}
		
		@Override
		public void placed(){
			super.placed();
			rescan();
		}
		
		@Override
		public void onProximityUpdate(){
			super.onProximityUpdate();
			rescan();
		}
		
		public void rescan(){
			analyze(tile, result);
			locked = result.tooLow ? null : result.locked;
			rate = 0f;
			for(var entry : result.rates)rate += entry.value;
			
			//Progress of ores that are no longer produced is dropped.
			for(Item item : progress.keys().toSeq()){
				if(!result.rates.containsKey(item))progress.remove(item, 0f);
			}
		}
		
		@Override
		public boolean shouldConsume(){
			return items.total() < itemCapacity && enabled;
		}
		
		@Override
		public void updateTile(){
			if(timer(timerDump, dumpTime)){
				dump();
			}
			
			warmup = Mathf.lerpDelta(warmup, rate > 0f && items.total() < itemCapacity ? 1f : 0f, 0.02f);
			if(rate <= 0f || items.total() >= itemCapacity)return;
			
			for(var entry : result.rates){
				float value = progress.get(entry.key, 0f) + entry.value / 60f * delta() * efficiency;
				if(value >= 1f && items.total() < itemCapacity){
					value -= 1f;
					offload(entry.key);
				}
				progress.put(entry.key, value);
			}
		}
		
		@Override
		public boolean canDump(Building to, Item item){
			return !(to instanceof FilterBuild);
		}
		
		@Override
		public float warmup(){
			return warmup;
		}
		
		@Override
		public float progress(){
			return warmup;
		}
	}
}
