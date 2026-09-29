package gt6bugfix;

import java.util.Arrays;

import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;

import com.google.common.eventbus.EventBus;

/**
 * Metadata-only ModContainer for this pure coremod. Instantiated by FML via
 * FixLoadingPlugin.getModContainerClass(). It registers no handlers and adds
 * no game content; it only makes name/version/author/description visible in
 * the FML mod list and the Mods GUI.
 */
public class FixModContainer extends DummyModContainer {

	public FixModContainer() {
		super(buildMetadata());
	}

	/**
	 * DummyModContainer.registerBus() returns false, which makes the
	 * LoadController mark the container DISABLED. We have no @Subscribe
	 * handlers, but returning true keeps the container in the normal
	 * lifecycle (so it shows Available instead of Disabled).
	 */
	@Override
	public boolean registerBus(EventBus bus, LoadController controller) {
		bus.register(this);
		return true;
	}

	private static ModMetadata buildMetadata() {
		ModMetadata md = new ModMetadata();
		md.modId       = "gt6washingfix";
		md.name        = "GT6 Washing Bugfix";
		md.version     = "1.0.0";
		md.description = "Fixes the dedicated server crash (IllegalArgumentException: bound must be positive) "
			+ "caused by gregapi.util.UT$Code.select when a washable GregTech 6 material has no byproducts. "
			+ "No gameplay changes.";

		// TODO: replace with your name (multiple authors: Arrays.asList("A", "B"))
		md.authorList  = Arrays.asList("MC_Stomato");
		md.credits     = "Original bug analysis for GregTech 6 Unofficial 6.15.08";
		md.url         = "";   // optional project/homepage URL

		return md;
	}
}
