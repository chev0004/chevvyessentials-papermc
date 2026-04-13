package com.chevvy.state;

import com.chevvy.ChevvyEssentials;
import com.chevvy.DeathLocation;
import com.chevvy.Vec3d;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DeathState {

    private static final Map<UUID, DeathLocation> deathLocations = new ConcurrentHashMap<>();

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(DeathLocation.class, (JsonSerializer<DeathLocation>) (deathLocation, type, ctx) -> {
                JsonObject obj = new JsonObject();
                JsonObject posObj = new JsonObject();
                posObj.addProperty("x", deathLocation.pos().x());
                posObj.addProperty("y", deathLocation.pos().y());
                posObj.addProperty("z", deathLocation.pos().z());
                obj.add("pos", posObj);
                obj.addProperty("dimension", deathLocation.dimension());
                return obj;
            })
            .registerTypeAdapter(DeathLocation.class, (JsonDeserializer<DeathLocation>) (json, type, ctx) -> {
                JsonObject obj = json.getAsJsonObject();
                JsonObject posObj = obj.getAsJsonObject("pos");
                Vec3d pos = new Vec3d(
                        posObj.get("x").getAsDouble(),
                        posObj.get("y").getAsDouble(),
                        posObj.get("z").getAsDouble()
                );
                String dimension = obj.get("dimension").getAsString();
                return new DeathLocation(pos, dimension);
            })
            .setPrettyPrinting()
            .create();

    private static final Type DEATH_LOCATIONS_TYPE =
            new TypeToken<HashMap<UUID, DeathLocation>>() {}.getType();

    private static Path stateFile;

    public static void initialize() {
        Path configDir = Paths.get("config", ChevvyEssentials.PLUGIN_ID);
        stateFile = configDir.resolve("deaths.json");

        try {
            Files.createDirectories(configDir);
            if (Files.exists(stateFile)) {
                try (FileReader reader = new FileReader(stateFile.toFile())) {
                    Map<UUID, DeathLocation> loadedLocations = GSON.fromJson(reader, DEATH_LOCATIONS_TYPE);
                    if (loadedLocations != null) {
                        deathLocations.clear();
                        deathLocations.putAll(new ConcurrentHashMap<>(loadedLocations));
                    }
                }
                ChevvyEssentials.LOGGER.info("Death locations loaded successfully.");
            }
        } catch (IOException e) {
            ChevvyEssentials.LOGGER.error("Failed to load death locations data", e);
        }
    }

    public static DeathLocation getDeathLocation(UUID playerUuid) {
        return deathLocations.get(playerUuid);
    }

    public static void setDeathLocation(UUID playerUuid, DeathLocation location) {
        deathLocations.put(playerUuid, location);
        save();
    }

    public static void save() {
        if (stateFile == null) {
            return;
        }
        try (FileWriter writer = new FileWriter(stateFile.toFile())) {
            GSON.toJson(deathLocations, writer);
        } catch (IOException e) {
            ChevvyEssentials.LOGGER.error("Failed to save death locations data", e);
        }
    }
}
