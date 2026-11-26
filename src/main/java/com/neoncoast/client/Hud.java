package com.neoncoast.client;

import com.neoncoast.vehicle.CarEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/** Wanted stars and cash (top right), delivery directions (top middle), car gauges (bottom right). */
public final class Hud {
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    private Hud() {
    }

    public static String format(long v) {
        return v < 0 ? "..." : String.format("%,d", v);
    }

    public static void render(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        Font tr = mc.font;
        int w = ctx.guiWidth(), h = ctx.guiHeight();

        // wanted level: 5 stars, flashing while you're out of sight of the cops
        boolean flashOff = ClientState.evading && (mc.player.tickCount / 6) % 2 == 0;
        for (int i = 0; i < 5; i++) {
            boolean lit = i < ClientState.stars && !flashOff;
            int color = lit ? 0xFFFFE040 : 0x50FFFFFF;
            ctx.text(tr, "★", w - 14 - (4 - i) * 11, 6, color);
        }
        if (ClientState.money >= 0) {
            String cash = "$" + format(ClientState.money);
            ctx.text(tr, cash, w - 6 - tr.width(cash), 18, 0xFF55FF55);
        }

        // delivery job
        if (ClientState.jobActive) {
            double dx = ClientState.jobTarget.getX() + 0.5 - mc.player.getX();
            double dz = ClientState.jobTarget.getZ() + 0.5 - mc.player.getZ();
            int dist = (int) Math.sqrt(dx * dx + dz * dz);
            float bearing = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float rel = Mth.wrapDegrees(bearing - mc.player.getYRot());
            int idx = Math.floorMod(Math.round(rel / 45.0f), 8);
            String text = "DELIVERY  " + ARROWS[idx] + "  " + dist + "m";
            ctx.text(tr, text, (w - tr.width(text)) / 2, 6, 0xFFFFB030);
        }

        // car gauges
        if (mc.player.getVehicle() instanceof CarEntity car) {
            int x = w / 2 + 98, y = h - 46;
            ctx.text(tr, Math.round(car.getSpeedKmh()) + " km/h", x, y, 0xFFFFFFFF);
            ctx.text(tr, "Gear " + car.getGear(), x, y + 10, 0xFFFFD24A);
            float rpm = Mth.clamp(car.getRpm(), 0.0f, 1.0f);
            ctx.fill(x, y + 21, x + 60, y + 26, 0x90000000);
            ctx.fill(x, y + 21, x + Math.round(60 * rpm), y + 26, rpm > 0.9f ? 0xFFFF4040 : rpm > 0.7f ? 0xFFFFC040 : 0xFF40E060);
            float hp = Mth.clamp(car.getCarHealth() / car.getCarType().maxHealth(), 0.0f, 1.0f);
            ctx.text(tr, "CAR", x, y + 30, hp < 0.25f ? 0xFFFF5050 : 0xFFFFFFFF);
            ctx.fill(x + 22, y + 31, x + 60, y + 37, 0x90000000);
            ctx.fill(x + 22, y + 31, x + 22 + Math.round(38 * hp), y + 37, hp < 0.25f ? 0xFFFF4040 : hp < 0.5f ? 0xFFFFC040 : 0xFF40A0FF);
        }
    }
}
