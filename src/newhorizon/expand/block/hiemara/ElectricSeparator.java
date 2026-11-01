package newhorizon.expand.block.hiemara;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.geom.Geometry;
import mindustry.graphics.Layer;
import mindustry.type.LiquidStack;
import mindustry.world.blocks.production.GenericCrafter;

import static mindustry.Vars.tilesize;

/**
 * Splits water into two gases. Each gas leaves on a side of its own, fixed relative to the rotation, and the sides are
 * marked with a patch in the colour of the gas. The two other sides are free for the water and the power.
 */
public class ElectricSeparator extends GenericCrafter{
	public ElectricSeparator(String name){
		super(name);
		
		rotate = true;
		//the sprite stays as it is, the patches show where the gases go
		rotateDraw = false;
		//the first output goes to the left of the rotation, the second to its right
		liquidOutputDirections = new int[]{1, 3};
	}
	
	/** Draws the patches of the outputs on the sides of the block. */
	protected void drawOutputs(float x, float y, int rotation){
		if(outputLiquids == null)return;
		
		for(int i = 0; i < outputLiquids.length; i++){
			LiquidStack output = outputLiquids[i];
			int dir = Math.floorMod(liquidOutputDirections[i] + rotation, 4);
			float edge = size * tilesize / 2f;
			float px = x + Geometry.d4x(dir) * edge, py = y + Geometry.d4y(dir) * edge;
			float w = dir % 2 == 0 ? 2f : size * tilesize * 0.4f, h = dir % 2 == 0 ? size * tilesize * 0.4f : 2f;
			Draw.z(Layer.blockOver);
			Draw.color(output.liquid.color);
			Fill.rect(px, py, w + 1f, h + 1f);
			Draw.color(output.liquid.color.cpy().mul(0.6f));
			Fill.rect(px - Geometry.d4x(dir), py - Geometry.d4y(dir), w, h);
			Draw.reset();
		}
	}
	
	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		super.drawPlace(x, y, rotation, valid);
		drawOutputs(x * tilesize + offset, y * tilesize + offset, rotation);
	}
	
	public class ElectricSeparatorBuild extends GenericCrafterBuild{
		@Override
		public void draw(){
			super.draw();
			drawOutputs(x, y, rotation);
		}
	}
}
