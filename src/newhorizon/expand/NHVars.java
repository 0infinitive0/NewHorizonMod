package newhorizon.expand;

import arc.Core;
import mindustry.Vars;
import newhorizon.*;
import newhorizon.expand.block.synth.SynthUpdater;
import newhorizon.expand.cutscene.NHCSS_Core;
import newhorizon.expand.cutscene.NHCSS_UI;
import newhorizon.expand.game.NHWorldData;
import newhorizon.util.func.GridUtil;
import newhorizon.util.ui.TableFunc;

public class NHVars{
	public static NHWorldData worldData;
	public static NHCSS_Core cutscene;
	public static NHRenderer renderer;
	public static NHModCore core;
	public static NHInputListener listener;
	public static NHInputControl control;


	public static void init(){
		worldData = new NHWorldData();
		listener = new NHInputListener();
		//It is a desktop input handler, which a server has no player for.
		if(!Vars.headless)control = new NHInputControl();

		core = new NHModCore();

		Core.app.addListener(core);
		NHCSS_UI.init();

		if(Vars.headless)return;
		GridUtil.init();
		renderer = new NHRenderer();
		NHSetting.loadUI();
		if(NHSetting.getBool(NHSetting.DEBUG_PANEL)){
			TableFunc.tableMain();
		}
	}
}
