package net.johnvictorfs.simple_utilities.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.johnvictorfs.simple_utilities.helpers.Colors;

@Config(name = "simple_utilities")
public class SimpleUtilitiesConfig implements ConfigData {

//    static class AdvancedOptionsObj {
//        @ConfigEntry.Gui.Tooltip
//        private int transitionSpeed = 4500;
//    }

    public static class StatusElements {
        public boolean toggleCoordinatesStatus = true;
        public boolean toggleDirectionStatus = true;
        public boolean toggleEquipmentStatus = true;
        public boolean toggleFpsStatus = true;
        public boolean toggleSprintStatus = true;
        public boolean toggleBiomeStatus = true;
        public boolean toggleGameTimeStatus = true;
        public boolean toggleNetherCoordinateConversion = false;
        public boolean togglePlayerSpeedStatus = true;
        public boolean toggleLightLevelStatus = true;
        public boolean togglePlayerName = true;
        public boolean toggleServerName = true;
        public boolean toggleServerAddress = true;
    }

    public static class UIConfig {
        public boolean toggleSimpleUtilitiesHUD = true;
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        public int Xcords = 0;
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        public int Ycords = 0;
        @ConfigEntry.ColorPicker
        public int textColor = Colors.white;
    }

    @ConfigEntry.Gui.TransitiveObject
    public UIConfig uiConfig = new UIConfig();

    @ConfigEntry.Gui.CollapsibleObject
    public StatusElements statusElements = new StatusElements();
}
