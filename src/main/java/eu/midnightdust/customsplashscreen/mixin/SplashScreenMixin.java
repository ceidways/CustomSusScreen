package eu.midnightdust.customsplashscreen.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import eu.midnightdust.customsplashscreen.CustomSplashScreenClient;
import eu.midnightdust.customsplashscreen.config.CustomSplashScreenConfig;
import eu.midnightdust.customsplashscreen.texture.BlurredConfigTexture;
import eu.midnightdust.customsplashscreen.texture.ConfigTexture;
import eu.midnightdust.customsplashscreen.texture.EmptyTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BackgroundHelper;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.io.File;

import static net.minecraft.client.gui.DrawableHelper.drawTexture;
import static net.minecraft.client.gui.DrawableHelper.fill;

@Mixin(SplashOverlay.class)
public class SplashScreenMixin {

    @Shadow @Final static Identifier LOGO;
    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private boolean reloading;
    @Shadow @Final private ResourceReload reload;
    @Shadow private float progress;
    @Shadow private long reloadCompleteTime;
    @Shadow private long reloadStartTime;
    @Shadow @Final private Consumer<Optional<Throwable>> exceptionHandler;

    private static final CustomSplashScreenConfig CS_CONFIG = CustomSplashScreenClient.CS_CONFIG;
    private static final Identifier EMPTY_TEXTURE = new Identifier("empty.png");
    private static final Identifier MOJANG_TEXTURE = new Identifier("mojangstudios.png");
    private static ArrayList<Identifier> frameList;
    private static Long startTimeLastRender = null;
    private static Float currentFrame = null;

    @Inject(method = "init(Lnet/minecraft/client/MinecraftClient;)V", at = @At("HEAD"), cancellable = true)
    private static void init(MinecraftClient client, CallbackInfo ci) { // Load our custom textures at game start //

        if (CS_CONFIG.logoType == CustomSplashScreenConfig.LogoType.Static) {
            client.getTextureManager().registerTexture(LOGO, new BlurredConfigTexture(MOJANG_TEXTURE));
        }
        else {
            client.getTextureManager().registerTexture(LOGO, new EmptyTexture(EMPTY_TEXTURE));
        }

        frameList = new ArrayList<Identifier>(CustomSplashScreenClient.getFrames());
        ArrayList<Identifier> frames = new ArrayList<Identifier>();


        for (Identifier frame : frameList) {
            client.getTextureManager().registerTexture(frame, new ConfigTexture(frame));
        }

        /* ~2s faster loading, but have to restart after changing anim speed.
        for (float i = 0f;  i < frameList.size() - 1;) {
            i = Math.min(i + CS_CONFIG.animSpeed/10, frameList.size()-1);
            Identifier frame = frameList.get(Math.round(i));
            client.getTextureManager().registerTexture(frame, new ConfigTexture(frame));
        }*/

        ci.cancel();
    }

    @Inject(at = @At("TAIL"), method = "render")
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {

        float prog = this.reload.getProgress();
        int width = this.client.getWindow().getScaledWidth();
        int height = this.client.getWindow().getScaledHeight();
        long l = Util.getMeasuringTimeMs();
        if (this.reloading && this.reloadStartTime == -1L) {
            this.reloadStartTime = l;
        }

        float f = this.reloadCompleteTime > -1L ? (float)(l - this.reloadCompleteTime) / 1000.0F : -1.0F;
        float g = this.reloadStartTime > -1L ? (float)(l - this.reloadStartTime) / 500.0F : -1.0F;
        float s;
        int m;

        // Render our custom color
        if (f >= 1.0F) {
            if (this.client.currentScreen != null) {
                this.client.currentScreen.render(matrices, 0, 0, delta);
            }

            m = MathHelper.ceil((1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
            fill(matrices, 0, 0, width, height, withAlpha(m));
            s = 1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F);
        } else if (this.reloading) {
            if (this.client.currentScreen != null && g < 1.0F) {
                this.client.currentScreen.render(matrices, mouseX, mouseY, delta);
            }

            m = MathHelper.ceil(MathHelper.clamp(g, 0.15D, 1.0D) * 255.0D);
            fill(matrices, 0, 0, width, height, withAlpha(m));
            s = MathHelper.clamp(g, 0.0F, 1.0F);
        } else {
            m = getBackgroundColor();
            float p = (float)(m >> 16 & 255) / 255.0F;
            float q = (float)(m >> 8 & 255) / 255.0F;
            float r = (float)(m & 255) / 255.0F;
            GlStateManager._clearColor(p, q, r, 1.0F);
            GlStateManager._clear(16384, MinecraftClient.IS_SYSTEM_MAC);
            s = 1.0F;
        }

        m = (int)((double)this.client.getWindow().getScaledWidth() * 0.5D);
        int u = (int)((double)this.client.getWindow().getScaledHeight() * 0.5D);
        double d = Math.min((double)this.client.getWindow().getScaledWidth() * 0.75D, this.client.getWindow().getScaledHeight()) * 0.25D;
        int v = (int)(d * 0.5D);
        double e = d * 4.0D;
        int w = (int)(e * 0.5D);

        // Render the background anim
        if (CS_CONFIG.logoType == CustomSplashScreenConfig.LogoType.Animated) {
            if (startTimeLastRender == null) {
                currentFrame = 0f;
            } else if (startTimeLastRender != this.reloadStartTime) {
                currentFrame = 0f;
            } else {
                currentFrame = Math.min(currentFrame + CS_CONFIG.animSpeed/10, (int) Math.ceil(prog*frameList.size()) - 1 + CS_CONFIG.framesAhead);
            }
            currentFrame = Math.min(currentFrame,frameList.size()-1);
            int roundFrame = Math.round(currentFrame);
            //System.out.println(roundFrame + 1);
            RenderSystem.setShaderTexture(0, frameList.get(roundFrame));
            RenderSystem.enableBlend();
            RenderSystem.blendEquation(32774);
            RenderSystem.blendFunc(770, 1);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, s);
            drawTexture(matrices, 0, 0, 0, 0, 0, width, height, width, height);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }

        // Render the Logo
        if (CS_CONFIG.logoType == CustomSplashScreenConfig.LogoType.Static) {
            RenderSystem.setShaderTexture(0, LOGO);
        }
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        if (CS_CONFIG.logoType == CustomSplashScreenConfig.LogoType.Static) {
            RenderSystem.blendFunc(770, 1);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, s);
            drawTexture(matrices, m - w, u - v, w, (int)d, -0.0625F, 0.0F, 120, 60, 120, 120);
            drawTexture(matrices, m, u - v, w, (int)d, 0.0625F, 60.0F, 120, 60, 120, 120);
        }

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        int x = (int)((double)this.client.getWindow().getScaledHeight() * 0.8325D);
        float y = this.reload.getProgress();
        this.progress = MathHelper.clamp(this.progress * 0.95F + y * 0.050000012F, 0.0F, 1.0F);
        if (f < 1.0F) {
            this.renderProgressBar(matrices, width / 2 - w, x - 5, width / 2 + w, x + 5, 1.0F - MathHelper.clamp(f, 0.0F, 1.0F), null);
        }

        if (f >= 2.0F) {
            this.client.setOverlay(null);
        }

        if (this.reloadCompleteTime == -1L && this.reload.isComplete() && (!this.reloading || g >= 2.0F)) {
            try {
                this.reload.throwException();
                this.exceptionHandler.accept(Optional.empty());
            } catch (Throwable var23) {
                this.exceptionHandler.accept(Optional.of(var23));
            }

            this.reloadCompleteTime = Util.getMeasuringTimeMs();
            if (this.client.currentScreen != null) {
                this.client.currentScreen.init(this.client, this.client.getWindow().getScaledWidth(), this.client.getWindow().getScaledHeight());
            }
        }

        startTimeLastRender = this.reloadStartTime;
    }

    private static int getBackgroundColor() {
        if (CS_CONFIG.logoType == CustomSplashScreenConfig.LogoType.Animated) {
            return BackgroundHelper.ColorMixer.getArgb(0, 0, 0, 0);
        }
        else {
            return CustomSplashScreenClient.CS_CONFIG.backgroundColor;
        }
    }

    private static int withAlpha(int alpha) {
        return getBackgroundColor() | alpha << 24;
    }

    @Inject(at = @At("TAIL"), method = "renderProgressBar")
    private void renderProgressBar(MatrixStack matrices, int x1, int y1, int x2, int y2, float opacity, CallbackInfo ci) {
        int i = MathHelper.ceil((float)(x2 - x1 - 2) * this.progress);

        // Vanilla / With Color progress bar
        if (CS_CONFIG.progressBar) {
            int j = Math.round(opacity * 255.0F);
            int k = CustomSplashScreenClient.CS_CONFIG.progressBarColor | 255 << 24;
            int kk = CustomSplashScreenClient.CS_CONFIG.progressFrameColor | 255 << 24;
            fill(matrices, x1 + 2, y1 + 2, x1 + i, y2 - 2, k);
            fill(matrices, x1 + 1, y1, x2 - 1, y1 + 1, kk);
            fill(matrices, x1 + 1, y2, x2 - 1, y2 - 1, kk);
            fill(matrices, x1, y1, x1 + 1, y2, kk);
            fill(matrices, x2, y1, x2 - 1, y2, kk);
        }

    }

}
