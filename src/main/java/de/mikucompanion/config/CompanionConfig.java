package de.mikucompanion.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

import de.mikucompanion.MikuCompanionClient;

/**
 * Minimale lokale Einstellungen (Phase 1). Die Datei liegt im Config-Ordner des Spiels:
 * config/mikucompanion.properties. Es werden keine anderen Dateien veraendert.
 */
public final class CompanionConfig {
	private static final String FILE_NAME = "mikucompanion.properties";
	private static boolean introShown = false;

	private CompanionConfig() {
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	public static void load() {
		Path path = file();
		if (!Files.exists(path)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(path)) {
			Properties props = new Properties();
			props.load(reader);
			introShown = Boolean.parseBoolean(props.getProperty("introShown", "false"));
		} catch (IOException | RuntimeException e) {
			MikuCompanionClient.LOGGER.warn("Konfiguration konnte nicht gelesen werden, Standardwerte werden verwendet.", e);
		}
	}

	public static void save() {
		Path path = file();
		try {
			Files.createDirectories(path.getParent());
			Properties props = new Properties();
			props.setProperty("introShown", Boolean.toString(introShown));
			try (Writer writer = Files.newBufferedWriter(path)) {
				props.store(writer, "Miku Companion - lokale Einstellungen");
			}
		} catch (IOException | RuntimeException e) {
			MikuCompanionClient.LOGGER.warn("Konfiguration konnte nicht gespeichert werden.", e);
		}
	}

	public static boolean isIntroShown() {
		return introShown;
	}

	public static void setIntroShown(boolean value) {
		introShown = value;
		save();
	}
}
