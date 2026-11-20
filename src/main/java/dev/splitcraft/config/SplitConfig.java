package dev.splitcraft.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;

/** Persisted settings (config/splitcraft.json). */
public final class SplitConfig {
    public enum Orientation {
        HORIZONTAL, // stacked: P1 top, P2 bottom
        VERTICAL;   // side by side: P1 left, P2 right
        public Orientation toggled() { return this == HORIZONTAL ? VERTICAL : HORIZONTAL; }
    }
    public enum RenderMode {
        RESIZE, // each view is rendered at its own (half) size: full field of view. Best looking, uses more engine tricks.
        CROP;   // each view is rendered full-window size and the centre is cropped: safest, trims the edges (bottom HUD in horizontal)
        public RenderMode toggled() { return this == RESIZE ? CROP : RESIZE; }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("splitcraft.json");
    private static Orientation orientation = Orientation.VERTICAL;
    private static RenderMode renderMode = RenderMode.RESIZE;
    private static String player2Name = "Player2";

    public static Orientation orientation() { return orientation; }
    public static RenderMode renderMode() { return renderMode; }
    public static String player2Name() { return player2Name; }
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
        } catch (Exception e) { /* keep defaults */ }
    }
    public static void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("orientation", orientation.name());
            o.addProperty("renderMode", renderMode.name());
            o.addProperty("player2Name", player2Name);
            Files.writeString(FILE, GSON.toJson(o));
        } catch (Exception ignored) {}
    }
    private SplitConfig() {}
}
