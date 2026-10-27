package newhorizon.expand.units.serpent;

import mindustry.gen.Teamc;

/** What a Serpent head's AI is locked on to, if anything: the target it is going round (a valid one, of another team). */
public interface SerpentLock{
	Teamc lockedTarget();
}
