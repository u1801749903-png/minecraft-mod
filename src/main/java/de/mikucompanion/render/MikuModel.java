package de.mikucompanion.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import de.mikucompanion.companion.CompanionController.Snapshot;

/**
 * Selbst erstelltes, von Miku Nakano INSPIRIERTES Modell aus einfachen Quadern (keine Texturen,
 * keine fremden Assets). 1 Einheit im Code = 1 "Pixel" = U Bloecke. Gesamthoehe ca. 1,75 Bloecke.
 * Ursprung: Mitte zwischen den Fuessen am Boden, Blickrichtung +Z.
 */
final class MikuModel {
	private static final float U = 0.046f;
	private static final float LIGHT = 0.92f;

	private static final int SKIN = 0xFFE3D3;
	private static final int HAIR = 0xA85F36;
	private static final int NAVY = 0x1B2A63;
	private static final int BLOUSE = 0xF4F1EA;
	private static final int SKIRT = 0x33405E;
	private static final int STOCKING = 0x24242C;
	private static final int SHOE = 0x4A2E24;
	private static final int RIBBON = 0xB3404F;
	private static final int EYE_WHITE = 0xF4F6FF;
	private static final int IRIS = 0x3C78D8;
	private static final int PUPIL = 0x14233F;
	private static final int LASH = 0x5A3320;
	private static final int MOUTH = 0xD98585;
	private static final int BLUSH = 0xFFA9B0;

	private MikuModel() {
	}

	static void build(BufferBuilder b, Matrix4f base, Snapshot s) {
		float swing = (float) Math.sin(s.walkPhase()) * 0.9f * s.walkAmount();
		float bob = (float) (Math.abs(Math.cos(s.walkPhase())) * 0.5 * s.walkAmount() + Math.sin(s.anim() * 0.08) * 0.12);
		Matrix4f root = new Matrix4f(base).translate(0, bob * U, 0);

		// Beine (Struempfe + Schuhe)
		for (int side = -1; side <= 1; side += 2) {
			Matrix4f leg = new Matrix4f(root).translate(side * 2.3f * U, 13f * U, 0).rotateX(side * swing * 0.8f);
			box(b, leg, -1.8f, -11.4f, -1.8f, 1.8f, 0f, 1.8f, STOCKING);
			box(b, leg, -2.0f, -13f, -2.0f, 2.0f, -11.4f, 3.4f, SHOE);
		}

		// Rock, Oberkoerper, Schleife, Hals
		box(b, root, -5.2f, 11f, -3.2f, 5.2f, 16.2f, 3.2f, SKIRT);
		box(b, root, -4f, 16f, -2.4f, 4f, 26f, 2.4f, BLOUSE);
		box(b, root, -1.6f, 21.5f, 2.4f, 1.6f, 23.5f, 2.9f, RIBBON);
		box(b, root, -1.5f, 26f, -1.5f, 1.5f, 27.2f, 1.5f, SKIN);

		// Arme (gegengleich zu den Beinen)
		for (int side = -1; side <= 1; side += 2) {
			Matrix4f arm = new Matrix4f(root).translate(side * 5.4f * U, 25f * U, 0)
					.rotateX(-side * swing * 0.7f - 0.1f).rotateZ(side * 0.07f);
			box(b, arm, -1.4f, -8f, -1.5f, 1.4f, 1f, 1.5f, BLOUSE);
			box(b, arm, -1.2f, -11f, -1.2f, 1.2f, -8f, 1.2f, SKIN);
		}

		// Kopfhoerer um den Hals (dunkelblau)
		Matrix4f neck = new Matrix4f(root).translate(0, 27f * U, 0);
		box(b, neck, -4.4f, -1.6f, -3.4f, 4.4f, 0.8f, -2.0f, NAVY);
		for (int side = -1; side <= 1; side += 2) {
			float x0 = side > 0 ? 4.2f : -6.2f;
			float x1 = side > 0 ? 6.2f : -4.2f;
			box(b, neck, x0, -2.4f, -2.6f, x1, 1.6f, 1.2f, NAVY);
		}

		// Kopf mit Blickrichtung
		Matrix4f head = new Matrix4f(neck).rotateY(s.headYaw()).rotateX(s.headPitch());
		box(b, head, -4.8f, 0f, -4.4f, 4.8f, 10f, 4.4f, SKIN);

		// Haare: lang, braun-orange
		box(b, head, -5.6f, -14f, -6.6f, 5.6f, 11f, -3.2f, HAIR);
		box(b, head, -5.4f, 9f, -5.4f, 5.4f, 11.6f, 5.0f, HAIR);
		box(b, head, -5.9f, -10f, -4.8f, -4.9f, 10f, 3.4f, HAIR);
		box(b, head, 4.9f, -10f, -4.8f, 5.9f, 10f, 3.4f, HAIR);
		box(b, head, -5.0f, 6.2f, 4.3f, -1.2f, 10f, 5.3f, HAIR);
		box(b, head, -1.2f, 7.4f, 4.3f, 1.2f, 10f, 5.3f, HAIR);
		box(b, head, 1.2f, 6.2f, 4.3f, 5.0f, 10f, 5.3f, HAIR);

		// Gesicht
		for (int side = -1; side <= 1; side += 2) {
			float cx = side * 2.4f;
			if (s.blink()) {
				box(b, head, cx - 1.2f, 4.4f, 4.40f, cx + 1.2f, 4.9f, 4.55f, LASH);
			} else {
				box(b, head, cx - 1.2f, 3.4f, 4.40f, cx + 1.2f, 5.8f, 4.55f, EYE_WHITE);
				box(b, head, cx - 0.8f, 3.6f, 4.55f, cx + 0.8f, 5.6f, 4.70f, IRIS);
				box(b, head, cx - 0.4f, 4.0f, 4.70f, cx + 0.4f, 5.0f, 4.80f, PUPIL);
				box(b, head, cx - 0.5f, 4.8f, 4.80f, cx - 0.0f, 5.4f, 4.88f, EYE_WHITE);
				box(b, head, cx - 1.3f, 5.8f, 4.40f, cx + 1.3f, 6.1f, 4.58f, LASH);
			}
			if (s.blush()) {
				box(b, head, side * 3.4f - 0.8f, 2.2f, 4.40f, side * 3.4f + 0.8f, 3.2f, 4.50f, BLUSH);
			}
		}
		box(b, head, -0.9f, 1.4f, 4.40f, 0.9f, 1.8f, 4.52f, MOUTH);
	}

	private static void box(BufferBuilder b, Matrix4fc m, float x0, float y0, float z0, float x1, float y1, float z1, int rgb) {
		float minX = x0 * U, minY = y0 * U, minZ = z0 * U;
		float maxX = x1 * U, maxY = y1 * U, maxZ = z1 * U;
		float r = ((rgb >> 16) & 0xFF) / 255f * LIGHT;
		float g = ((rgb >> 8) & 0xFF) / 255f * LIGHT;
		float bl = (rgb & 0xFF) / 255f * LIGHT;

		// Reihenfolge der Ecken wie im offiziellen Fabric-Beispiel (gleiche Flaechenausrichtung)
		face(b, m, r, g, bl, 0.88f, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ); // vorne
		face(b, m, r, g, bl, 0.72f, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ); // hinten
		face(b, m, r, g, bl, 0.80f, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ); // links
		face(b, m, r, g, bl, 0.80f, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ); // rechts
		face(b, m, r, g, bl, 1.00f, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ); // oben
		face(b, m, r, g, bl, 0.55f, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ); // unten
	}

	private static void face(BufferBuilder b, Matrix4fc m, float r, float g, float bl, float shade,
			float ax, float ay, float az, float bx, float by, float bz,
			float cx, float cy, float cz, float dx, float dy, float dz) {
		float rr = r * shade;
		float gg = g * shade;
		float bb = bl * shade;
		b.addVertex(m, ax, ay, az).setColor(rr, gg, bb, 1f);
		b.addVertex(m, bx, by, bz).setColor(rr, gg, bb, 1f);
		b.addVertex(m, cx, cy, cz).setColor(rr, gg, bb, 1f);
		b.addVertex(m, dx, dy, dz).setColor(rr, gg, bb, 1f);
	}
}
