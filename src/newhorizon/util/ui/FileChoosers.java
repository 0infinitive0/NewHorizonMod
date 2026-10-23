package newhorizon.util.ui;

import arc.files.Fi;
import arc.func.Cons;
import mindustry.Vars;
import mindustry.ui.FileChooser;

/** Thin wrapper over the file chooser of Mindustry v8, which takes a parameter object instead of a callback and extensions. */
public class FileChoosers{
	/** Asks the player for a file with one of the extensions and passes it to the callback. */
	public static void choose(Cons<Fi> handler, String... extensions){
		FileChooser.FileChooserParams params = new FileChooser.FileChooserParams().open(true).extensions(extensions);
		params.submit(handler);
		Vars.platform.showFileChooser(params);
	}
}
