package newhorizon.expand.block.chronos;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.meta.Env;

/**
 * A block that is linked to every other block of its kind on the same team that is set to the same channel. The channel is picked
 * from a row of colored buttons. Links are by channel, not by distance: one end can be anywhere.
 */
public abstract class ChannelBlock extends Block{
	public static final Color[] channelColors = {
		Color.valueOf("ff5c5c"), Color.valueOf("ff9f43"), Color.valueOf("ffd36b"), Color.valueOf("9be564"),
		Color.valueOf("3fd8a8"), Color.valueOf("6fd8ff"), Color.valueOf("4a7bff"), Color.valueOf("9b7bff"),
		Color.valueOf("e08bff"), Color.valueOf("ff7fb5"), Color.valueOf("d9d9e8"), Color.valueOf("8a8aa3")
	};
	public static final int channels = channelColors.length;

	public TextureRegion topRegion;
	/** Every built block of this kind, for finding partners. */
	public final Seq<ChannelBuild> all = new Seq<>();

	public ChannelBlock(String name){
		super(name);
		update = true;
		hasPower = true;
		configurable = true;
		saveConfig = true;
		copyConfig = true;
		envEnabled = Env.any;

		config(Integer.class, (ChannelBuild build, Integer channel) -> build.channel = Mathf.mod(channel, channels));
	}

	@Override
	public void load(){
		super.load();
		topRegion = arc.Core.atlas.find(name + "-top", region);
	}

	public class ChannelBuild extends Building{
		public int channel;
		protected boolean registered;

		@Override
		public void add(){
			if(!registered){
				registered = true;
				all.add(this);
			}
			super.add();
		}

		@Override
		public void remove(){
			if(registered){
				registered = false;
				all.remove(this, true);
			}
			super.remove();
		}

		public Color channelColor(){
			return channelColors[Mathf.mod(channel, channels)];
		}

		/** Another block of this kind on the same team and channel, if there is one that powers. */
		public Seq<ChannelBuild> partners(){
			Seq<ChannelBuild> out = new Seq<>();
			for(ChannelBuild other : all){
				if(other != this && other.team == team && other.channel == channel && other.isValid()) out.add(other);
			}
			return out;
		}

		@Override
		public void buildConfiguration(Table table){
			table.table(Styles.black6, t -> {
				t.margin(4f);
				for(int i = 0; i < channels; i++){
					int index = i;
					Button button = new Button(Styles.clearTogglei);
					button.image(Tex.whiteui).color(channelColors[i]).size(20f);
					button.clicked(() -> configure(index));
					button.update(() -> button.setChecked(channel == index));
					t.add(button).size(32f).pad(1f);
					if(i % 6 == 5) t.row();
				}
			});
		}

		@Override
		public Integer config(){
			return channel;
		}

		/** Draws the channel's color over the block's top sprite. */
		protected void drawChannel(){
			Draw.color(channelColor());
			Draw.rect(topRegion, x, y);
			Draw.color();
		}

		@Override
		public void write(Writes write){
			super.write(write);
			write.b(channel);
		}

		@Override
		public void read(Reads read, byte revision){
			super.read(read, revision);
			channel = Mathf.mod(read.b(), channels);
		}
	}
}
