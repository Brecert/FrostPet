package net.techcable.sonarpet.nms.versions.v1_9_R2;

import lombok.*;

import com.google.common.collect.ImmutableList;

import net.minecraft.server.v1_9_R2.DamageSource;
import net.minecraft.server.v1_9_R2.Entity;
import net.techcable.sonarpet.nms.INMS;
import net.techcable.sonarpet.nms.NMSEntity;
import net.techcable.sonarpet.nms.NMSLivingEntity;

@RequiredArgsConstructor
public class NMSEntityImpl implements NMSEntity {
    @Getter
    private final Entity handle;

    //
    // Deobfuscated methods
    //

    @Override
    public boolean damageEntity(net.techcable.sonarpet.nms.DamageSource rawSource, float amount) {
        DamageSource damageSource = ((DamageSourceImpl) rawSource).getHandle();
        return getHandle().damageEntity(damageSource, amount);
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity() {
        return handle.getBukkitEntity();
    }

    @Override
    public ImmutableList<NMSEntity> getPassengers() {
        return ImmutableList.copyOf(handle.passengers.stream()
                .map(Entity::getBukkitEntity)
                .map(INMS.getInstance()::wrapEntity)
                .toArray(NMSEntity[]::new));
    }
}
