package de.mikucompanion.companion;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Nur lesende Abfragen der (clientseitigen) Welt. Veraendert niemals Bloecke.
 * Vereinfachung: Halbbloecke/Treppen werden wie ganze Bloecke behandelt.
 */
final class WorldProbe {
	private WorldProbe() {
	}

	static boolean isHazard(BlockState s) {
		return s.is(Blocks.LAVA) || s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE) || s.is(Blocks.MAGMA_BLOCK)
				|| s.is(Blocks.CACTUS) || s.is(Blocks.CAMPFIRE) || s.is(Blocks.SOUL_CAMPFIRE)
				|| s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.WITHER_ROSE) || s.is(Blocks.POWDER_SNOW)
				|| s.is(Blocks.COBWEB);
	}

	/** Zelle ist frei begehbar (keine Kollision, kein Wasser/Lava, keine Gefahr). Offene Tueren/Tore gelten als frei. */
	static boolean passable(Level level, int x, int y, int z) {
		BlockPos pos = new BlockPos(x, y, z);
		BlockState s = level.getBlockState(pos);
		if (isHazard(s)) {
			return false;
		}
		if (!s.getFluidState().isEmpty()) {
			return false;
		}
		if (s.hasProperty(BlockStateProperties.OPEN)) {
			return s.getValue(BlockStateProperties.OPEN);
		}
		return s.getCollisionShape(level, pos).isEmpty();
	}

	/** Block kann als Boden dienen. */
	static boolean solidGround(Level level, int x, int y, int z) {
		BlockPos pos = new BlockPos(x, y, z);
		BlockState s = level.getBlockState(pos);
		if (isHazard(s) || s.hasProperty(BlockStateProperties.OPEN)) {
			return false;
		}
		return !s.getCollisionShape(level, pos).isEmpty();
	}

	/**
	 * Berechnet die neue Standhoehe, wenn die Figur auf (nx, nz) steht. NaN = nicht moeglich.
	 * Erlaubt: eben laufen, bis 3 Bloecke herabsteigen, 1 Block hochsteigen.
	 */
	static double resolveStep(Level level, double nx, double nz, double curY, double curX, double curZ) {
		int cy = Mth.floor(curY + 0.001);
		int bx = Mth.floor(nx);
		int bz = Mth.floor(nz);

		if (passable(level, bx, cy, bz) && passable(level, bx, cy + 1, bz)) {
			for (int k = 0; k <= 3; k++) {
				if (solidGround(level, bx, cy - k - 1, bz)) {
					return cy - k;
				}
				if (!passable(level, bx, cy - k - 1, bz)) {
					return Double.NaN;
				}
			}
			return Double.NaN;
		}

		int cbx = Mth.floor(curX);
		int cbz = Mth.floor(curZ);
		if (solidGround(level, bx, cy, bz)
				&& passable(level, bx, cy + 1, bz)
				&& passable(level, bx, cy + 2, bz)
				&& passable(level, cbx, cy + 2, cbz)) {
			return cy + 1;
		}
		return Double.NaN;
	}

	/** Naechster sicherer Standplatz in der Spalte (x, z) nahe fromY, sonst Integer.MIN_VALUE. */
	static int nearestGround(Level level, int x, int z, int fromY) {
		int[] offsets = {0, 1, -1, 2, -2, 3, -3, -4};
		for (int off : offsets) {
			int y = fromY + off;
			if (passable(level, x, y, z) && passable(level, x, y + 1, z) && solidGround(level, x, y - 1, z)) {
				return y;
			}
		}
		return Integer.MIN_VALUE;
	}

	/** Sicherer Platz hinter/neben dem Spieler oder null. Rueckgabe: {x, y, z}. */
	static double[] findSafeSpot(Level level, double px, double py, double pz, float playerYawDeg) {
		double yaw = Math.toRadians(playerYawDeg);
		double[] radii = {2.5, 3.5, 2.0, 4.5};
		for (double r : radii) {
			for (int i = 0; i < 12; i++) {
				int step = (i + 1) / 2;
				double angle = yaw + (i % 2 == 0 ? 1 : -1) * step * Math.toRadians(30);
				double cx = px + Math.sin(angle) * r;
				double cz = pz - Math.cos(angle) * r;
				int gy = nearestGround(level, Mth.floor(cx), Mth.floor(cz), Mth.floor(py));
				if (gy != Integer.MIN_VALUE) {
					return new double[] {Mth.floor(cx) + 0.5, gy, Mth.floor(cz) + 0.5};
				}
			}
		}
		return null;
	}
}
