package de.mikucompanion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import de.mikucompanion.companion.CompanionController;
import de.mikucompanion.config.CompanionConfig;
import de.mikucompanion.input.CompanionKeys;
import de.mikucompanion.render.CompanionRenderer;

/** Einstiegspunkt der rein clientseitigen Mod. */
public final class MikuCompanionClient implements ClientModInitializer {
	public static final String MOD_ID = "mikucompanion";
	public static final Logger LOGGER = LoggerFactory.getLogger("MikuCompanion");

	private static boolean tickErrorLogged = false;

	@Override
	public void onInitializeClient() {
		CompanionConfig.load();
		CompanionKeys.register();
		CompanionRenderer.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			try {
				CompanionController.get().tick(client);
			} catch (RuntimeException e) {
				// Ein Fehler in der Begleiterin darf niemals das Spiel zum Absturz bringen.
				if (!tickErrorLogged) {
					tickErrorLogged = true;
					LOGGER.error("Fehler in der Miku-Logik. Miku wird zurueckgesetzt. Bitte melde diesen Fehler samt Log.", e);
				}
				CompanionController.get().emergencyReset();
			}
		});

		LOGGER.info("Miku Companion (rein clientseitig) wurde initialisiert.");
	}
}
