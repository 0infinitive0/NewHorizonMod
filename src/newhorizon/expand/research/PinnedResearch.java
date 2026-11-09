package newhorizon.expand.research;

import arc.Core;
import arc.Events;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.TechTree;
import mindustry.content.TechTree.TechNode;
import mindustry.ctype.ContentType;
import mindustry.ctype.MappableContent;
import mindustry.ctype.UnlockableContent;
import mindustry.game.EventType;
import mindustry.gen.Sounds;
import mindustry.type.ItemStack;
import mindustry.type.Planet;
import mindustry.type.Sector;

import static mindustry.Vars.state;

/**
 * Pinned research and auto research. The player pins research nodes (a queue); about every ten seconds the first pinned node that can be
 * researched has its missing items paid for out of the stored items of the player's OTHER sectors, and when it is complete it is unlocked
 * like a normal research and the next pin gets the items.
 *
 * <p>The sector being played is never touched: its core is neither read nor changed. Sectors without a base and sectors under attack are not
 * used either (the same rule the research dialog's item pool has). Nothing happens outside the campaign or on a multiplayer client.
 */
public class PinnedResearch{
	public static final String key = "nh-pinned-research", settingKey = "nh_auto_research";
	/** Ticks between two rounds of spending: ten seconds. */
	public static final float intervalTicks = 60f * 10f;

	private static final Seq<String> pinned = new Seq<>();
	private static float timer;
	private static boolean loaded;

	/** Reads the saved pins and starts the ten-second spending. */
	public static void load(){
		if(loaded) return;
		loaded = true;
		read();
		Events.run(EventType.Trigger.update, PinnedResearch::tick);
	}

	/** Re-reads the saved list (public for tests). */
	public static void read(){
		pinned.clear();
		String s = Core.settings.getString(key, "");
		for(String n : s.split(",")){
			if(!n.isEmpty() && !pinned.contains(n)) pinned.add(n);
		}
	}

	private static void write(){
		Core.settings.put(key, String.join(",", pinned));
	}

	public static boolean enabled(){
		return Core.settings.getBool(settingKey, true);
	}

	private static String id(UnlockableContent c){
		return c.getContentType().name() + ":" + c.name;
	}

	public static boolean isPinned(UnlockableContent c){
		return pinned.contains(id(c));
	}

	/** Pins the content (at the end of the queue) or unpins it. */
	public static void toggle(UnlockableContent c){
		if(!pinned.remove(id(c))) pinned.add(id(c));
		write();
	}

	public static void unpin(UnlockableContent c){
		if(pinned.remove(id(c))) write();
	}

	public static void clear(){
		pinned.clear();
		write();
	}

	private static UnlockableContent resolve(String id){
		int i = id.indexOf(':');
		if(i < 0) return null;
		try{
			MappableContent c = Vars.content.getByName(ContentType.valueOf(id.substring(0, i)), id.substring(i + 1));
			return c instanceof UnlockableContent u ? u : null;
		}catch(IllegalArgumentException e){
			return null;
		}
	}

	/** The node of a content in the tree of the planet being played, else its first node. */
	public static TechNode nodeOf(UnlockableContent c){
		Planet pl = state.isCampaign() ? state.rules.sector.planet : null;
		TechNode first = null;
		for(TechNode n : TechTree.all){
			if(n.content != c) continue;
			if(first == null) first = n;
			if(pl != null && n.rootNode == pl.techTree) return n;
		}
		return first;
	}

	/** The pinned nodes still to research, in order. Pins already researched, or no longer in any tree, drop out of the list. */
	public static Seq<TechNode> pins(){
		Seq<TechNode> out = new Seq<>();
		boolean changed = false;
		for(int i = 0; i < pinned.size; i++){
			UnlockableContent c = resolve(pinned.get(i));
			TechNode n = c == null ? null : nodeOf(c);
			if(c == null || n == null || c.unlocked()){
				pinned.remove(i--);
				changed = true;
				continue;
			}
			out.add(n);
		}
		if(changed) write();
		return out;
	}

	/** Whether the node can be paid for now: its content locked, its objectives done and its parent researched. */
	public static boolean researchable(TechNode node){
		if(node.content.unlocked()) return false;
		if(node.objectives.contains(o -> !o.complete())) return false;
		return node.parent == null || node.parent.content.unlocked();
	}

	/** How much of the node's cost is paid, 0 to 1. */
	public static float progress(TechNode node){
		int req = 0, fin = 0;
		for(int i = 0; i < node.requirements.length; i++){
			req += node.requirements[i].amount;
			fin += Math.min(node.finishedRequirements[i].amount, node.requirements[i].amount);
		}
		return req == 0 ? 1f : fin / (float)req;
	}

	private static void tick(){
		tickFor(Time.delta);
	}

	/** Advances the clock by {@code ticks} and spends when the interval is up (public so a test can drive it). Returns the items spent. */
	public static int tickFor(float ticks){
		if(Vars.net.client() || !state.isCampaign() || !enabled()) return 0;
		timer += ticks;
		if(timer < intervalTicks) return 0;
		timer = 0f;
		return step();
	}

	/** One round of spending (public so a test can run it). Returns the number of items spent. */
	public static int step(){
		if(Vars.net.client() || !state.isCampaign() || !enabled()) return 0;
		Sector cur = state.rules.sector;
		TechNode node = pins().find(PinnedResearch::researchable);
		if(node == null) return 0;

		//the sectors of this planet, other than the one being played, whose items count
		Seq<Sector> pool = cur.planet.sectors.select(s -> s != cur && !s.isBeingPlayed() && s.hasBase() && !s.isFrozen());

		int spent = 0;
		boolean complete = true;
		for(int i = 0; i < node.requirements.length; i++){
			ItemStack req = node.requirements[i], fin = node.finishedRequirements[i];
			int need = req.amount - fin.amount;
			if(need > 0 && pool.size > 0){
				//from the sector that has most of the item first
				Seq<Sector> order = pool.copy().sort(s -> -s.items().get(req.item));
				for(Sector s : order){
					if(need <= 0) break;
					int take = Math.min(need, s.items().get(req.item));
					if(take <= 0) continue;
					s.removeItem(req.item, take);
					fin.amount += take;
					need -= take;
					spent += take;
				}
			}
			if(fin.amount < req.amount) complete = false;
		}
		if(spent > 0) node.save();
		if(complete) finish(node);
		return spent;
	}

	/** Unlocks the node's content and its parents' like the research dialog does, and takes it off the queue. */
	protected static void finish(TechNode node){
		node.content.unlock();
		for(TechNode p = node.parent; p != null; p = p.parent) p.content.unlock();
		Events.fire(new EventType.ResearchEvent(node.content));
		pinned.remove(id(node.content));
		write();
		if(!Vars.headless && Vars.ui != null){
			Sounds.uiUnlock.play();
			Vars.ui.hudfrag.showUnlock(node.content);
		}
	}
}
