package newhorizon.expand.block.hiemara;

import arc.Core;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import newhorizon.expand.block.production.SandDredger;
import newhorizon.expand.game.HotSpotManager;

/**
 * Brings a volcano up where it stands: it takes a lot of blast compound, and when it is full it can be set off, which destroys it and
 * starts an eruption on the spot. It can only stand on shallow water. If a hot spot is in reach, that one starts to wait again.
 */
public class SeismicDestabilizer extends Block{
	/** Radius in tiles of the eruption. */
	public float radius = HotSpotManager.eruptRadius;
	
	public SeismicDestabilizer(String name){
		super(name);
		
		update = true;
		solid = true;
		hasItems = true;
		configurable = true;
		
		config(Boolean.class, (SeismicDestabilizerBuild build, Boolean go) -> {
			if(go)build.detonate();
		});
	}
	
	@Override
	public boolean canPlaceOn(Tile tile, Team team, int rotation){
		//only within the reach of a hot spot
		if(HotSpotManager.spotAt(tile.worldx() + offset, tile.worldy() + offset) == null)return false;
		for(Tile other : tile.getLinkedTilesAs(this, tempTiles)){
			if(!SandDredger.shallow(other))return false;
		}
		return true;
	}
	
	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		super.drawPlace(x, y, rotation, valid);
		
		for(HotSpotManager.Spot spot : HotSpotManager.spots){
			mindustry.graphics.Drawf.dashCircle(spot.wx(), spot.wy(), HotSpotManager.influence * mindustry.Vars.tilesize, Pal.remove);
		}
		if(!valid)drawPlaceText(Core.bundle.get("destabilizer.outside"), x, y, false);
	}
	
	@Override
	public void setBars(){
		super.setBars();
		
		addBar("charge", (SeismicDestabilizerBuild build) -> new Bar(
			() -> Core.bundle.format("destabilizer.charge", build.items.get(Items.blastCompound), itemCapacity),
			() -> build.ready() ? Pal.remove : Pal.accent,
			() -> build.items.get(Items.blastCompound) / (float)itemCapacity
		));
	}
	
	public class SeismicDestabilizerBuild extends Building{
		public boolean ready(){
			return items.get(Items.blastCompound) >= itemCapacity;
		}
		
		@Override
		public boolean acceptItem(Building source, Item item){
			return item == Items.blastCompound && items.get(item) < itemCapacity;
		}
		
		@Override
		public int getMaximumAccepted(Item item){
			return itemCapacity;
		}
		
		/** Destroys the block and starts the eruption where it stood. */
		public void detonate(){
			if(Vars.net.client() || !ready() || !isValid())return;
			int cx = tile.x, cy = tile.y;
			//the hot spot comes up where the destabilizer stands, and the volcano erupts there
			HotSpotManager.Spot spot = HotSpotManager.spotAt(x, y);
			if(spot != null)HotSpotManager.moveSpot(spot, cx, cy);
			HotSpotManager.erupt(cx, cy, radius);
			kill();
		}
		
		@Override
		public void buildConfiguration(Table table){
			table.button(Core.bundle.get("destabilizer.detonate"), Icon.warning, Styles.defaultt, () -> {
				configure(true);
				deselect();
			}).size(200f, 50f).disabled(b -> !ready());
		}
	}
}
