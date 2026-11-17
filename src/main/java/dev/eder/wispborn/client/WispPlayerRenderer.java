package dev.eder.wispborn.client;

import dev.eder.wispborn.form.WispAnimatable;
import dev.eder.wispborn.form.WispTuning;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class WispPlayerRenderer extends GeoReplacedEntityRenderer<AbstractClientPlayer, WispAnimatable> {
    public WispPlayerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new WispModel(), WispAnimatable.INSTANCE);
        // Aumenta só o visual; a hitbox continua definida pelo atributo SCALE.
        this.withScale(WispTuning.MODEL_VISUAL_SCALE);
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
