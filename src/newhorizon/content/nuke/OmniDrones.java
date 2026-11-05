package newhorizon.content.nuke;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.ai.UnitCommand;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.gen.UnitEntity;
import mindustry.type.UnitType;
import mindustry.world.blocks.units.UnitFactory;
import newhorizon.expand.units.unitType.NHUnitType;

import static mindustry.type.ItemStack.with;

/**
 * The Omni Miner: a mono that can mine omni-matter. It is made in the same block as the mono, the base game's Air Factory, costs a little more,
 * and has the mono's own stats (they are read from the mono, so that it stays the same as the mono if the base game changes it); only its mining
 * differs: it can mine items as hard as omni-matter (hardness 3), and what it mines is copper, lead and omni-matter.
 */
public class OmniDrones{
	public static NHUnitType omniMiner;

	public static void load(){
		UnitType mono = UnitTypes.mono;

		omniMiner = new NHUnitType("omni-miner"){{
			constructor = UnitEntity::create;
			outlineColor = Color.valueOf("1b1b22");
			drawCell = false;

			//Everything of the mono's.
			flying = mono.flying;
			lowAltitude = mono.lowAltitude;
			faceTarget = mono.faceTarget;
			health = mono.health;
			armor = mono.armor;
			speed = mono.speed;
			accel = mono.accel;
			drag = mono.drag;
			hitSize = mono.hitSize;
			rotateSpeed = mono.rotateSpeed;
			itemCapacity = mono.itemCapacity;
			mineSpeed = mono.mineSpeed;
			mineFloor = mono.mineFloor;
			mineHardnessScaling = mono.mineHardnessScaling;
			range = mono.range;
			maxRange = mono.maxRange;
			engineSize = mono.engineSize;
			engineOffset = mono.engineOffset;
			isEnemy = mono.isEnemy;
			allowedInPayloads = mono.allowedInPayloads;
			useUnitCap = mono.useUnitCap;
			payloadCapacity = mono.payloadCapacity;
			envDisabled = mono.envDisabled;
			defaultCommand = UnitCommand.mineCommand;

			//What differs: omni-matter is as hard as titanium, and deposits are solid blocks (the mono's mining speed is kept: a hard item takes longer).
			mineTier = 3;
			mineWalls = true;
			mineItems = Seq.with(Items.copper, Items.lead, NukeItems.omniMatter);
		}};

		//Made where the mono is made, after it in the list of what the factory can make (so that the numbers saved in factories stay right).
		((UnitFactory)Blocks.airFactory).plans.add(new UnitFactory.UnitPlan(omniMiner, 60f * 40f, with(Items.silicon, 40, Items.lead, 25, Items.titanium, 10)));
	}
}
