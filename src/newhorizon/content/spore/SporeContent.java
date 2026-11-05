package newhorizon.content.spore;

import arc.Events;
import arc.struct.IntSet;
import arc.struct.Seq;
import mindustry.ai.UnitCommand;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.effect.MultiEffect;
import mindustry.game.EventType;
import mindustry.gen.Bullet;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.type.StatusEffect;
import mindustry.type.Weapon;
import mindustry.world.blocks.units.UnitFactory;
import newhorizon.expand.units.spore.BlightInfection;
import newhorizon.expand.units.spore.MoteAI;
import newhorizon.expand.units.unitType.NHUnitType;

import static mindustry.type.ItemStack.with;

/**
 * The Spore Mote: a small, fast, flying suicide unit that hunts enemy units (air and ground, never buildings), blows up on them and
 * infects what it hits with Blight. A Blighted unit seeks out the closest uninfected unit of its own team, and when it dies it bursts into
 * more Motes for whoever infected it; it is cured if it is healed back to full health. It is built in the Ground Factory, beside the crawler,
 * and belongs to no faction.
 */
public class SporeContent{
	public static StatusEffect blight;
	public static BulletType moteBlast;
	public static NHUnitType sporeMote;
	public static UnitCommand moteHunt;
	/** Motes that have just gone off by their own blast, which killed them: their death must not set the blast off again. */
	private static final IntSet exploded = new IntSet();

	public static void load(){
		blight = new StatusEffect("blight"){{
			color = SporeFx.amber;
			permanent = true;
			damage = 0.3f;
			speedMultiplier = 0.92f;
			effect = Fx.sporeSlowed;
			effectChance = 0.12f;
		}};

		//The Mote's body: explodes on contact, infects everything caught in the blast and kills its owner.
		moteBlast = new BulletType(){
			{
				speed = 0f;
				rangeOverride = 30f;
				instantDisappear = true;
				killShooter = true;
				hittable = false;
				collides = false;
				collidesTiles = false;
				collidesAir = true;
				collidesGround = true;
				splashDamageRadius = 48f;
				splashDamage = 70f;
				//Deliberately harmless to structures: Motes are an anti-unit swarm.
				buildingDamageMultiplier = 0f;
				status = blight;
				statusDuration = 60f * 10f;
				lightColor = hitColor = SporeFx.amber;
				hitSound = Sounds.explosion;
				hitShake = 1f;
				hitEffect = new MultiEffect(SporeFx.moteBurst, SporeFx.moteRing);
				despawnEffect = Fx.none;
			}

			//An instantly vanishing bullet does its splash in despawned(), without going through hit(): record the infection there (and in hit() too, which does no harm).
			@Override
			public void despawned(Bullet b){
				//Its own blast kills the Mote that fired it: that death is not a second explosion.
				if(b.owner instanceof Unit && ((Unit)b.owner).type == sporeMote) exploded.add(((Unit)b.owner).id);
				super.despawned(b);
				BlightInfection.infectArea(b.team, b.x, b.y, splashDamageRadius);
			}

			@Override
			public void hit(Bullet b, float x, float y){
				super.hit(b, x, y);
				BlightInfection.infectArea(b.team, x, y, splashDamageRadius);
			}
		};

		//Like the Sporeling's Hunt, but flying (MoteAI); a click only guides it while it is idle.
		moteHunt = new UnitCommand("nh-mote-hunt", "units", u -> new MoteAI());
		moteHunt.switchToMove = false;

		sporeMote = new NHUnitType("spore-mote"){{
			constructor = UnitEntity::create;
			aiController = MoteAI::new;
			commands = Seq.with(moteHunt, UnitCommand.moveCommand);
			defaultCommand = moteHunt;
			outlineColor = SporeFx.amberDark.cpy().mul(0.25f);
			drawCell = false;

			flying = true;
			lowAltitude = true;
			health = 90f;
			speed = 3.4f;
			accel = 0.1f;
			drag = 0.04f;
			rotateSpeed = 12f;
			hitSize = 5f;
			range = 36f;
			targetAir = true;
			targetGround = true;
			//Motes burst out in swarms (one per 100 max health of the host), which the normal unit cap would cull.
			useUnitCap = false;
			engineOffset = 3.5f;
			engineSize = 1.5f;
			engineColor = SporeFx.amber;

			weapons.add(new Weapon(){{
				//(No shootOnDeath: a flying wreck would only go off after it landed; the death is handled at once, see below.)
				reload = 24f;
				shootCone = 180f;
				ejectEffect = Fx.none;
				shootSound = Sounds.explosion;
				x = shootY = 0f;
				mirror = false;
				bullet = moteBlast;
			}});
		}};

		//Built in the Ground Factory, beside the crawler.
		((UnitFactory)Blocks.groundFactory).plans.add(new UnitFactory.UnitPlan(sporeMote, 60f * 11f, with(Items.silicon, 14, Items.coal, 10)));

		BlightInfection.load();

		//A Mote that dies any other way (shot down, crushed...) explodes where it is, the moment it dies, and spreads Blight all the same. A
		//flying unit's wreck only "is destroyed" (the destroy event, the death weapons) after it has fallen, so the dead Mote is caught here.
		Events.run(EventType.Trigger.update, () -> {
			if(mindustry.Vars.net.client()) return;
			for(int i = 0; i < mindustry.gen.Groups.unit.size(); i++){
				Unit u = mindustry.gen.Groups.unit.index(i);
				if(u.type == sporeMote && u.dead && exploded.add(u.id)){
					moteBlast.create(null, u.team, u.x, u.y, 0f);
				}
			}
		});
		Events.on(EventType.UnitDestroyEvent.class, e -> {
			if(e.unit != null && e.unit.type == sporeMote) exploded.remove(e.unit.id);
		});
		Events.on(EventType.ResetEvent.class, e -> exploded.clear());
	}

	/**
	 * Whether the unit carries Blight. {@code hasEffect} alone is not enough: it reads a bitset that is only refreshed on the unit's next
	 * update, so it is still false in the frame the status was applied (e.g. by the explosion).
	 */
	public static boolean carriesBlight(Unit unit){
		return unit.hasEffect(blight) || unit.getDuration(blight) > 0f;
	}
}
