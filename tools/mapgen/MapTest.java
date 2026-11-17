import arc.files.*;
import arc.graphics.*;
import arc.struct.*;
import mindustry.*;
import mindustry.core.GameState.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.maps.Map;

/**
 * Loads each map the way a custom game does, runs it for some minutes with nobody playing, and prints what happened: what the enemy
 * has in the air, how many nukes have gone, and how the player's core is doing. Also writes a preview picture of each map.
 *
 * <p>Usage: {@code java -cp <server-release.jar>:<classes> MapTest <data dir> <maps dir> <minutes> <picture dir> <map names...>}
 */
public class MapTest{
	public static void main(String[] args){
		Harness.boot(args[0], () -> {
			int minutes = Integer.parseInt(args[2]);
			for(int i = 4; i < args.length; i++){
				Fi file = new Fi(args[1]).child(args[i] + ".msav");
				Map map = MapIO.createMap(file, true);
				System.out.println("== " + map.name() + " (" + map.width + "x" + map.height + ")");
				Vars.world.loadMap(map, map.applyRules(Gamemode.attack));
				Vars.state.rules = map.applyRules(Gamemode.attack);
				try{
					Pixmap picture = MapIO.generatePreview(Vars.world.tiles);
					new Fi(args[3]).child(args[i] + ".png").writePng(picture);
				}catch(Throwable t){
					System.out.println("no picture: " + t);
				}
				Vars.logic.play();
				Rules rules = Vars.state.rules;
				System.out.println("attack " + rules.attackMode + ", waves " + rules.waves + ", groups " + rules.spawns.size + ", enemy cheat " + rules.teams.get(Team.crux).cheat
					+ ", build speed " + rules.teams.get(Team.crux).unitBuildSpeedMultiplier + ", loadout " + rules.loadout.size);
				int nukesSeen = 0;
				IntSet seen = new IntSet();
				for(int minute = 1; minute <= minutes; minute++){
					for(int t = 0; t < 60; t++){
						Harness.step(60);
						//With -Dhold=true the player's core cannot be lost, so that the test gets as far as the enemy's late units.
						if(Boolean.getBoolean("hold") && Team.sharded.core() != null) Team.sharded.core().health = 1e9f;
						for(Unit unit : Groups.unit){
							if(unit.team == Team.crux && unit.type.name.endsWith("-active") && seen.add(unit.id)) nukesSeen++;
						}
					}
					ObjectIntMap<String> counts = new ObjectIntMap<>();
					for(Unit unit : Groups.unit) counts.increment((unit.team == Team.crux ? "" : unit.team.name + ":") + unit.type.name.replace("new-horizon-", ""), 0, 1);
					Building core = Team.sharded.core();
					System.out.println("min " + minute + ": wave " + Vars.state.wave + ", nukes launched " + nukesSeen + ", player core " + (core == null ? "gone" : (int)core.health + "/" + (int)core.maxHealth)
						+ ", enemy structures " + Team.crux.data().buildings.size + ", units " + counts + (Vars.state.gameOver ? ", GAME OVER" : ""));
					if(Vars.state.gameOver || !Vars.state.is(State.playing)) break;
				}
			}
		});
	}
}
