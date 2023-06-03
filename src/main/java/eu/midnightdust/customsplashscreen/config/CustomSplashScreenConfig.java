package eu.midnightdust.customsplashscreen.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "customsplashscreen")
public class CustomSplashScreenConfig implements ConfigData {
    @Comment(value = "Change the type of logo RESTART GAME IF CHANGE.")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public LogoType logoType = LogoType.Animated;
    @Comment(value= "Each render advances current frame by this/10.")
    @ConfigEntry.BoundedDiscrete(min=0,max=200)
    public long animSpeed = 30;
    @Comment(value= "anim with 100 frames, at 50% load, and this at 20, pauses on frame 70. ")
    @ConfigEntry.BoundedDiscrete(min=0,max=300)
    public long framesAhead = 83;
    @Comment(value = "Toggle progress bar")
    public boolean progressBar = true;
    @Comment(value = "Change the color of the background")
    @ConfigEntry.ColorPicker
    public int backgroundColor = 15675965;
    @Comment(value = "Change the color of the progress bar")
    @ConfigEntry.ColorPicker
    public int progressBarColor = 16777215;
    @Comment(value = "Change the color of the progress bar frame")
    @ConfigEntry.ColorPicker
    public int progressFrameColor = 16777215;;

    public enum LogoType {
        Static, Animated;
    }
}
