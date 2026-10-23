package newhorizon.expand.block.env;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.struct.ObjectMap;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Liquids;
import mindustry.graphics.MultiPacker;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

/**
 * Frozen version of a water floor. Every freezable water floor owns one ice floor which remembers the original water
 * ({@link #melted}), so the ice melts back to exactly the tile it came from.
 */
public class IceFloor extends Floor{
	/** Water floor -> ice floor. */
	public static final ObjectMap<Floor, IceFloor> iceOf = new ObjectMap<>();

	/** The water this ice returns to when it melts or is mined. */
	public final Floor melted;

	public IceFloor(String name, Floor melted){
		super(name, 0);
		this.melted = melted;

		dragMultiplier = 0.25f;
		speedMultiplier = 0.95f;
		blendGroup = Blocks.ice;
		inEditor = false;
		albedo = 0.65f;
	}

	/** Creates an ice floor for every water floor that is registered at this point. */
	public static void createAll(){
		for(Block block : Vars.content.blocks().copy()){
			if(block instanceof Floor && !(block instanceof IceFloor) && freezable((Floor)block)){
				register((Floor)block);
			}
		}
	}

	/** Makes the given floor freezable. Has to be called before content is initialized. */
	public static IceFloor register(Floor water){
		IceFloor ice = iceOf.get(water);
		if(ice == null){
			ice = new IceFloor("ice-" + water.name, water);
			iceOf.put(water, ice);
		}
		return ice;
	}

	public static boolean freezable(Floor floor){
		return floor.isLiquid && floor.liquidDrop == Liquids.water;
	}

	/** @return the ice floor for this floor, or null if it can not freeze. */
	public static IceFloor of(Floor water){
		return iceOf.get(water);
	}

	/** Changes the floor of a tile in a synced way and keeps the ground AI paths up to date. */
	public static void setFloor(Tile tile, Floor floor){
		if(Vars.net.client())return;
		tile.setFloorNet(floor, tile.overlay());
		if(Vars.pathfinder != null)Vars.pathfinder.updateTile(tile);
	}

	public void melt(Tile tile){
		setFloor(tile, melted);
	}

	@Override
	public void init(){
		super.init();

		if(Core.bundle != null){
			localizedName = Core.bundle.get("nh.ice-floor", "Frozen {0}").replace("{0}", melted.localizedName);
		}
	}

	@Override
	public void load(){
		super.load();

		//Share the vanilla ice sprites.
		variantRegions = new TextureRegion[3];
		for(int i = 0; i < variantRegions.length; i++){
			variantRegions[i] = Core.atlas.find("ice" + (i + 1));
		}
		fullIcon = uiIcon = region = variantRegions[0];
	}

	@Override
	public TextureRegion[] icons(){
		return new TextureRegion[]{region};
	}

	@Override
	public void createIcons(MultiPacker packer){
		//No own sprites.
	}
}
