package dev.eder.wispborn.client;

import dev.eder.wispborn.WispbornMod;
import dev.eder.wispborn.form.WispAnimatable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class WispModel extends DefaultedEntityGeoModel<WispAnimatable> {
    public WispModel() {
        super(ResourceLocation.fromNamespaceAndPath(WispbornMod.MOD_ID, "wisp"));
    }

    @Override
    public RenderType getRenderType(WispAnimatable animatable, ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }
}
