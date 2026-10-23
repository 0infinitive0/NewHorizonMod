package newhorizon.expand.weather;

import arc.graphics.Color;
import arc.util.Time;
import mindustry.content.StatusEffects;
import mindustry.gen.Sounds;
import mindustry.gen.WeatherState;
import mindustry.type.weather.ParticleWeather;
import mindustry.world.meta.Attribute;

/** Cold weather. The actual freezing of water is done by {@link newhorizon.expand.game.IceManager}. */
public class ColdWave extends ParticleWeather{
	public ColdWave(String name){
		super(name);

		duration = 3f * Time.toMinutes;

		color = noiseColor = Color.valueOf("d5eeff");
		particleRegion = "particle";
		sizeMax = 11f;
		sizeMin = 2.2f;
		density = 900f;
		baseSpeed = 5.5f;
		xspeed = 2f;
		yspeed = -0.4f;

		drawNoise = true;
		noisePath = "fog";
		noiseLayers = 3;
		noiseLayerAlphaM = 0.8f;
		noiseLayerSpeedM = 1.6f;
		noiseLayerSclM = 0.8f;
		noiseScale = 1100f;
		opacityMultiplier = 0.55f;
		useWindVector = true;

		status = StatusEffects.none;
		statusGround = true;
		statusAir = false;

		attrs.set(Attribute.light, -0.2f);

		sound = Sounds.windhowl;
		soundVol = 0f;
		soundVolOscMag = 1.5f;
		soundVolOscScl = 1100f;
		soundVolMin = 0.02f;
	}

	/** @return strength in [0, 1] of the strongest active cold wave, 0 if there is none. */
	public static float strength(){
		float strength = 0f;
		for(WeatherState state : mindustry.gen.Groups.weather){
			if(state.weather() instanceof ColdWave){
				strength = Math.max(strength, state.intensity * state.opacity);
			}
		}
		return strength;
	}
}
