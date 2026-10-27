package de.mikucompanion.companion;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import de.mikucompanion.MikuCompanionClient;
import de.mikucompanion.config.CompanionConfig;
import de.mikucompanion.input.CompanionKeys;

/**
 * Rein lokale Logik der Begleiterin (keine Server-Entity!). Laeuft einmal pro Client-Tick (20 Hz).
 * Phase 1: Folgen, Warten, Herbeirufen, Wegschicken, kontrollierte Rueckkehr, Welt-/Respawn-Behandlung.
 */
public final class CompanionController {
	public enum Mode { FOLLOW, WAIT }

	public enum State { IDLE, FOLLOW, WAIT, RETURN, RECOVERY }

	/** Unveraenderlicher, interpolierter Zustand fuer den Renderer. */
	public record Snapshot(double x, double y, double z, float theta, float headYaw, float headPitch,
			float walkPhase, float walkAmount, float anim, boolean blink, boolean blush) {
	}

	private static final CompanionController INSTANCE = new CompanionController();

	public static CompanionController get() {
		return INSTANCE;
	}

	// Abstaende und Geschwindigkeiten (Bloecke bzw. Bloecke pro Tick)
	private static final double STOP_DIST = 3.0;
	private static final double START_DIST = 4.6;
	private static final double RETURN_DIST = 10.0;
	private static final double RECOVER_DIST = 32.0;
	private static final double RECOVER_DY = 14.0;
	private static final int RECOVER_COOLDOWN = 100;
	private static final int ACTION_COOLDOWN = 10;

	private boolean active = false;
	private Mode mode = Mode.FOLLOW;
	private State state = State.IDLE;

	private double x, y, z, prevX, prevZ;
	private double visY, prevVisY;
	private double speed;
	private float theta, prevTheta;
	private float headYaw, prevHeadYaw, headPitch, prevHeadPitch;
	private float walkPhase, walkAmount;

	private long tick = 0;
	private long lastTickNanos = System.nanoTime();
	private long actionCooldownUntil = 0;
	private long recoverCooldownUntil = 0;

	private int stuckTicks = 0;
	private int sidestepTicks = 0;
	private float sidestepAngle = 0f;
	private boolean sidestepFlip = false;
	private int recoveryTicks = 0;
	private int blinkTicks = 0;
	private int nextBlinkIn = 60;
	private int blushTicks = 0;
	private int shyTicks = 0;
	private float shySide = 1f;

	private Level lastLevel = null;
	private Player lastPlayer = null;

	private CompanionController() {
	}

	// ------------------------------------------------------------------ Tick

	public void tick(Minecraft mc) {
		Level level = mc.level;
		Player player = mc.player;

		if (level == null || player == null) {
			// Verbindungsabbruch / Welt verlassen: sauber zuruecksetzen
			if (active) {
				MikuCompanionClient.LOGGER.info("Welt beendet - Miku wird zurueckgesetzt.");
			}
			active = false;
			lastLevel = null;
			lastPlayer = null;
			drainKeys();
			return;
		}

		tick++;

		if (lastLevel != level || lastPlayer != player) {
			boolean firstInSession = lastLevel == null;
			lastLevel = level;
			lastPlayer = player;
			if (firstInSession) {
				showIntroOnce(mc);
			} else if (active) {
				// Dimensionswechsel oder Respawn: kontrolliert neben dem Spieler wiederherstellen
				if (!relocate(level, player)) {
					active = false;
				}
			}
		}

		handleKeys(mc, level, player);
		if (active) {
			simulate(level, player);
		}
		lastTickNanos = System.nanoTime();
	}

	/** Wird vom Entrypoint bei unerwarteten Fehlern aufgerufen. */
	public void emergencyReset() {
		active = false;
		state = State.IDLE;
		mode = Mode.FOLLOW;
	}

	private void drainKeys() {
		if (CompanionKeys.SUMMON == null) {
			return;
		}
		while (CompanionKeys.SUMMON.consumeClick()) { }
		while (CompanionKeys.DISMISS.consumeClick()) { }
		while (CompanionKeys.TOGGLE_WAIT.consumeClick()) { }
	}

	// ------------------------------------------------------------------ Tasten

	private void handleKeys(Minecraft mc, Level level, Player player) {
		boolean summon = false;
		boolean dismiss = false;
		boolean toggle = false;
		while (CompanionKeys.SUMMON.consumeClick()) {
			summon = true;
		}
		while (CompanionKeys.DISMISS.consumeClick()) {
			dismiss = true;
		}
		while (CompanionKeys.TOGGLE_WAIT.consumeClick()) {
			toggle = true;
		}
		if (!(summon || dismiss || toggle)) {
			return;
		}
		// Schutz gegen Tastenspam
		if (tick < actionCooldownUntil) {
			return;
		}
		actionCooldownUntil = tick + ACTION_COOLDOWN;

		if (dismiss) {
			if (active) {
				active = false;
				say(mc, "message.mikucompanion.dismiss");
			}
		} else if (summon) {
			if (!active) {
				spawn(mc, level, player);
			} else {
				mode = Mode.FOLLOW;
				double d = Math.hypot(player.getX() - x, player.getZ() - z);
				if (d > 8.0) {
					relocate(level, player);
				}
				say(mc, "message.mikucompanion.callover");
			}
		} else {
			if (!active) {
				say(mc, "message.mikucompanion.not_here", CompanionKeys.SUMMON.getTranslatedKeyMessage());
			} else if (mode == Mode.FOLLOW) {
				mode = Mode.WAIT;
				say(mc, "message.mikucompanion.wait");
			} else {
				mode = Mode.FOLLOW;
				say(mc, "message.mikucompanion.follow");
			}
		}
	}

	private void showIntroOnce(Minecraft mc) {
		if (CompanionConfig.isIntroShown() || mc.player == null) {
			return;
		}
		mc.player.sendSystemMessage(Component.translatable("message.mikucompanion.intro",
				CompanionKeys.SUMMON.getTranslatedKeyMessage(),
				CompanionKeys.TOGGLE_WAIT.getTranslatedKeyMessage(),
				CompanionKeys.DISMISS.getTranslatedKeyMessage()));
		CompanionConfig.setIntroShown(true);
	}

	private void say(Minecraft mc, String key, Object... args) {
		if (mc.player != null) {
			mc.player.sendSystemMessage(Component.translatable(key, args));
		}
	}

	// ------------------------------------------------------------------ Spawn / Relocate

	private void spawn(Minecraft mc, Level level, Player player) {
		double[] spot = WorldProbe.findSafeSpot(level, player.getX(), player.getY(), player.getZ(), player.getYRot());
		if (spot == null) {
			say(mc, "message.mikucompanion.no_safe_spot");
			return;
		}
		active = true;
		mode = Mode.FOLLOW;
		state = State.IDLE;
		placeAt(spot, player);
		blushTicks = 80;
		shyTicks = 40;
		shySide = ThreadLocalRandom.current().nextBoolean() ? 1f : -1f;
		say(mc, ThreadLocalRandom.current().nextBoolean()
				? "message.mikucompanion.summon.1" : "message.mikucompanion.summon.2");
	}

	/** Teleportiert kontrolliert an einen sicheren Platz neben dem Spieler. */
	private boolean relocate(Level level, Player player) {
		double[] spot = WorldProbe.findSafeSpot(level, player.getX(), player.getY(), player.getZ(), player.getYRot());
		if (spot == null) {
			return false;
		}
		placeAt(spot, player);
		state = State.RECOVERY;
		recoveryTicks = 15;
		recoverCooldownUntil = tick + RECOVER_COOLDOWN;
		return true;
	}

	private void placeAt(double[] spot, Player player) {
		x = spot[0];
		y = spot[1];
		z = spot[2];
		prevX = x;
		prevZ = z;
		visY = y;
		prevVisY = y;
		speed = 0;
		stuckTicks = 0;
		sidestepTicks = 0;
		theta = (float) Math.atan2(player.getX() - x, player.getZ() - z);
		prevTheta = theta;
		headYaw = 0;
		prevHeadYaw = 0;
		headPitch = 0;
		prevHeadPitch = 0;
		walkAmount = 0;
	}

	// ------------------------------------------------------------------ Simulation

	private void simulate(Level level, Player player) {
		prevX = x;
		prevZ = z;
		prevVisY = visY;
		prevTheta = theta;
		prevHeadYaw = headYaw;
		prevHeadPitch = headPitch;

		double px = player.getX();
		double py = player.getY();
		double pz = player.getZ();
		double dx = px - x;
		double dz = pz - z;
		double d = Math.hypot(dx, dz);
		boolean canRecover = tick >= recoverCooldownUntil;

		// 1) Zu weit weg -> kontrollierte Rueckkehr per Teleport (nur im Folgen-Modus)
		if (mode == Mode.FOLLOW && canRecover && (d > RECOVER_DIST || Math.abs(py - y) > RECOVER_DY)) {
			if (relocate(level, player)) {
				return;
			}
		}

		// 2) Boden pruefen (Schwerkraft / Block entfernt / Block platziert)
		double gy = WorldProbe.resolveStep(level, x, z, y, x, z);
		if (Double.isNaN(gy)) {
			if (canRecover && relocate(level, player)) {
				return;
			}
		} else {
			y = gy;
		}

		// 3) Zustand und Zielgeschwindigkeit
		double targetSpeed = 0;
		if (mode == Mode.WAIT) {
			state = State.WAIT;
		} else if (recoveryTicks > 0) {
			state = State.RECOVERY;
			recoveryTicks--;
		} else if (d > RETURN_DIST) {
			state = State.RETURN;
			targetSpeed = 0.30;
		} else if (d > START_DIST || (state == State.FOLLOW && d > STOP_DIST)) {
			state = State.FOLLOW;
			targetSpeed = Mth.clamp(0.12 + (d - STOP_DIST) * 0.03, 0.12, 0.27);
		} else {
			state = State.IDLE;
		}
		speed += (targetSpeed - speed) * 0.25;
		if (targetSpeed == 0 && speed < 0.01) {
			speed = 0;
		}

		// 4) Bewegung
		if (speed > 0.005 && d > 1.0E-4) {
			double dirX = dx / d;
			double dirZ = dz / d;
			if (sidestepTicks > 0) {
				double c = Math.cos(sidestepAngle);
				double s = Math.sin(sidestepAngle);
				double rx = dirX * c - dirZ * s;
				double rz = dirX * s + dirZ * c;
				dirX = rx;
				dirZ = rz;
				sidestepTicks--;
			}
			moveHorizontally(level, dirX * speed, dirZ * speed);
		}
		double movedDist = Math.hypot(x - prevX, z - prevZ);

		// 5) Haengt sie fest? -> seitlich ausweichen, danach notfalls Teleport
		if (targetSpeed > 0.05 && movedDist < 0.015) {
			stuckTicks++;
		} else {
			stuckTicks = Math.max(0, stuckTicks - 2);
		}
		if (stuckTicks > 25 && sidestepTicks == 0) {
			sidestepTicks = 25;
			sidestepAngle = sidestepFlip ? 1.0f : -1.0f;
			sidestepFlip = !sidestepFlip;
		}
		if (stuckTicks > 90 && canRecover) {
			stuckTicks = 0;
			if (relocate(level, player)) {
				return;
			}
		}

		// 6) Ausrichtung des Koerpers
		double lookYaw = Math.atan2(dx, dz);
		float rel = wrapAngle((float) lookYaw - theta);
		float desired;
		if (movedDist > 0.01) {
			desired = (float) Math.atan2(x - prevX, z - prevZ);
		} else if (Math.abs(rel) > 0.9f && d > 0.8) {
			desired = (float) lookYaw;
		} else {
			desired = theta;
		}
		float maxTurn = movedDist > 0.01 ? 0.3f : 0.08f;
		theta += Mth.clamp(wrapAngle(desired - theta), -maxTurn, maxTurn);

		// 7) Kopf: schaut zum Spieler, wirkt bei Verlegenheit weg
		float targetHeadYaw = Mth.clamp(wrapAngle((float) lookYaw - theta), -1.1f, 1.1f);
		float targetPitch = Mth.clamp((float) Math.atan2(y + 1.55 - player.getEyeY(), Math.max(d, 0.5)), -0.6f, 0.6f);
		if (shyTicks > 0) {
			targetHeadYaw = shySide * 0.6f;
			targetPitch = 0.3f;
			shyTicks--;
		}
		headYaw += (targetHeadYaw - headYaw) * 0.25f;
		headPitch += (targetPitch - headPitch) * 0.25f;

		// 8) Kleine Animationen: Laufphase, Blinzeln, Erroeten
		walkPhase += (float) (movedDist * 2.2);
		float moving = (float) Math.min(1.0, movedDist / 0.18);
		walkAmount += (moving - walkAmount) * 0.3f;
		if (blushTicks > 0) {
			blushTicks--;
		}
		if (blinkTicks > 0) {
			blinkTicks--;
		} else if (--nextBlinkIn <= 0) {
			blinkTicks = 4;
			nextBlinkIn = 60 + ThreadLocalRandom.current().nextInt(80);
		}

		// 9) Optische Hoehenglaettung (Stufen/Faelle ruckeln nicht)
		visY += (y - visY) * 0.4;
		if (Math.abs(y - visY) > 3.0) {
			visY = y;
		}
	}

	private void moveHorizontally(Level level, double vx, double vz) {
		if (tryStep(level, x + vx, z + vz, vx, vz)) {
			return;
		}
		// An Waenden entlanggleiten
		if (Math.abs(vx) > 1.0E-4 && tryStep(level, x + vx, z, vx, 0)) {
			return;
		}
		if (Math.abs(vz) > 1.0E-4) {
			tryStep(level, x, z + vz, 0, vz);
		}
	}

	private boolean tryStep(Level level, double nx, double nz, double dirX, double dirZ) {
		double ny = WorldProbe.resolveStep(level, nx, nz, y, x, z);
		if (Double.isNaN(ny)) {
			return false;
		}
		double len = Math.hypot(dirX, dirZ);
		if (len > 1.0E-6) {
			double lx = nx + dirX / len * 0.3;
			double lz = nz + dirZ / len * 0.3;
			if (Mth.floor(lx) != Mth.floor(nx) || Mth.floor(lz) != Mth.floor(nz)) {
				double ly = WorldProbe.resolveStep(level, lx, lz, y, x, z);
				if (Double.isNaN(ly) || Math.abs(ly - ny) > 1.01) {
					return false;
				}
			}
		}
		x = nx;
		z = nz;
		y = ny;
		return true;
	}

	private static float wrapAngle(float a) {
		double twoPi = Math.PI * 2.0;
		double r = a % twoPi;
		if (r > Math.PI) {
			r -= twoPi;
		} else if (r < -Math.PI) {
			r += twoPi;
		}
		return (float) r;
	}

	// ------------------------------------------------------------------ Renderer-Zugriff

	public Snapshot snapshot() {
		if (!active) {
			return null;
		}
		float a = (float) Math.min(1.0, Math.max(0.0, (System.nanoTime() - lastTickNanos) / 50_000_000.0));
		return new Snapshot(
				prevX + (x - prevX) * a,
				prevVisY + (visY - prevVisY) * a,
				prevZ + (z - prevZ) * a,
				prevTheta + (theta - prevTheta) * a,
				prevHeadYaw + (headYaw - prevHeadYaw) * a,
				prevHeadPitch + (headPitch - prevHeadPitch) * a,
				walkPhase,
				walkAmount,
				tick + a,
				blinkTicks > 0,
				blushTicks > 0);
	}

	public boolean isActive() {
		return active;
	}

	public State getState() {
		return state;
	}

	public Mode getMode() {
		return mode;
	}
}
