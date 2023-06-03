package eu.midnightdust.customsplashscreen;

import eu.midnightdust.customsplashscreen.config.CustomSplashScreenConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;


public class CustomSplashScreenClient implements ClientModInitializer {

    public static CustomSplashScreenConfig CS_CONFIG;
    public static File CONFIG_PATH = new File(FabricLoader.getInstance().getConfigDir() + "/customsplashscreen");
    public static File ANIM_PATH = new File(FabricLoader.getInstance().getConfigDir() + "/customsplashscreen/animation");
    private static Path MojangTexture = Paths.get(CONFIG_PATH + "/mojangstudios.png");
    private static Path AnimationFramesTemp = Paths.get(ANIM_PATH + "/animation.txt");

    public static ArrayList<Identifier> getFrames() {
        ArrayList<Identifier> frames = new ArrayList<Identifier>();
        File[] files = new File("config/customsplashscreen/animation").listFiles();
        for (File file : files) {
            if (file.isFile()) {
                frames.add(new Identifier("animation/" + file.getName()));
            }
        }
        return frames;
    }

    @Override
    public void onInitializeClient() {
        AutoConfig.register(CustomSplashScreenConfig.class, JanksonConfigSerializer::new);
        CS_CONFIG = AutoConfig.getConfigHolder(CustomSplashScreenConfig.class).getConfig();

        if (!CONFIG_PATH.exists()) { // Run when config directory is nonexistant //
            CONFIG_PATH.mkdir(); // Create our custom config directory //
            ANIM_PATH.mkdir();

            // Open Input Streams for copying the default textures to the config directory //
            InputStream mojangstudios = Thread.currentThread().getContextClassLoader().getResourceAsStream("mojangstudios.png");
            try {
                // Copy the default textures into the config directory //
                Files.copy(mojangstudios, MojangTexture, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //copy animation files
            InputStream animationframes = Thread.currentThread().getContextClassLoader().getResourceAsStream("animation.txt");
            try {
                Files.copy(animationframes, AnimationFramesTemp);
            } catch (IOException e) {
                e.printStackTrace();
            }
            BufferedReader reader;
            ArrayList<String> frames = null;
            try {
                reader = new BufferedReader(new FileReader(AnimationFramesTemp.toString()));
                String line = reader.readLine();
                frames = new ArrayList<String>();
                while (line != null) {
                    Path current = Paths.get(ANIM_PATH + "/" + line);
                    try {
                        Files.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("animation/" + line), current);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // read next line
                    line = reader.readLine();
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            File obj = new File(AnimationFramesTemp.toString());
            obj.delete();




        }
    }
}
