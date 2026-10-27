package de.mikucompanion.render;

import java.util.OptionalDouble;
import java.util.OptionalInt;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import de.mikucompanion.MikuCompanionClient;
import de.mikucompanion.companion.CompanionController;

/**
 * Zeichnet die Figur als echtes 3D-Objekt in der Spielwelt (nicht im HUD).
 * Vorlage: offizielle Fabric-Dokumentation "Rendering in the World" fuer 26.1.2.
 * Im Unterschied zum Doku-Beispiel bleibt der Tiefentest AKTIV, damit Miku hinter Bloecken verschwindet.
 */
public final class CompanionRenderer {
	private static final RenderPipeline PIPELINE = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
					.withLocation(Identifier.fromNamespaceAndPath(MikuCompanionClient.MOD_ID, "pipeline/miku_filled"))
					.build());

	private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
	private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
	private static final Vector3f MODEL_OFFSET = new Vector3f();
	private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

	private static BufferBuilder buffer;
	private static MappableRingBuffer vertexBuffer;
	private static boolean broken = false;

	private CompanionRenderer() {
	}

	public static void register() {
		LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(CompanionRenderer::renderAndDraw);
	}

	private static void renderAndDraw(LevelRenderContext context) {
		if (broken) {
			return;
		}
		CompanionController.Snapshot snapshot = CompanionController.get().snapshot();
		if (snapshot == null) {
			return;
		}
		try {
			Vec3 camera = context.levelState().cameraRenderState.pos;
			Matrix4f base = new Matrix4f(context.poseStack().last().pose())
					.translate((float) (snapshot.x() - camera.x),
							(float) (snapshot.y() - camera.y),
							(float) (snapshot.z() - camera.z))
					.rotateY(snapshot.theta());

			if (buffer == null) {
				buffer = new BufferBuilder(ALLOCATOR, PIPELINE.getVertexFormatMode(), PIPELINE.getVertexFormat());
			}
			MikuModel.build(buffer, base, snapshot);
			draw(Minecraft.getInstance());
		} catch (RuntimeException e) {
			broken = true;
			buffer = null;
			MikuCompanionClient.LOGGER.error("Rendering der Begleiterin ist fehlgeschlagen und wurde deaktiviert.", e);
		}
	}

	private static void draw(Minecraft client) {
		MeshData builtBuffer = buffer.buildOrThrow();
		buffer = null;
		MeshData.DrawState drawParameters = builtBuffer.drawState();
		VertexFormat format = drawParameters.format();

		GpuBuffer vertices = upload(drawParameters, format, builtBuffer);

		GpuBuffer indices;
		VertexFormat.IndexType indexType;
		if (PIPELINE.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
			builtBuffer.sortQuads(ALLOCATOR, RenderSystem.getProjectionType().vertexSorting());
			indices = PIPELINE.getVertexFormat().uploadImmediateIndexBuffer(builtBuffer.indexBuffer());
			indexType = builtBuffer.drawState().indexType();
		} else {
			RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(PIPELINE.getVertexFormatMode());
			indices = shapeIndexBuffer.getBuffer(drawParameters.indexCount());
			indexType = shapeIndexBuffer.type();
		}

		GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
				.writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
		try (RenderPass renderPass = RenderSystem.getDevice()
				.createCommandEncoder()
				.createRenderPass(() -> MikuCompanionClient.MOD_ID + " miku render pass",
						client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(),
						client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
			renderPass.setPipeline(PIPELINE);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", dynamicTransforms);
			renderPass.setVertexBuffer(0, vertices);
			renderPass.setIndexBuffer(indices, indexType);
			renderPass.drawIndexed(0, 0, drawParameters.indexCount(), 1);
		}

		builtBuffer.close();
		vertexBuffer.rotate();
	}

	private static GpuBuffer upload(MeshData.DrawState drawParameters, VertexFormat format, MeshData builtBuffer) {
		int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();

		if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
			if (vertexBuffer != null) {
				vertexBuffer.close();
			}
			vertexBuffer = new MappableRingBuffer(() -> MikuCompanionClient.MOD_ID + " miku vertex buffer",
					GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, vertexBufferSize);
		}

		CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
		try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(
				vertexBuffer.currentBuffer().slice(0, builtBuffer.vertexBuffer().remaining()), false, true)) {
			MemoryUtil.memCopy(builtBuffer.vertexBuffer(), mappedView.data());
		}
		return vertexBuffer.currentBuffer();
	}
}
