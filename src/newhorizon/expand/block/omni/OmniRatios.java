package newhorizon.expand.block.omni;

import arc.struct.ObjectFloatMap;
import arc.struct.Seq;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.ctype.UnlockableContent;
import mindustry.type.Item;
import mindustry.type.Liquid;

/**
 * What omni-matter can be turned into, and how much of it. Every resource of the base game is on the list (not those of mods): an item
 * comes out in that many pieces for one omni-matter, a liquid in that many units. The numbers are in one table, here, so that they are
 * easy to tune: the rarer the resource the fewer pieces.
 */
public class OmniRatios{
	/** Pieces per omni-matter for the items that are not in the table, and units for the liquids that are not. */
	public static float defaultItem = 4f, defaultLiquid = 150f;

	private static final ObjectFloatMap<String> table = new ObjectFloatMap<>();
	private static Seq<UnlockableContent> outputs;

	static{
		put(10f, "copper", "lead", "sand", "scrap", "coal");
		put(6f, "graphite", "metaglass", "beryllium", "spore-pod");
		put(4f, "titanium", "silicon", "pyratite", "blast-compound", "tungsten", "oxide");
		put(2f, "thorium", "plastanium", "carbide", "dormant-cyst");
		put(1f, "phase-fabric", "surge-alloy", "fissile-matter");
		put(400f, "water");
		put(200f, "slag", "oil");
		put(100f, "cryofluid");
	}

	private static void put(float amount, String... names){
		for(String name : names) table.put(name, amount);
	}

	/** How many pieces (items) or units (liquid) one omni-matter makes. */
	public static float perOmni(UnlockableContent content){
		return table.get(content.name, content instanceof Liquid ? defaultLiquid : defaultItem);
	}

	/** Everything the factory can make: the items, then the liquids, of the base game. */
	public static Seq<UnlockableContent> outputs(){
		if(outputs == null){
			outputs = new Seq<>();
			outputs.addAll(Items.copper, Items.lead, Items.metaglass, Items.graphite, Items.sand, Items.coal, Items.titanium, Items.thorium,
				Items.scrap, Items.silicon, Items.plastanium, Items.phaseFabric, Items.surgeAlloy, Items.sporePod, Items.blastCompound, Items.pyratite,
				Items.beryllium, Items.tungsten, Items.oxide, Items.carbide, Items.fissileMatter, Items.dormantCyst);
			outputs.addAll(Liquids.water, Liquids.slag, Liquids.oil, Liquids.cryofluid, Liquids.arkycite, Liquids.gallium, Liquids.neoplasm,
				Liquids.ozone, Liquids.hydrogen, Liquids.nitrogen, Liquids.cyanogen);
		}
		return outputs;
	}

	public static boolean allowed(UnlockableContent content){
		return content instanceof Item || content instanceof Liquid ? outputs().contains(content) : false;
	}
}
