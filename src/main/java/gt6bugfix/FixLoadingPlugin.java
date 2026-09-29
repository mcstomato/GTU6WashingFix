package gt6bugfix;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

/**
 * Pure coremod entry point. No @Mod container, no game content.
 * Registered via "FMLCorePlugin" in META-INF/MANIFEST.MF.
 */
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.Name("GT6WashingBugfix")
@IFMLLoadingPlugin.TransformerExclusions({"gt6bugfix."})
public class FixLoadingPlugin implements IFMLLoadingPlugin {

	@Override
	public String[] getASMTransformerClass() {
		return new String[] {"gt6bugfix.UTSelectTransformer"};
	}

	@Override
	public String getModContainerClass() {
		return "gt6bugfix.FixModContainer";
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
		// no data needed
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}
}
