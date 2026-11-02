package newhorizon.expand.block.hiemara;

import arc.Core;
import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.Tile;
import newhorizon.expand.game.HotSpotManager;

import static mindustry.Vars.tilesize;

/** Holds the volcano of a hot spot back for as long as it is powered. It has to stand within the reach of the hot spot. */
public class SeismicStabilizer extends Block{
	/** Every stabilizer that has been running, checked by the volcano. */
	public static final Seq<Building> active = new Seq<>();
	
	public SeismicStabilizer(String name){
		super(name);
		
		update = true;
		solid = true;
		hasPower = true;
		placeableLiquid = true;
	}
	
	@Override
	public boolean canPlaceOn(Tile tile, Team team, int rotation){
		return HotSpotManager.spotAt(tile.worldx() + offset, tile.worldy() + offset) != null;
	}
	
	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		super.drawPlace(x, y, rotation, valid);
		
		for(HotSpotManager.Spot spot : HotSpotManager.spots){
			Drawf.dashCircle(spot.wx(), spot.wy(), HotSpotManager.influence * tilesize, Pal.remove);
		}
		if(!valid)drawPlaceText(Core.bundle.get("stabilizer.outside"), x, y, false);
	}
	
	@Override
	public void setBars(){
		super.setBars();
		
		addBar("stabilizing", (SeismicStabilizerBuild build) -> new Bar(
			() -> Core.bundle.get(build.holding() ? "stabilizer.holding" : "stabilizer.idle"),
			() -> build.holding() ? Pal.heal : Color.lightGray,
			() -> build.holding() ? 1f : 0f
		));
	}
	
	public class SeismicStabilizerBuild extends Building{
		protected boolean registered;
		
		public boolean holding(){
			return efficiency > 0f && HotSpotManager.spotAt(x, y) != null;
		}
		
		@Override
		public void updateTile(){
			if(!registered){
				registered = true;
				active.add(this);
			}
			active.removeAll(b -> !b.isValid());
		}
		
		@Override
		public void drawSelect(){
			var spot = HotSpotManager.spotAt(x, y);
			if(spot != null)Drawf.dashCircle(spot.wx(), spot.wy(), HotSpotManager.influence * tilesize, holding() ? Pal.heal : Pal.remove);
		}
	}
}
