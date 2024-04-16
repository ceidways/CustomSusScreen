package eu.midnightdust.customsplashscreen.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import eu.midnightdust.customsplashscreen.CustomSplashScreenClient;
import eu.midnightdust.customsplashscreen.config.CustomSplashScreenConfig;
import eu.midnightdust.customsplashscreen.texture.BlurredConfigTexture;
import eu.midnightdust.customsplashscreen.texture.ConfigTexture;
import eu.midnightdust.customsplashscreen.texture.EmptyTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
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
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.ArrayList;

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

    @Shadow
    private static int withAlpha(int color, int alpha) {
        return 0;
    }

    @Shadow @Final private static IntSupplier BRAND_ARGB;
    private static final CustomSplashScreenConfig CS_CONFIG = CustomSplashScreenClient.CS_CONFIG;
    private static final Identifier EMPTY_TEXTURE = new Identifier("empty.png");
    private static final Identifier MOJANG_TEXTURE = new Identifier("mojangstudios.png");
    private static ArrayList<Identifier> frameList;
    private static Long startTimeLastRender = null;
    private static Float currentFrame = null;

    @Inject(method = "init(Lnet/minecraft/client/MinecraftClient;)V", at = @At("TAIL"), cancellable = true)
    private static void init(MinecraftClient client, CallbackInfo ci) { // Load our custom textures at game start //

        if (CS_CONFIG.logoType == CustomSplashScreenConfig.LogoType.Static) {
            client.getTextureManager().registerTexture(LOGO, new BlurredConfigTexture(MOJANG_TEXTURE));
        }
        else {
            client.getTextureManager().registerTexture(LOGO, new EmptyTexture(EMPTY_TEXTURE));
        }

        frameList = new ArrayList<Identifier>(CustomSplashScreenClient.getFrames());
        ArrayList<Identifier> frames = new ArrayList<Identifier>();

        if (CS_CONFIG.loadUnusedFrames) {
            for (Identifier frame : frameList) {
                client.getTextureManager().registerTexture(frame, new ConfigTexture(frame));
            }
        } else {
            for (float i = 0f;  i < frameList.size() - 1;) {
                i = Math.min(i + CS_CONFIG.animSpeed/10, frameList.size()-1);
                Identifier frame = frameList.get(Math.round(i));
                client.getTextureManager().registerTexture(frame, new ConfigTexture(frame));
            }
        }


        ci.cancel();
    }

    // Render the background anim
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;getScaledWindowWidth()I", shift = At.Shift.BEFORE, ordinal = 2))
    private void css$renderSplashBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (CS_CONFIG.logoType == CustomSplashScreenConfig.LogoType.Animated) {
            int width = client.getWindow().getScaledWidth();
            int height = client.getWindow().getScaledHeight();
            float prog = this.reload.getProgress();
            float f = this.reloadCompleteTime > -1L ? (float)(Util.getMeasuringTimeMs() - this.reloadCompleteTime) / 1000.0F : -1.0F;
            float g = this.reloadStartTime> -1L ? (float)(Util.getMeasuringTimeMs() - this.reloadStartTime) / 500.0F : -1.0F;
            float s;
            if (f >= 1.0F) s = 1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F);
            else if (reloading) s = MathHelper.clamp(g, 0.0F, 1.0F);
            else s = 1.0F;

            if (startTimeLastRender == null) {
                currentFrame = 0f;
            } else if (startTimeLastRender != this.reloadStartTime) {
                currentFrame = 0f;
            } else {
                currentFrame = Math.min(currentFrame + CS_CONFIG.animSpeed/10, (int) Math.ceil(prog*frameList.size()) - 1 + CS_CONFIG.framesAhead);
            }
            currentFrame = Math.min(currentFrame,frameList.size()-1);
            int roundFrame = Math.round(currentFrame);

            RenderSystem.enableBlend();
            RenderSystem.blendEquation(32774);
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, s);
            context.drawTexture(frameList.get(roundFrame), 0, 0, 0, 0, 0, width, height, width, height);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
        startTimeLastRender = this.reloadStartTime;
    }

    @Inject(method = "withAlpha", at = @At("RETURN"), cancellable = true)
    private static void css$modifyBackgroundColor(int color, int alpha, CallbackInfoReturnable<Integer> cir) {
        if (color == BRAND_ARGB.getAsInt()) {
            int configColor = (CS_CONFIG.logoType == CustomSplashScreenConfig.LogoType.Static) ? CustomSplashScreenClient.CS_CONFIG.backgroundColor | 255 << 24 : 0;
            cir.setReturnValue(configColor & 16777215 | alpha << 24);
        }
    }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_clearColor(FFFF)V"))
    private void css$clearModifiedColor(float red, float green, float blue, float alpha) {
        int k = (CS_CONFIG.logoType == CustomSplashScreenConfig.LogoType.Static) ? CustomSplashScreenClient.CS_CONFIG.backgroundColor : 0;
        float m = (float)(k >> 16 & 255) / 255.0F;
        float n = (float)(k >> 8 & 255) / 255.0F;
        float o = (float)(k & 255) / 255.0F;
        GlStateManager._clearColor(m, n, o, 1.0F);
    }
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;blendFunc(II)V", shift = At.Shift.AFTER))
    private void css$betterBlend(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {

    }

    @ModifyArg(method = "renderProgressBar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"), index = 4)
    private int css$modifyProgressFrame(int color) { // Set the Progress Bar Frame Color to our configured value //
        return CS_CONFIG.progressBar ? CS_CONFIG.progressFrameColor | 255 << 24 : 0;
    }
    @ModifyArg(method = "renderProgressBar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", ordinal = 0), index = 4)
    private int css$modifyProgressColor(int color) { // Set the Progress Bar Color to our configured value //
        return CS_CONFIG.progressBar ? CS_CONFIG.progressBarColor | 255 << 24 : 0;
    }
}
