package newhorizon.expand.map.planet;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Geometry;
import arc.math.geom.Vec2;
import arc.math.geom.Vec3;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.noise.Simplex;
import mindustry.content.Blocks;
import mindustry.content.Loadouts;
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

	public HiemaraPlanet(){
		super("hiemara", NHPlanets.ceito, 1f, 3);

		generator = new HiemaraGenerator();

		meshLoader = () -> new HexMesh(this, 6);
		cloudMeshLoader = () -> new MultiMesh(
				new HexSkyMesh(this, 3, 0.15f, 0.13f, 5, Color.white.cpy().lerp(iceColor, 0.3f).a(0.5f), 2, 0.45f, 1f, 0.38f),
				new HexSkyMesh(this, 5, 0.25f, 0.155f, 4, Color.white.cpy().a(0.38f), 3, 0.5f, 1.2f, 0.42f)
		);

		bloom = false;
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
		/** Radius in tiles of the shallow water kept around the core and around the enemy spawn. */
		public int coreArea = 16, spawnArea = 7;

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
			out.set(deepColor).lerp(shallowColor, Mathf.clamp(1.4f - depth * 1.6f));

			float cold = cold(position);
			if(cold > 0.52f){
				out.lerp(iceColor, Mathf.clamp((cold - 0.52f) * 6f));
			}
		}

		@Override
		public void genTile(Vec3 position, TileGen tile){
			tile.floor = depth(position) > 0.66f ? Blocks.deepwater : Blocks.water;
			tile.block = Blocks.air;
		}

		@Override
		protected void generate(){
			rand.setSeed(seed);
			float difficulty = sector == null ? rand.random(0.4f, 1f) : sector.threat;

			int coreX = width / 2, coreY = height / 2;
			Vec2 trns = Tmp.v1.trns(rand.random(360f), width / 2.8f);
			int spawnX = (int)(-trns.x + width / 2f), spawnY = (int)(-trns.y + height / 2f);

			//deep water gets more likely towards the edges of the map
			float maxDst = Mathf.dst(width / 2f, height / 2f);
			for(Tile tile : tiles){
				float deepness = noise(tile.x + 120, tile.y - 40, 6, 0.55, 80f, 1f) + Mathf.dst(tile.x, tile.y, coreX, coreY) / maxDst * 0.3f;
				tile.setFloor((deepness > 0.7f ? Blocks.deepwater : Blocks.water).asFloor());
				tile.setBlock(Blocks.air);
			}

			shallow(coreX, coreY, coreArea);
			shallow(spawnX, spawnY, spawnArea);

			//ores can only lie in shallow water
			pass((x, y) -> {
				if(floor != Blocks.water)return;

				if(noise(x + 644, y - 538, 5.1, 2f, 125f, 1f) > 0.74f){
					ore = Blocks.oreCopper;
				}
				if(noise(x + 134, y - 134, 5, 4f, 45f, 1f) > 0.775f){
					ore = Blocks.oreLead;
				}
				if(noise(x + 344 + y * 0.35f, y - 538, 5, 6f, 45f, 1f) > 0.84f){
					ore = Blocks.oreCoal;
				}
				if(noise(x + 150, y + x * 2 + 100, 4, 3.8f, 55f, 1f) > 0.82f){
					ore = Blocks.oreTitanium;
				}
			});

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

		@Override
		public void addWeather(Sector sector, Rules rules){
			//cold waves keep coming back: the water freezes during them and thaws in between
			rules.weather.clear();
			Weather.WeatherEntry cold = new Weather.WeatherEntry(NHWeathers.coldWave, 1.5f * Time.toMinutes, 3f * Time.toMinutes, 3f * Time.toMinutes, 6f * Time.toMinutes);
			rules.weather.add(cold);
		}
		
		/** Turns everything in the circle into shallow water. */
		void shallow(int cx, int cy, int radius){
			Geometry.circle(cx, cy, radius, (x, y) -> {
				Tile tile = tiles.get(x, y);
				if(tile != null)tile.setFloor(Blocks.water.asFloor());
			});
		}
	}
}
