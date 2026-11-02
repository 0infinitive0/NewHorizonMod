package newhorizon.expand.block.hiemara;

import arc.Core;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.type.Planet;
import mindustry.type.Sector;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.blocks.campaign.LaunchPad;

import static mindustry.Vars.state;

/**
 * A launch pad that can send its items to a sector with a base on any planet.
 *
 * <p>The base game's pad only lets you pick sectors of the planet it stands on, and its modern way of delivering (a steady import rate
 * between the sectors of one planet, worked out by {@code SectorInfo}) cannot cross planets. So this pad lets you pick a sector of any
 * planet, and when what it launched lands ({@code LaunchItemEvent}, which the base game fires for every launched stack) and the target is
 * on another planet, the stack is added to the target sector's saved items with {@code Sector.addItems}, as the old launch pads did:
 * they are there when you land on that sector. As in the base game the target belongs to the whole sector, not to one pad.
 */
public class InterplanetaryLaunchPad extends LaunchPad{
	public InterplanetaryLaunchPad(String name){
		super(name);
	}
	
	/** Starts the delivery to other planets. Called once, when the content is loaded. */
	public static void registerDelivery(){
		arc.Events.on(mindustry.game.EventType.LaunchItemEvent.class, e -> {
			if(Vars.net.client() || !state.isCampaign())return;
			Sector target = state.rules.sector.info.destination;
			if(target == null || target.planet == state.rules.sector.planet || !target.hasBase())return;
			var items = new mindustry.type.ItemSeq();
			items.add(e.stack);
			target.addItems(items);
		});
	}
	
	/** @return every sector of every planet in which the player has a base, other than the one being played. */
	public static arc.struct.Seq<Sector> targets(){
		var list = new arc.struct.Seq<Sector>();
		for(Planet planet : Vars.content.planets()){
			for(Sector sector : planet.sectors){
				if(sector != state.rules.sector && sector.hasBase())list.add(sector);
			}
		}
		return list;
	}
	
	public class InterplanetaryLaunchPadBuild extends LaunchPadBuild{
		@Override
		public void buildConfiguration(Table table){
			if(!state.isCampaign() || Vars.net.client()){
				deselect();
				return;
			}
			table.button(Icon.upOpen, Styles.cleari, this::showTargets).size(40f);
		}
		
		protected void showTargets(){
			BaseDialog dialog = new BaseDialog("@interplanetary.title");
			dialog.cont.pane(t -> {
				t.defaults().pad(4f).growX().minWidth(320f);
				var list = targets();
				if(list.isEmpty()){
					t.add("@interplanetary.none").wrap().width(380f).row();
				}
				Planet last = null;
				for(Sector sector : list){
					if(sector.planet != last){
						last = sector.planet;
						t.add(last.localizedName).color(arc.graphics.Color.gold).left().padTop(10f).row();
					}
					boolean chosen = state.rules.sector.info.destination == sector;
					t.button((chosen ? "[accent]> [] " : "") + sector.name(), Styles.flatt, () -> {
						state.rules.sector.info.destination = sector;
						dialog.hide();
						deselect();
					}).left().height(46f).row();
				}
				t.button("@interplanetary.clear", Icon.cancel, Styles.flatt, () -> {
					state.rules.sector.info.destination = null;
					dialog.hide();
					deselect();
				}).left().height(46f).padTop(10f).row();
			}).grow();
			dialog.addCloseButton();
			dialog.show();
		}
	}
}
