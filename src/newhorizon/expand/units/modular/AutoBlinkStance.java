package newhorizon.expand.units.modular;

import arc.Core;
import mindustry.ai.UnitStance;

/** The Auto blink button of a modular frame: while it is on, a frame under orders uses its short teleport drive by itself (see {@link TeleportAbility}). */
public class AutoBlinkStance extends UnitStance{
	public AutoBlinkStance(){
		super("nh-auto-blink", "warning", null);
	}

	@Override
	public String localized(){
		return Core.bundle.get("stance.nh-auto-blink", "Auto blink");
	}
}
