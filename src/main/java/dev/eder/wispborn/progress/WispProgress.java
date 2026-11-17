package dev.eder.wispborn.progress;

import com.mojang.serialization.Codec;

/**
 * Progresso persistido do wisp. Só o EP total é guardado: o nível é sempre derivado da curva do config
 * ({@link LevelCurve#levelFor}), para que editar {@code levels.thresholds} mude o nível na hora.
 * Saves antigos com o campo {@code level} continuam carregando (o campo extra é ignorado).
 */
public record WispProgress(int totalEp) {
    public static final WispProgress START = new WispProgress(0);

    public static final Codec<WispProgress> CODEC = Codec.INT.fieldOf("total_ep")
            .xmap(WispProgress::new, WispProgress::totalEp)
            .codec();
}
