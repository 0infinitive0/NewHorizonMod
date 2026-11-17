import arc.*;
import arc.backend.headless.*;
import arc.files.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;

/** Boots a headless game with the mod loaded and hands over to a task; the game is stepped by hand so that tests run faster than real time. */
public class Harness{
	public interface Task{
		void run() throws Exception;
	}

	public static void boot(String dataDir, Task task){
		new HeadlessApplication(new ApplicationListener(){
			@Override
			public void init(){
				try{
					Core.settings.setDataDirectory(new Fi(dataDir));
					Vars.loadLocales = false;
					Vars.headless = true;
					Vars.loadSettings();
					Vars.init();
					mindustry.ui.Fonts.loadContentIconsHeadless();
					Vars.content.createBaseContent();
					Vars.mods.loadScripts();
					Vars.content.createModContent();
					Vars.content.init();
					if(Vars.mods.hasContentErrors()) throw new RuntimeException("mod content errors");
					Vars.bases.load();
					Core.app.addListener(Vars.logic = new Logic());
					Core.app.addListener(Vars.netServer = new NetServer());
					Vars.mods.eachClass(Mod::init);
					Events.fire(new ServerLoadEvent());
					Time.setDeltaProvider(() -> 1f);
					task.run();
				}catch(Throwable t){
					t.printStackTrace();
					System.exit(1);
				}
				System.exit(0);
			}
		}, 1f / 60f, t -> {
			t.printStackTrace();
			System.exit(1);
		});
	}

	/** Runs the game for this many ticks. */
	public static void step(int ticks){
		for(int i = 0; i < ticks; i++){
			Time.update();
			Vars.asyncCore.begin();
			Vars.logic.update();
			Vars.asyncCore.end();
		}
	}
}
