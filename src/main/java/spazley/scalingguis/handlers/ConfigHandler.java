package spazley.scalingguis.handlers;

import spazley.scalingguis.ScalingGUIs;
import spazley.scalingguis.config.CustomScales;
import spazley.scalingguis.config.JsonHelper;
import spazley.scalingguis.gui.guiconfig.ScaleConfigElement;
import spazley.scalingguis.gui.guiconfig.SnappingSliderEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.io.*;
import java.util.*;

/*
@Config(modid = ScalingGUIs.MODID, name = "ScalingGUIs")
public class ConfigHandler {

    @Config.Comment("Enable logging of GUI class names.")
    public static boolean logGuiClassNames = false;

    @Config.Comment("Maintain separate persistent log of GUI class names.")
    public static boolean persistentLog = false;

    //@Config.Comment("Default scale for GUI rendering. Auto is 0.")
    //@Config.RangeInt(min = 0, max = 16)
    //public static int mainScale = Minecraft.getMinecraft().gameSettings.guiScale;

}
*/


public class ConfigHandler
{
    public static Configuration config;
    public static CustomScales customScales;

    private static List<String> individualGuiClassNames;
    private static List<String> groupGuiClassNames;

    public static boolean logGuiClassNames = false;
    public static boolean persistentLog = true;

    //public static String configPath = "config/ScalingGUIs/ScalingGUIs.cfg";
    public static String scaleOverridesPath = "config/ScalingGUIs/ScalingGUIsCustomScales.json";

    public static final int MIN_SCALE = 0;
    public static final int MAX_SCALE = 9; //max actual scale of 8x. +1 to allow sliders to have default scale option


    public static void initConfigs()
    {
        if(config == null) {
            ScalingGUIs.logger.error("Attempted to load config before initialized.");
            return;
        }

        config.load();

        logGuiClassNames = config.getBoolean("Log GUI Class Names", Configuration.CATEGORY_GENERAL, false, "Enable logging of GUI class names in the Minecraft log.");
        persistentLog = config.getBoolean("Persistent Log", Configuration.CATEGORY_GENERAL, false, "Maintain persistent log of GUI class names.");
        //Collections.addAll(loggedGuiClassNames, config.getStringList("Logged GUI Class Names", "log", new String[0], "Persistent log of GUI class names. Updated on config save."));

        config.save();


        File fileScaleOverrides = new File(scaleOverridesPath);
        customScales = JsonHelper.scalesFromJsonFile(fileScaleOverrides);
        customScales.checkCustomEntries();
        JsonHelper.scalesToJsonFile(fileScaleOverrides, customScales);

        individualGuiClassNames = JsonHelper.getKeyList(customScales.customIndividualGUIScales);
        groupGuiClassNames = JsonHelper.getKeyList(customScales.customGroupGUIScales);
    }

    public static void saveConfigs()
    {
        ScalingGUIs.logger.info("Saving configs");
        File fileScaleOverrides = new File(scaleOverridesPath);

        //config.getStringList("Logged GUI Class Names", "log", loggedGuiClassNames.toArray(new String[loggedGuiClassNames.size()]), "Persistent log of GUI class names. Updated on config save.");

        Minecraft.getMinecraft().gameSettings.guiScale = customScales.guiScale;
        Minecraft.getMinecraft().gameSettings.saveOptions();
        config.save();
        JsonHelper.scalesToJsonFile(fileScaleOverrides, customScales);
        initConfigs();
    }

    public static boolean inIndividuals(String className)
    {
        return individualGuiClassNames.contains(className);
    }

    public static String inGroups(GuiScreen guiScreen)
    {
        for (String s : groupGuiClassNames) {
            try {
                Class<?> c = Class.forName(s);
                if (c.isInstance(guiScreen)) {
                    //ScalingGUIs.logger.info("Groups contains '" + guiScreen.getClass().getName() + "'.");
                    return s;
                }
            } catch(Exception e) {
                ScalingGUIs.logger.error("Unable to determine class for '" + s + "': ", e);
            }

        }

        return "NONE";
    }

    public static int getIndividualScale(String className)
    {
        int scale = customScales.customIndividualGUIScales.getAsJsonObject(className).getAsJsonPrimitive("scale").getAsInt();
        return scale == MAX_SCALE ? customScales.guiScale : scale;
    }

    public static int getGroupScale(String className)
    {
        int scale = customScales.customGroupGUIScales.getAsJsonObject(className).getAsJsonPrimitive("scale").getAsInt();
        return scale == MAX_SCALE ? customScales.guiScale : scale;
    }

    public static int getGuiScale(GuiScreen guiScreen)
    {
        if (inIndividuals(guiScreen.getClass().getName())) {
            return getIndividualScale(guiScreen.getClass().getName());
        } else if (!"NONE".equals(inGroups(guiScreen))) {
            return getGroupScale(inGroups(guiScreen));
        } else if (guiScreen instanceof GuiChat) {
            return customScales.hudScale;
        } else {
            return customScales.guiScale;
        }
    }

    public static int getTooltipScale()
    {
        return customScales.tooltipScale == MAX_SCALE ? customScales.guiScale : customScales.tooltipScale;
    }

    public static int getHudScale()
    {
        return customScales.hudScale == MAX_SCALE ? customScales.guiScale : customScales.hudScale;
    }


    public static List<IConfigElement> getMainsList()
    {
        List<IConfigElement> list = new ArrayList<>();
        //int defaultScale = customScales.guiScale;
        int defaultScale = MAX_SCALE; //Causes the scale to default to the main GUI scale

        Property guiScaleProp = new Property("guiScale", String.valueOf(customScales.guiScale), Property.Type.INTEGER, "scalingguis.config.main.guiscale");
        guiScaleProp.setDefaultValue(defaultScale);
        guiScaleProp.setMinValue(MIN_SCALE);
        guiScaleProp.setMaxValue(MAX_SCALE);
        list.add(new ScaleConfigElement(guiScaleProp).setCustomListEntryClass(SnappingSliderEntry.class));

        Property hudScaleProp = new Property("hudScale", String.valueOf(customScales.hudScale), Property.Type.INTEGER, "scalingguis.config.main.hudscale");
        hudScaleProp.setDefaultValue(defaultScale);
        hudScaleProp.setMinValue(MIN_SCALE);
        hudScaleProp.setMaxValue(MAX_SCALE);
        list.add(new ScaleConfigElement(hudScaleProp).setCustomListEntryClass(SnappingSliderEntry.class));

        Property tooltipScaleProp = new Property("tooltipScale", String.valueOf(customScales.tooltipScale), Property.Type.INTEGER, "scalingguis.config.main.tooltipscale");
        tooltipScaleProp.setDefaultValue(defaultScale);
        tooltipScaleProp.setMinValue(MIN_SCALE);
        tooltipScaleProp.setMaxValue(MAX_SCALE);
        list.add(new ScaleConfigElement(tooltipScaleProp).setCustomListEntryClass(SnappingSliderEntry.class));

        return list;
    }

    public static List<IConfigElement> getIndividualsList()
    {
        List<IConfigElement> list = new ArrayList<>();
        int defaultScale = customScales.guiScale;
/*
        int minValue = 0;
        int maxValue = 3;
*/

        for (String s : JsonHelper.getKeyList(customScales.customIndividualGUIScales)) {
            String val = customScales.customIndividualGUIScales.getAsJsonObject(s).get("scale").getAsString();
            String name = customScales.customIndividualGUIScales.getAsJsonObject(s).get("name").getAsString();

            Property prop = new Property(name, val, Property.Type.INTEGER, "");
            prop.setDefaultValue(defaultScale);
            prop.setMinValue(MIN_SCALE);
            prop.setMaxValue(MAX_SCALE);
            prop.setComment(s);
            list.add(new ScaleConfigElement(prop).setCustomListEntryClass(SnappingSliderEntry.class));
        }

        return list;
    }

    public static List<IConfigElement> getGroupsList()
    {
        List<IConfigElement> list = new ArrayList<>();
        int defaultScale = customScales.guiScale;
/*
        int minValue = 0;
        int maxValue = 3;
*/

        for (String s : JsonHelper.getKeyList(customScales.customGroupGUIScales)) {
            String val = customScales.customGroupGUIScales.getAsJsonObject(s).get("scale").getAsString();
            String name = customScales.customGroupGUIScales.getAsJsonObject(s).get("name").getAsString();

            Property prop = new Property(name, val, Property.Type.INTEGER);
            prop.setDefaultValue(defaultScale);
            prop.setMinValue(MIN_SCALE);
            prop.setMaxValue(MAX_SCALE);
            prop.setComment(s);
            list.add(new ScaleConfigElement(prop).setCustomListEntryClass(SnappingSliderEntry.class));
        }

        return list;
    }

    public static Map<Object, String> getLoggedClassNames()
    {
        List<String> list = new ArrayList<>(customScales.loggedGUIClassNames);

        list.removeAll(individualGuiClassNames);
        list.removeAll(groupGuiClassNames);

        Map<Object, String> map = new TreeMap<>();

        for (String s : list) {
            map.put((Object)s,s);
        }

        return map;
    }

    public static void logClassName(String className)
    {
        customScales.loggedGUIClassNames.add(className);
    }

}
