package org.crebs.skpackets.api.update;

import ch.njol.skript.util.Version;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ModrinthVersion {

    private final Version updateVersion;
    private final String versionId;
    private final List<Version> supportedVersions = new ArrayList<>();

    public ModrinthVersion(JsonElement jsonElement) {
        JsonObject json = jsonElement.getAsJsonObject();
        this.versionId = json.get("id").getAsString();
        this.updateVersion = new Version(json.get("version_number").getAsString().trim().replaceFirst("^[vV]", ""));
        JsonArray gameVersions = json.getAsJsonArray("game_versions");
        gameVersions.forEach(version -> this.supportedVersions.add(new Version(version.getAsString())));
    }

    public Version getUpdateVersion() {
        return this.updateVersion;
    }

    public String getUpdateLink() {
        return "https://modrinth.com/plugin/skpackets/version/" + this.versionId;
    }

    public List<Version> getSupportedVersions() {
        return this.supportedVersions;
    }

    public boolean isServerSupported(Version serverVersion) {
        return this.supportedVersions.contains(serverVersion);
    }

}
