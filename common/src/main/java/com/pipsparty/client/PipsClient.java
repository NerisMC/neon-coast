package com.pipsparty.client;

import com.pipsparty.PipsParty;
import com.pipsparty.entity.AnimatronicEntity;
import com.pipsparty.entity.Character;
import com.pipsparty.entity.SecurityCameraEntity;
import com.pipsparty.item.CameraTabletItem;
import com.pipsparty.registry.ModEntities;
import com.pipsparty.registry.ModSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** Client side, shared by all loaders: renderers, model layers, the camera system and the HUD. */
public final class PipsClient {
    public static final ModelLayerLocation CAMERA_LAYER = new ModelLayerLocation(PipsParty.id("security_camera"), "main");
    public static final Identifier HUD_ID = PipsParty.id("hud");
    private static final Map<Character, ModelLayerLocation> LAYERS = new LinkedHashMap<>();

    static {
        for (Character c : Character.values()) LAYERS.put(c, new ModelLayerLocation(PipsParty.id(c.name().toLowerCase()), "main"));
    }

    private static int camIndex;
    private static boolean viewing;
    private static boolean prevLeft, prevRight;
    private static int flipTicks;
    private static int scareTicks;
    private static String camLabel = "";
    private static int camCount;
    private static final Random NOISE = new Random();

    private PipsClient() {
    }

    public static ModelLayerLocation layer(Character c) {
        return LAYERS.get(c);
    }

    public static Map<ModelLayerLocation, Supplier<LayerDefinition>> layers() {
        Map<ModelLayerLocation, Supplier<LayerDefinition>> map = new LinkedHashMap<>();
        for (Character c : Character.values()) map.put(LAYERS.get(c), () -> AnimatronicModel.create(c));
        map.put(CAMERA_LAYER, SecurityCameraRenderer::create);
        return map;
    }

    /** Loaders implement this with their own renderer registration call. */
    public interface RendererSink {
        <E extends Entity> void register(EntityType<? extends E> type, EntityRendererProvider<E> provider);
    }

    public static void registerRenderers(RendererSink sink) {
        for (Character c : Character.values()) {
            sink.register(ModEntities.of(c).get(), ctx -> new AnimatronicRenderer(ctx, c));
        }
        sink.register(ModEntities.SECURITY_CAMERA.get(), SecurityCameraRenderer::new);
    }

    // ------------------------------------------------------------------ tick

    public static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            viewing = false;
            return;
        }
        if (flipTicks > 0) flipTicks--;
        if (scareTicks > 0) scareTicks--;

        // am I being jumpscared?
        for (AnimatronicEntity a : mc.level.getEntitiesOfClass(AnimatronicEntity.class, p.getBoundingBox().inflate(4.0), a -> true)) {
            if (a.getScareTarget() == p.getId()) scareTicks = Math.max(scareTicks, 18);
        }

        boolean watching = CameraTabletItem.isWatching(p) && scareTicks == 0 && mc.screen == null;
        List<SecurityCameraEntity> cams = new ArrayList<>();
        if (watching) {
            cams.addAll(mc.level.getEntitiesOfClass(SecurityCameraEntity.class, p.getBoundingBox().inflate(64.0), c -> true));
            cams.sort(Comparator.comparing(SecurityCameraEntity::label));
        }
        boolean left = mc.options.keyLeft.isDown(), right = mc.options.keyRight.isDown();
        if (watching && !cams.isEmpty()) {
            if (left && !prevLeft) flip(p, -1);
            if (right && !prevRight) flip(p, 1);
            camIndex = Math.floorMod(camIndex, cams.size());
            SecurityCameraEntity cam = cams.get(camIndex);
            if (mc.getCameraEntity() != cam) {
                if (!viewing) flipTicks = 6;
                mc.setCameraEntity(cam);
            }
            viewing = true;
            camLabel = cam.label();
            camCount = cams.size();
        } else {
            if (viewing || (mc.getCameraEntity() instanceof SecurityCameraEntity)) mc.setCameraEntity(p);
            viewing = false;
            camLabel = watching ? "NO SIGNAL - place Security Cameras nearby" : "";
        }
        prevLeft = left;
        prevRight = right;
    }

    private static void flip(LocalPlayer p, int dir) {
        camIndex += dir;
        flipTicks = 6;
        p.level().playLocalSound(p.getX(), p.getY(), p.getZ(), ModSounds.CAMERA_FLIP.get(), SoundSource.PLAYERS, 0.6f, 1.0f, false);
    }

    // ------------------------------------------------------------------ HUD

    public static void renderHud(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        int w = g.guiWidth(), h = g.guiHeight();
        long time = mc.level != null ? mc.level.getGameTime() : 0;

        if (viewing || !camLabel.isEmpty()) {
            // a cheap security feed: green tint, scanlines, snow, REC light
            g.fill(0, 0, w, h, viewing ? 0x2A0A2A12 : 0xF0101010);
            for (int y = (int) (time % 3); y < h; y += 3) g.fill(0, y, w, y + 1, 0x22000000);
            int specks = flipTicks > 0 ? 2200 : viewing ? 140 : 900;
            for (int i = 0; i < specks; i++) {
                int x = NOISE.nextInt(w), y = NOISE.nextInt(h), s = 1 + NOISE.nextInt(2);
                int v = 120 + NOISE.nextInt(136);
                g.fill(x, y, x + s, y + s, (flipTicks > 0 ? 0xC0000000 : 0x50000000) | (v << 16) | (v << 8) | v);
            }
            if (flipTicks > 0) g.fill(0, 0, w, h, ((flipTicks * 30) << 24) | 0xFFFFFF);
            // frame corners
            int c = 0xCCFFFFFF;
            g.fill(10, 10, 40, 12, c); g.fill(10, 10, 12, 40, c);
            g.fill(w - 40, 10, w - 10, 12, c); g.fill(w - 12, 10, w - 10, 40, c);
            g.fill(10, h - 12, 40, h - 10, c); g.fill(10, h - 40, 12, h - 10, c);
            g.fill(w - 40, h - 12, w - 10, h - 10, c); g.fill(w - 12, h - 40, w - 10, h - 10, c);
            g.text(mc.font, camLabel, 20, 20, 0xFFFFFFFF, true);
            if (viewing) {
                if ((time / 10) % 2 == 0) g.fill(w - 58, 21, w - 52, 27, 0xFFE01010);
                g.text(mc.font, "REC", w - 48, 20, 0xFFFFFFFF, true);
                g.centeredText(mc.font, "◀ " + mc.options.keyLeft.getTranslatedKeyMessage().getString() + "    "
                        + (camIndex % Math.max(1, camCount) + 1) + " / " + camCount + "    "
                        + mc.options.keyRight.getTranslatedKeyMessage().getString() + " ▶", w / 2, h - 30, 0xFFFFFFFF);
            }
        }

        if (p.getVehicle() instanceof AnimatronicEntity a) {
            g.centeredText(mc.font, "You are " + a.character.displayName + ". Hunt the night guard. Sneak to bail out.", w / 2, h - 62, 0xFFFF5555);
        }

        if (scareTicks > 0) {
            int alpha = Math.min(200, scareTicks * 14);
            g.fill(0, 0, w, h, (alpha << 24) | 0x600000);
            for (int i = 0; i < 600; i++) {
                int x = NOISE.nextInt(w), y = NOISE.nextInt(h);
                g.fill(x, y, x + 2, y + 2, 0x90FFFFFF);
            }
        }
    }
}
