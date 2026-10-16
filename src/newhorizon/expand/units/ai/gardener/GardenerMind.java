package newhorizon.expand.units.ai.gardener;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.math.geom.Position;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.struct.IntFloatMap;
import arc.struct.ObjectFloatMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.game.Teams;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.world.Block;
import mindustry.world.Tile;
import newhorizon.content.jade.JadeFx;
import newhorizon.content.jade.JadeUnits;

import static mindustry.Vars.tilesize;

/**
 * What the Jade Gardeners have learned. One shared "hive memory" for every Gardener, split in two parts:
 *
 * <p><b>Long-term knowledge</b>, saved with the game settings so it survives restarts (a fresh install knows nothing):
 * <ul>
 *     <li>{@code xp}, which decides the {@link #stage()} and therefore which behaviours the AI may use;</li>
 *     <li>a value per block type, learned from whether Gardener-built blocks survive or are destroyed soon after;</li>
 *     <li>{@code risk}, how cautious the Gardeners are: it grows when one dies and relaxes while they stay safe.</li>
 * </ul>
 * <p><b>Short-term memory</b>, forgotten when a world is (re)loaded: a decaying map of where it is dangerous, per team.
 *
 * <p>Everything is learned from stage 0 on, the stage only gates what the AI does with it. This is a statistical
 * adaptive AI, not a neural network: every number can be read with {@link #report}.
 */
public class GardenerMind{
	public static final GardenerMind instance = new GardenerMind();

	public static final String SETTING = "nh-gardener-mind";
	public static final String[] stageNames = {"Seedling", "Sprout", "Sapling", "Elder"};
	/** XP needed for each stage. Stage 0 is what a fresh install starts with. */
	public static final float[] stageXp = {0f, 60f, 300f, 900f};
	public static final int maxStage = stageXp.length - 1;

	/** Danger is stored per square chunk of this many tiles. */
	public static final float chunkSize = 6f * tilesize;
	/** Danger halves every this many seconds. */
	public static final float dangerHalfLife = 180f;
	/** Danger at which a stage 1 Gardener leaves. */
	public static final float baseThreshold = 3f;
	/** A built block that stays up this long counts as a success, one destroyed within it as a failure. */
	public static final float survivalSeconds = 90f;
	public static final float minRisk = 0.5f, maxRisk = 2.5f;

	private static final int maxTracked = 256;
	private static final float learnRate = 0.15f;

	//long-term, saved
	public float xp;
	public float risk = 1f;
	public int deaths, built, survived;
	public final ObjectFloatMap<String> blockValue = new ObjectFloatMap<>();

	//short-term, per world
	private final IntFloatMap[] danger = new IntFloatMap[Team.all.length];
	private final Seq<Built> tracked = new Seq<>();
	private float stepTimer, saveTimer;
	private boolean dirty, hooked;

	private static class Built{
		final int pos;
		final short block;
		final Team team;
		final float time;

		Built(int pos, short block, Team team, float time){
			this.pos = pos;
			this.block = block;
			this.team = team;
			this.time = time;
		}
	}

	/** Registers the event hooks and loads the saved knowledge. Safe to call more than once. */
	public void init(){
		if(hooked) return;
		hooked = true;
		load();

		Events.on(EventType.BlockBuildEndEvent.class, e -> {
			if(Vars.net.client() || e.breaking || e.unit == null || e.unit.type != JadeUnits.gardener) return;
			blockBuilt(e.tile, e.team);
		});
		Events.on(EventType.BlockDestroyEvent.class, e -> {
			if(!Vars.net.client()) blockDestroyed(e.tile);
		});
		Events.on(EventType.UnitDestroyEvent.class, e -> {
			if(!Vars.net.client() && e.unit.type == JadeUnits.gardener) gardenerDied(e.unit);
		});
		Events.on(EventType.WorldLoadEvent.class, e -> forgetWorld());
		Events.on(EventType.ResetEvent.class, e -> forgetWorld());
		Events.run(EventType.Trigger.update, () -> {
			stepTimer += Time.delta;
			if(stepTimer >= 60f){
				step(stepTimer / 60f);
				stepTimer = 0f;
			}
		});
	}

	//---------------------------------------------------------------- stages

	public int stage(){
		return stageOf(xp);
	}

	public static int stageOf(float xp){
		int stage = 0;
		for(int i = 1; i < stageXp.length; i++){
			if(xp >= stageXp[i]) stage = i;
		}
		return stage;
	}

	public String stageName(){
		return stageNames[stage()];
	}

	public void addXp(float amount){
		if(amount <= 0f) return;
		int before = stage();
		xp += amount;
		dirty = true;
		int after = stage();
		if(after > before) stageUp(after);
	}

	protected void stageUp(int stage){
		Log.info("[Jade Gardener] evolved to stage @ (@)", stage, stageNames[stage]);
		save();
		if(Vars.headless) return;
		Groups.unit.each(u -> u.type == JadeUnits.gardener, u -> JadeFx.bloomRingBig.at(u.x, u.y));
		if(Vars.ui != null && Vars.ui.hudfrag != null){
			Vars.ui.hudfrag.showToast("[accent]The Gardeners evolved:[] [#3fbf8f]" + stageNames[stage]);
		}
	}

	//---------------------------------------------------------------- danger (short-term)

	private IntFloatMap dangerOf(Team team){
		IntFloatMap map = danger[team.id];
		if(map == null) map = danger[team.id] = new IntFloatMap();
		return map;
	}

	private static int chunk(float world){
		return Math.max(0, (int)(world / chunkSize));
	}

	/** Remembers that it is dangerous around here. Part of it leaks into the neighbouring chunks. */
	public void reinforce(Team team, float x, float y, float amount){
		if(amount <= 0f) return;
		IntFloatMap map = dangerOf(team);
		int cx = chunk(x), cy = chunk(y);
		for(int dx = -1; dx <= 1; dx++){
			for(int dy = -1; dy <= 1; dy++){
				int nx = cx + dx, ny = cy + dy;
				if(nx < 0 || ny < 0) continue;
				int key = Point2.pack(nx, ny);
				map.put(key, map.get(key, 0f) + (dx == 0 && dy == 0 ? amount : amount * 0.35f));
			}
		}
	}

	public float danger(Team team, float x, float y){
		return dangerOf(team).get(Point2.pack(chunk(x), chunk(y)), 0f);
	}

	/** Danger fades with time: old fights stop mattering. */
	public void decay(float seconds){
		float factor = Mathf.pow(0.5f, seconds / dangerHalfLife);
		for(IntFloatMap map : danger){
			if(map == null || map.isEmpty()) continue;
			IntFloatMap.Keys keys = map.keys();
			arc.struct.IntSeq remove = new arc.struct.IntSeq();
			while(keys.hasNext){
				int key = keys.next();
				float v = map.get(key, 0f) * factor;
				if(v < 0.02f) remove.add(key);
				else map.put(key, v);
			}
			for(int i = 0; i < remove.size; i++) map.remove(remove.get(i), 0f);
		}
	}

	/**
	 * Centre of the calmest chunk within {@code radius} chunks, preferring close ones and, if given, ones far from {@code threat}.
	 * Writes it to {@code out} and returns the danger found there.
	 */
	public float safestNear(Team team, float x, float y, int radius, Position threat, Vec2 out){
		IntFloatMap map = dangerOf(team);
		int cx = chunk(x), cy = chunk(y);
		int maxX = Math.max(0, (int)(Vars.world.width() * tilesize / chunkSize) - 1);
		int maxY = Math.max(0, (int)(Vars.world.height() * tilesize / chunkSize) - 1);
		float best = Float.MAX_VALUE, bestDanger = 0f;
		float half = chunkSize / 2f;

		for(int dx = -radius; dx <= radius; dx++){
			for(int dy = -radius; dy <= radius; dy++){
				int nx = cx + dx, ny = cy + dy;
				if(nx < 0 || ny < 0 || nx > maxX || ny > maxY) continue;

				float d = map.get(Point2.pack(nx, ny), 0f);
				float score = d + Mathf.len(dx, dy) * 0.35f;
				if(threat != null){
					float dist = Mathf.dst(nx * chunkSize + half, ny * chunkSize + half, threat.getX(), threat.getY());
					score += Math.max(0f, 1f - dist / (chunkSize * 5f)) * 4f;
				}
				if(score < best){
					best = score;
					bestDanger = d;
					out.set(nx * chunkSize + half, ny * chunkSize + half);
				}
			}
		}
		return bestDanger;
	}

	//---------------------------------------------------------------- caution

	/** More cautious Gardeners leave sooner. Elders anticipate and leave earlier still. */
	public float fleeThreshold(int stage){
		return baseThreshold / risk * (stage >= 3 ? 0.6f : 1f);
	}

	/** How far away an enemy makes a Gardener leave. */
	public float senseRange(int stage){
		return (150f + (stage >= 3 ? 40f : 0f)) * Mathf.clamp(risk, 0.7f, 1.8f);
	}

	public void gardenerDied(Unit unit){
		deaths++;
		risk = Math.min(maxRisk, risk * 1.2f);
		reinforce(unit.team, unit.x, unit.y, 12f);
		addXp(25f);
		dirty = true;
	}

	/** Called by the AI after a stretch without being hurt. */
	public void calmPeriod(){
		risk = Math.max(minRisk, risk * 0.97f);
		dirty = true;
	}

	//---------------------------------------------------------------- building outcomes

	public float value(Block block){
		return blockValue.get(block.name, 0f);
	}

	/** Moves the learned value of a block type towards +1 (it survived) or -1 (it was destroyed soon after). */
	public void learnOutcome(Block block, boolean survived){
		float v = blockValue.get(block.name, 0f);
		blockValue.put(block.name, Mathf.clamp(v + ((survived ? 1f : -1f) - v) * learnRate, -1f, 1f));
		dirty = true;
	}

	/** What the Gardeners believe a type of block is worth rebuilding, before location and distance. */
	public static float prior(Block block){
		switch(block.category){
			case turret: return 3f;
			case power: return 2.2f;
			case production: case crafting: return 1.8f;
			case units: case effect: return 1.5f;
			case distribution: return 1.2f;
			default: return 1f;
		}
	}

	/** Higher is more worth rebuilding now, from the point of view of a Gardener standing at (x, y). */
	public float planScore(Team team, Teams.BlockPlan plan, float x, float y){
		Block block = Vars.content.block(plan.block);
		if(block == null) return -1000f;

		float wx = plan.x * tilesize, wy = plan.y * tilesize;
		float distTiles = Mathf.dst(x, y, wx, wy) / tilesize;
		return prior(block) + 2f * value(block) - danger(team, wx, wy) / baseThreshold * 2.5f - 0.03f * distTiles;
	}

	private void blockBuilt(Tile tile, Team team){
		if(tile == null || tile.block() == null) return;
		tracked.add(new Built(tile.pos(), tile.block().id, team, Time.time));
		if(tracked.size > maxTracked) tracked.remove(0);
		built++;
		addXp(1f);
	}

	private void blockDestroyed(Tile tile){
		if(tile == null || tile.team() == Team.derelict) return;
		Block block = tile.block();
		reinforce(tile.team(), tile.worldx(), tile.worldy(), 1.5f);

		for(int i = tracked.size - 1; i >= 0; i--){
			Built b = tracked.get(i);
			if(b.pos == tile.pos() && b.block == block.id && Time.time - b.time < survivalSeconds * 60f){
				learnOutcome(block, false);
				tracked.remove(i);
				return;
			}
		}
	}

	//---------------------------------------------------------------- upkeep

	/** Called about once a second: decay, success checks, saving. */
	public void step(float seconds){
		decay(seconds);

		for(int i = tracked.size - 1; i >= 0; i--){
			Built b = tracked.get(i);
			if(Time.time - b.time < survivalSeconds * 60f) continue;

			Tile tile = Vars.world.tile(b.pos);
			if(tile != null && tile.build != null && tile.block().id == b.block && tile.team() == b.team){
				learnOutcome(tile.block(), true);
				survived++;
				addXp(3f);
			}
			tracked.remove(i);
		}

		saveTimer += seconds;
		if(dirty && saveTimer >= 30f) save();
	}

	public void forgetWorld(){
		for(int i = 0; i < danger.length; i++) danger[i] = null;
		tracked.clear();
	}

	//---------------------------------------------------------------- persistence

	public String serialize(){
		StringBuilder values = new StringBuilder();
		for(ObjectFloatMap.Entry<String> e : blockValue){
			if(values.length() > 0) values.append(',');
			values.append(e.key).append(':').append(Strings.fixed(e.value, 3));
		}
		return "v1;xp=" + Strings.fixed(xp, 2) + ";risk=" + Strings.fixed(risk, 3) + ";deaths=" + deaths + ";built=" + built
			+ ";survived=" + survived + ";values=" + values;
	}

	/** Never throws: anything unreadable is ignored and the Gardeners simply know less. */
	public void deserialize(String text){
		reset(false);
		if(text == null || !text.startsWith("v1;")) return;
		for(String part : text.split(";")){
			int eq = part.indexOf('=');
			if(eq < 0) continue;
			String key = part.substring(0, eq), val = part.substring(eq + 1);
			try{
				switch(key){
					case "xp": xp = Math.max(0f, Float.parseFloat(val)); break;
					case "risk": risk = Mathf.clamp(Float.parseFloat(val), minRisk, maxRisk); break;
					case "deaths": deaths = Math.max(0, Integer.parseInt(val)); break;
					case "built": built = Math.max(0, Integer.parseInt(val)); break;
					case "survived": survived = Math.max(0, Integer.parseInt(val)); break;
					case "values":
						for(String entry : val.split(",")){
							int colon = entry.lastIndexOf(':');
							if(colon > 0) blockValue.put(entry.substring(0, colon), Mathf.clamp(Float.parseFloat(entry.substring(colon + 1)), -1f, 1f));
						}
						break;
					default: break;
				}
			}catch(NumberFormatException ignored){
			}
		}
	}

	public void save(){
		saveTimer = 0f;
		dirty = false;
		if(Core.settings != null) Core.settings.put(SETTING, serialize());
	}

	public void load(){
		deserialize(Core.settings == null ? null : Core.settings.getString(SETTING, null));
	}

	/** Wipes what has been learned. {@code persist} also overwrites the saved copy. */
	public void reset(boolean persist){
		xp = 0f;
		risk = 1f;
		deaths = built = survived = 0;
		blockValue.clear();
		forgetWorld();
		dirty = false;
		if(persist) save();
	}

	public String report(){
		StringBuilder sb = new StringBuilder();
		int stage = stage();
		sb.append("[#3fbf8f]Jade Gardeners[]: stage ").append(stage).append(" [accent]").append(stageNames[stage]).append("[]\n");
		sb.append("Experience: ").append(Strings.fixed(xp, 0));
		if(stage < maxStage) sb.append(" / ").append(Strings.fixed(stageXp[stage + 1], 0)).append(" for ").append(stageNames[stage + 1]);
		sb.append("\nBuilt ").append(built).append(" blocks, ").append(survived).append(" survived; lost ").append(deaths).append(" Gardeners");
		sb.append("\nCaution: ").append(Strings.fixed(risk, 2)).append("x");
		if(blockValue.size > 0){
			sb.append("\nLearned block values (-1 dies fast .. +1 survives):");
			for(ObjectFloatMap.Entry<String> e : blockValue){
				sb.append("\n  ").append(e.key).append(": ").append(Strings.fixed(e.value, 2));
			}
		}
		return sb.toString();
	}
}
