package newhorizon.expand.map.planet;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Geometry;
import arc.math.geom.Vec2;
import arc.math.geom.Vec3;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.noise.Simplex;
import arc.struct.Seq;
import arc.util.Structs;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import newhorizon.content.hiemara.HiemaraContent;
import newhorizon.content.hiemara.HiemaraFloors;
import newhorizon.expand.block.env.RichWater;
import mindustry.content.Loadouts;
import mindustry.game.EventType;
import mindustry.game.Rules;
import mindustry.game.Schematics;
import mindustry.game.Team;
import mindustry.graphics.g3d.HexMesh;
import mindustry.graphics.g3d.HexSkyMesh;
import mindustry.graphics.g3d.MultiMesh;
import mindustry.maps.generators.PlanetGenerator;
import mindustry.type.Sector;
import mindustry.type.Weather;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.TileGen;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.meta.Env;
import newhorizon.content.NHOverride;
import newhorizon.content.NHPlanets;
import newhorizon.content.NHWeathers;
import newhorizon.expand.weather.ColdWave;

import static mindustry.Vars.state;

/**
 * A world that is nothing but water. There is no land, only shallow water that can be built on and deep water that can not,
 * until a cold wave freezes it. The sector generator and the rules are the only content the planet has for now.
 */
public class HiemaraPlanet extends mindustry.type.Planet{
	public static final Color
			deepColor = Color.valueOf("1d4f95"),
			shallowColor = Color.valueOf("4a9fdc"),
			iceColor = Color.valueOf("e3f4ff");

	/** No cold wave this long after a sector starts or is loaded, in ticks. */
	public static float coldWaveGrace = 10f * Time.toMinutes;

	/**
	 * The game sets every weather cooldown to a random value when play starts, which is 3 minutes at most for the cold
	 * wave. This runs right after that and sets the first wave to the grace time plus the usual wait.
	 */
	public static void registerGrace(){
		Events.on(EventType.PlayEvent.class, e -> Core.app.post(() -> {
			if(state.rules.sector == null || state.rules.sector.planet != NHPlanets.hiemara)return;
			for(Weather.WeatherEntry entry : state.rules.weather){
				if(entry.weather instanceof ColdWave)entry.cooldown = coldWaveGrace + Mathf.random(entry.minFrequency, entry.maxFrequency);
			}
		}));
	}

	public HiemaraPlanet(){
		super("hiemara", NHPlanets.ceito, 1f, 3);

		generator = new HiemaraGenerator();

		meshLoader = () -> new HexMesh(this, 6);
		cloudMeshLoader = () -> new MultiMesh(
				new HexSkyMesh(this, 3, 0.15f, 0.13f, 5, Color.white.cpy().lerp(iceColor, 0.3f).a(0.5f), 2, 0.45f, 1f, 0.38f),
				new HexSkyMesh(this, 5, 0.25f, 0.155f, 4, Color.white.cpy().a(0.38f), 3, 0.5f, 1.2f, 0.42f)
		);

		bloom = false;
		//A bright sea: the day and night of a sector would leave solar panels with a third of their power.
		updateLighting = false;
		visible = true;
		accessible = true;
		alwaysUnlocked = true;
		hasAtmosphere = true;
		clearSectorOnLose = true;
		allowLaunchSchematics = false;
		allowLaunchLoadout = false;

		iconColor = shallowColor.cpy().lerp(Color.white, 0.4f);
		landCloudColor = atmosphereColor = Color.valueOf("8fc9f2");
		atmosphereRadIn = 0.02f;
		atmosphereRadOut = 0.3f;

		defaultEnv = Env.terrestrial | Env.groundWater | Env.oxygen;
		startSector = 10;

		ruleSetter = r -> {
			//Only what Hiemara has is built here.
			r.hideBannedBlocks = true;
			r.blockWhitelist = true;
			r.bannedBlocks.addAll(HiemaraContent.buildable());
			r.loadout = ItemStack.list(Items.copper, 500, Items.lead, 400, Items.sand, 200, Items.scrap, 100);
			r.waveTeam = Team.malis;
			r.placeRangeCheck = false;
			r.hideSpawns = false;
			r.waveSpacing = 80 * Time.toSeconds;
			r.initialWaveSpacing = 8f * Time.toMinutes;
			r.coreDestroyClear = true;
			r.dropZoneRadius = 64;
		};
	}

	public static class HiemaraGenerator extends PlanetGenerator{
		/** Share of the map that is shallow water. */
		public float shallowShare = 0.2f;
		/** Smallest all-shallow square, in tiles, that the core is put on, and that the enemy spawn needs. */
		public int coreSquare = 7, spawnSquare = 5;

		{
			baseSeed = 11;
		}

		@Override
		public boolean allowLanding(Sector sector){
			return sector.hasBase() || sector.near().contains(s -> s.hasBase() && s.isCaptured());
		}

		@Override
		public void generateSector(Sector sector){
			//no bases right now
		}

		@Override
		public float getSizeScl(){
			return 2200f;
		}

		/** Roughly [0, 1], where large values are deep water. */
		float depth(Vec3 position){
			return Simplex.noise3d(seed, 6, 0.55f, 1.6f, 10f + position.x, 10f + position.y, 10f + position.z);
		}

		/** Roughly [0, 1], where large values are ice. Polar caps are the most frozen. */
		float cold(Vec3 position){
			float noise = Simplex.noise3d(seed + 3, 5, 0.6f, 2.2f, 10f + position.x, 10f + position.y, 10f + position.z);
			return noise * 0.55f + Mathf.pow(Math.abs(position.y), 3f) * 0.7f;
		}

		@Override
		public float getHeight(Vec3 position){
			//a smooth ball, ice stands out a little
			return Mathf.clamp(cold(position) - 0.55f, 0f, 1f) * 0.05f;
		}

		@Override
		public void getColor(Vec3 position, Color out){
			float depth = depth(position);
			out.set(deepColor).lerp(shallowColor, Mathf.clamp((0.4f - depth) * 8f));

			float cold = cold(position);
			if(cold > 0.52f){
				out.lerp(iceColor, Mathf.clamp((cold - 0.52f) * 6f));
			}
		}

		@Override
		public void genTile(Vec3 position, TileGen tile){
			tile.floor = depth(position) < 0.4f ? Blocks.water : Blocks.deepwater;
			tile.block = Blocks.air;
		}

		@Override
		protected void generate(){
			rand.setSeed(seed);
			float difficulty = sector == null ? rand.random(0.4f, 1f) : sector.threat;
			
			//The land is a noise field, and the lowest fifth of it is shallow water. Using the exact fifth of the values
			//and not a fixed threshold is what keeps the ratio the same on every map.
			float[] field = new float[width * height];
			for(int x = 0; x < width; x++){
				for(int y = 0; y < height; y++){
					field[x + y * width] = noise(x + 120, y - 40, 4, 0.55, 48f, 1f);
				}
			}
			float[] sorted = field.clone();
			java.util.Arrays.sort(sorted);
			float threshold = sorted[(int)(sorted.length * shallowShare)];
			
			boolean[] shallow = new boolean[width * height];
			for(int i = 0; i < shallow.length; i++){
				shallow[i] = field[i] < threshold;
			}
			
			//The core goes on the shallow patch with the biggest all-shallow square, the nearest to the middle.
			int[] square = squares(shallow);
			int coreX = -1, coreY = -1;
			float bestScore = -1f;
			for(int x = 0; x < width; x++){
				for(int y = 0; y < height; y++){
					int size = square[x + y * width];
					if(size < coreSquare)continue;
					//The square ends at (x, y), so its middle is half its size back.
					int mx = x - size / 2, my = y - size / 2;
					float score = Math.min(size, coreSquare + 4) * 100f - Mathf.dst(mx, my, width / 2f, height / 2f);
					if(score > bestScore){
						bestScore = score;
						coreX = mx;
						coreY = my;
					}
				}
			}
			if(coreX < 0){
				//No patch is big enough, so make one in the middle.
				coreX = width / 2;
				coreY = height / 2;
				fill(shallow, coreX, coreY, coreSquare);
			}
			
			//The enemy comes from the shallow water that is the furthest away from the core.
			int spawnX = -1, spawnY = -1;
			float far = -1f;
			for(int x = 0; x < width; x++){
				for(int y = 0; y < height; y++){
					if(square[x + y * width] < spawnSquare)continue;
					float dst = Mathf.dst(x, y, coreX, coreY);
					if(dst > far){
						far = dst;
						spawnX = x;
						spawnY = y;
					}
				}
			}
			if(spawnX < 0){
				spawnX = Mathf.clamp(width - coreX, 8, width - 8);
				spawnY = Mathf.clamp(height - coreY, 8, height - 8);
				fill(shallow, spawnX, spawnY, spawnSquare);
			}
			
			for(Tile tile : tiles){
				tile.setFloor((shallow[tile.x + tile.y * width] ? Blocks.water : Blocks.deepwater).asFloor());
				tile.setBlock(Blocks.air);
			}
			
			richWater(coreX, coreY);
			
			tiles.getn(spawnX, spawnY).setOverlay(Blocks.spawn);
			
			Schematics.placeLoadout(Loadouts.basicShard, coreX, coreY);
			
			state.rules.env = sector.planet.defaultEnv;
			state.rules.waves = true;
			state.rules.hideSpawns = false;
			state.rules.onlyDepositCore = false;
			state.rules.fog = false;
			
			if(state.rules.sector.preset != null)return;
			
			state.rules.winWave = Mathf.round(60 + 60 * difficulty, 5);
			
			state.rules.spawns = NHOverride.generate(difficulty, new Rand(sector.id), false, false, true);
		}
		
		/** For every tile, the size of the biggest all-shallow square that has the tile as its corner. */
		int[] squares(boolean[] shallow){
			int[] out = new int[shallow.length];
			for(int x = 0; x < width; x++){
				for(int y = 0; y < height; y++){
					int i = x + y * width;
					if(!shallow[i])continue;
					if(x == 0 || y == 0){
						out[i] = 1;
					}else{
						out[i] = 1 + Math.min(out[i - 1], Math.min(out[i - width], out[i - width - 1]));
					}
				}
			}
			return out;
		}
		
		/** Makes a square of shallow water. */
		void fill(boolean[] shallow, int cx, int cy, int size){
			for(int x = cx - size / 2 - 1; x <= cx + size / 2 + 1; x++){
				for(int y = cy - size / 2 - 1; y <= cy + size / 2 + 1; y++){
					if(Structs.inBounds(x, y, width, height))shallow[x + y * width] = true;
				}
			}
		}
		
		/** Scatters blobs of rich water. Common ores lie close to the core, the rare ones far away. */
		void richWater(int coreX, int coreY){
			Seq<Item> ores = HiemaraFloors.richOres;
			for(int i = 0; i < ores.size; i++){
				Item ore = ores.get(i);
				RichWater rich = RichWater.byItem.get(ore);
				if(rich == null)continue;
				
				//The first ores (scrap, copper, lead, coal) are near the core, the last (thorium, tungsten) far.
				float rank = i / (float)(ores.size - 1);
				float min = 14f + rank * 60f, max = 40f + rank * 120f;
				int blobs = i < 4 ? rand.random(4, 6) : rand.random(3, 4);
				
				for(int n = 0; n < blobs; n++){
					for(int tries = 0; tries < 80; tries++){
						int x = rand.random(4, width - 5), y = rand.random(4, height - 5);
						float dst = Mathf.dst(x, y, coreX, coreY);
						if(dst < min || dst > max)continue;
						
						int radius = rand.random(2, 4);
						if(!deepAround(x, y, radius + 1))continue;
						
						Geometry.circle(x, y, radius, (px, py) -> {
							Tile tile = tiles.get(px, py);
							if(tile != null && tile.floor() == Blocks.deepwater)tile.setFloor(rich);
						});
						break;
					}
				}
			}
		}
		
		boolean deepAround(int cx, int cy, int radius){
			for(int x = cx - radius; x <= cx + radius; x++){
				for(int y = cy - radius; y <= cy + radius; y++){
					Tile tile = tiles.get(x, y);
					if(tile == null || tile.floor() != Blocks.deepwater)return false;
				}
			}
			return true;
		}
		
		@Override
		public void addWeather(Sector sector, Rules rules){
			//cold waves keep coming back: the water freezes during them and thaws in between
			rules.weather.clear();
			Weather.WeatherEntry cold = new Weather.WeatherEntry(NHWeathers.coldWave, 1.5f * Time.toMinutes, 3f * Time.toMinutes, 3f * Time.toMinutes, 6f * Time.toMinutes);
			rules.weather.add(cold);
		}
		
	}
}
