package de.mikucompanion.input;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

import de.mikucompanion.MikuCompanionClient;

/**
 * Konfigurierbare Tasten (Optionen > Steuerung > "Miku Companion").
 * Standardtasten: K = herbeirufen, G = Warten/Folgen, H = wegschicken.
 * Alle drei lassen sich im Spiel frei umbelegen.
 */
public final class CompanionKeys {
	public static KeyMapping SUMMON;
	public static KeyMapping TOGGLE_WAIT;
	public static KeyMapping DISMISS;

	private CompanionKeys() {
	}

	public static void register() {
		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(MikuCompanionClient.MOD_ID, "main"));

		SUMMON = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.mikucompanion.summon", InputConstants.Type.KEYSYM, InputConstants.KEY_K, category));
		TOGGLE_WAIT = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.mikucompanion.toggle_wait", InputConstants.Type.KEYSYM, InputConstants.KEY_G, category));
		DISMISS = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.mikucompanion.dismiss", InputConstants.Type.KEYSYM, InputConstants.KEY_H, category));
	}
}
