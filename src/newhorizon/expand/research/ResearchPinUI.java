package newhorizon.expand.research;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.TechTree.TechNode;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.ui.Styles;

import static mindustry.Vars.state;

/**
 * The interface of pinned research: a Pin/Unpin button in the research dialog (for the node under the cursor), a pin mark on pinned nodes
 * of the tree, and a small panel on the HUD with the progress of each pinned node and a button to unpin it.
 */
public class ResearchPinUI{
	private static boolean installed;

	public static void init(){
		if(installed || Vars.headless || Vars.ui == null || Vars.ui.research == null) return;
		installed = true;
		installDialog();
		installHud();
	}

	/** The button under a node's info, and the pin marks on the nodes of the tree. */
	private static void installDialog(){
		var dialog = Vars.ui.research;
		Table info = dialog.view.infoTable;

		info.update(() -> {
			var hover = dialog.view.hoverNode;
			if(hover == null || !(hover.userObject instanceof TechNode node) || !info.hasChildren()) return;
			if(!state.isCampaign() || Vars.net.client() || info.find("nh-pin-button") != null) return;

			info.row();
			info.button(Core.bundle.get(PinnedResearch.isPinned(node.content) ? "research.nh-unpin" : "research.nh-pin"), Icon.star, Styles.cleart, () -> {
				PinnedResearch.toggle(node.content);
			}).name("nh-pin-button").growX().height(36f).update(b -> b.setText(Core.bundle.get(PinnedResearch.isPinned(node.content) ? "research.nh-unpin" : "research.nh-pin")));
			info.invalidateHierarchy();
			info.pack();
		});

		dialog.update(() -> {
			for(Element e : dialog.view.getChildren()){
				if(e instanceof ImageButton b && b.userObject instanceof TechNode node && b.find("nh-pin-mark") == null){
					Image mark = new Image(Icon.star);
					mark.name = "nh-pin-mark";
					mark.setColor(Pal.accent);
					mark.touchable = arc.scene.event.Touchable.disabled;
					b.addChild(mark);
					mark.update(() -> {
						mark.visible = PinnedResearch.isPinned(node.content);
						mark.setSize(16f);
						mark.setPosition(b.getWidth() - 18f, b.getHeight() - 18f);
					});
				}
			}
		});
	}

	/** A panel on the left of the HUD listing the pinned research. */
	private static void installHud(){
		Table panel = new Table(Styles.black6);
		panel.name = "nh-pins";
		float[] clock = {1000f};
		panel.update(() -> {
			clock[0] += Time.delta;
			if(clock[0] < 30f) return;
			clock[0] = 0f;
			rebuild(panel);
		});
		panel.visible(() -> state.isCampaign() && Vars.ui.hudfrag.shown && panel.hasChildren());

		Vars.ui.hudGroup.fill(root -> {
			root.name = "nh-pins-root";
			root.touchable = arc.scene.event.Touchable.childrenOnly;
			root.left();
			root.add(panel).left().margin(6f);
		});
	}

	private static void rebuild(Table panel){
		panel.clear();
		if(!state.isCampaign()) return;
		Seq<TechNode> pins = PinnedResearch.pins();
		if(pins.isEmpty()) return;

		panel.margin(6f);
		panel.add(Core.bundle.get("hud.nh-pins.title")).color(Pal.accent).left().colspan(3).row();
		TechNode first = pins.find(PinnedResearch::researchable);
		for(TechNode node : pins){
			boolean waiting = !PinnedResearch.researchable(node);
			panel.image(new TextureRegionDrawable(node.content.uiIcon)).size(28f).padRight(4f);
			panel.add(new Bar(
				() -> node.localizedName() + (waiting ? " (" + Core.bundle.get("hud.nh-pin.waiting") + ")" : " " + (int)(PinnedResearch.progress(node) * 100f) + "%"),
				() -> waiting ? Pal.gray : node == first ? Pal.accent : Color.lightGray,
				() -> PinnedResearch.progress(node))).size(190f, 24f);
			panel.button(Icon.cancel, Styles.cleari, 20f, () -> PinnedResearch.unpin(node.content)).size(26f).padLeft(2f).row();
		}
	}
}
