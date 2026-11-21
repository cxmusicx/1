package dev.splitcraft.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;

/** Persisted settings (config/splitcraft.json). */
public final class SplitConfig {
    public enum Orientation {
        HORIZONTAL, VERTICAL;
        public Orientation toggled() { return this == HORIZONTAL ? VERTICAL : HORIZONTAL; }
    }
    public enum RenderMode {
        RESIZE, CROP;
        public RenderMode toggled() { return this == RESIZE ? CROP : RESIZE; }
    }
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("splitcraft.json");
    private static Orientation orientation = Orientation.VERTICAL;
    private static RenderMode renderMode = RenderMode.RESIZE;
    private static String player2Name = "Player2";
    private static float lookSpeed = 200f, lookCurve = 1.8f, lookSensH = 1.0f, lookSensV = 0.9f, deadzone = 0.12f;
    private static boolean toggleSprint = true, toggleSneak = true;
    private static float lookSpeed = 200f, lookCurve = 1.8f, lookSensH = 1.0f, lookSensV = 0.9f, deadzone = 0.12f;
    private static boolean toggleSprint = true, toggleSneak = true;
    public static Orientation orientation() { return orientation; }
    public static RenderMode renderMode() { return renderMode; }
    public static String player2Name() { return player2Name; }
    public static float lookSpeed() { return lookSpeed; }
    public static float lookCurve() { return lookCurve; }
    public static float lookSensH() { return lookSensH; }
    public static float lookSensV() { return lookSensV; }
    public static float deadzone() { return deadzone; }
    public static boolean toggleSprint() { return toggleSprint; }
    public static boolean toggleSneak() { return toggleSneak; }
    public static float lookSpeed() { return lookSpeed; }
    public static float lookCurve() { return lookCurve; }
    public static float lookSensH() { return lookSensH; }
    public static float lookSensV() { return lookSensV; }
    public static float deadzone() { return deadzone; }
    public static boolean toggleSprint() { return toggleSprint; }
    public static boolean toggleSneak() { return toggleSneak; }
    public static void toggleOrientation() { orientation = orientation.toggled(); save(); }
    public static void toggleRenderMode() { renderMode = renderMode.toggled(); save(); }
    public static void setRenderMode(RenderMode m) { renderMode = m; save(); }
    public static void load() {
        try {
            if (!Files.exists(FILE)) { save(); return; }
            JsonObject o = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            if (o.has("orientation")) orientation = Orientation.valueOf(o.get("orientation").getAsString());
            if (o.has("renderMode")) renderMode = RenderMode.valueOf(o.get("renderMode").getAsString());
            if (o.has("player2Name")) player2Name = o.get("player2Name").getAsString();
            if (o.has("lookSpeed")) lookSpeed = o.get("lookSpeed").getAsFloat();
            if (o.has("lookCurve")) lookCurve = o.get("lookCurve").getAsFloat();
            if (o.has("lookSensH")) lookSensH = o.get("lookSensH").getAsFloat();
            if (o.has("lookSensV")) lookSensV = o.get("lookSensV").getAsFloat();
            if (o.has("deadzone")) deadzone = o.get("deadzone").getAsFloat();
            if (o.has("toggleSprint")) toggleSprint = o.get("toggleSprint").getAsBoolean();
            if (o.has("toggleSneak")) toggleSneak = o.get("toggleSneak").getAsBoolean();
        } catch (Exception ignored) {}
    }
    public static void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("orientation", orientation.name()); o.addProperty("renderMode", renderMode.name());
            o.addProperty("player2Name", player2Name); o.addProperty("lookSpeed", lookSpeed);
            o.addProperty("lookCurve", lookCurve); o.addProperty("lookSensH", lookSensH);
            o.addProperty("lookSensV", lookSensV); o.addProperty("deadzone", deadzone);
            o.addProperty("toggleSprint", toggleSprint); o.addProperty("toggleSneak", toggleSneak);
            Files.writeString(FILE, GSON.toJson(o));
        } catch (Exception ignored) {}
    }
    private SplitConfig() {}
}