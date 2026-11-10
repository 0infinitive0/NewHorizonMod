package newhorizon.content.industry;

/**
 * Industry: the big logistics and production blocks (the ultimate silicon factory, better pumps, tanks and batteries, long-range mass
 * drivers, channel teleporters, the electric separator and the fusion reactor). Like the nukes they belong to no faction: they are made
 * only of items every planet has, and the tech tree branch is attached from {@code NHTechTree} to all three planets.
 */
public class IndustryContent{
	public static void load(){
		IndustryLiquids.load();
		IndustryBlocks.load();
	}
}
