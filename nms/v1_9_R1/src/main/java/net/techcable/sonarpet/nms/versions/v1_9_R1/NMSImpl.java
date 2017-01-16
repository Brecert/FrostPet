package net.techcable.sonarpet.nms.versions.v1_9_R1;

import net.minecraft.server.v1_9_R1.EntityHorse;
import net.minecraft.server.v1_9_R1.EntityInsentient;
import net.techcable.sonarpet.nms.DismountingBlocked;
import net.techcable.sonarpet.nms.INMS;
import com.google.common.collect.ImmutableMap;

import net.minecraft.server.v1_9_R1.Block;
import net.minecraft.server.v1_9_R1.DamageSource;
import net.minecraft.server.v1_9_R1.EntityHuman;
import net.minecraft.server.v1_9_R1.EntityLiving;
import net.minecraft.server.v1_9_R1.EntityPlayer;
import net.minecraft.server.v1_9_R1.Packet;
import net.minecraft.server.v1_9_R1.PacketPlayOutMount;
import net.minecraft.server.v1_9_R1.SoundEffect;
import net.minecraft.server.v1_9_R1.SoundEffectType;
import net.techcable.sonarpet.item.SpawnEggItemData;
import net.techcable.sonarpet.nms.BlockSoundData;
import net.techcable.sonarpet.nms.NMSEntity;
import net.techcable.sonarpet.nms.NMSInsentientEntity;
import net.techcable.sonarpet.nms.versions.v1_9_R1.data.NMSSpawnEggItemData;
import net.techcable.sonarpet.nms.versions.v1_9_R1.data.NMSSpawnEggItemData;
import net.techcable.sonarpet.utils.reflection.SonarField;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_9_R1.CraftSound;
import org.bukkit.craftbukkit.v1_9_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.SpawnEgg;

import static com.google.common.base.Preconditions.*;

public class NMSImpl implements INMS {
    @Override
    public SpawnEggItemData createSpawnEggData(byte rawData, ItemMeta meta) {
        EntityType entityType = NMSSpawnEggItemData.getSpawnEggEntityTypeIfPresent(meta).orElse(EntityType.fromId(rawData));
        if (entityType == null) entityType = SpawnEggItemData.DEFAULT_TYPE; // 'Fix' broken configs
        return new NMSSpawnEggItemData(rawData, meta, entityType);
    }

    @Override
    public SpawnEggItemData createSpawnEggData(EntityType entityType, ItemMeta meta) {
        checkNotNull(entityType, "Null entity type");
        checkNotNull(meta, "Null item meta");
        return new NMSSpawnEggItemData(new SpawnEgg(entityType).getData(), meta, entityType); // Pretend we use metadata to make the code happy
    }

    @Override
    public void mount(Entity bukkitRider, Entity bukkitVehicle) {
        net.minecraft.server.v1_9_R1.Entity rider = ((CraftEntity) bukkitRider).getHandle();
        if (bukkitVehicle == null) {
            net.minecraft.server.v1_9_R1.Entity vehicle = rider.getVehicle(); // This is how you *really* get the vehicle :/
            if (rider instanceof DismountingBlocked) {
                ((DismountingBlocked) rider).reallyStopRiding();
            } else {
                rider.stopRiding();
            }
            if (vehicle != null) {
                Packet packet = new PacketPlayOutMount(vehicle);
                for (EntityHuman human : rider.world.players) {
                    ((EntityPlayer) human).playerConnection.sendPacket(packet);
                }
            }
        } else {
            checkArgument(bukkitRider.getWorld().equals(bukkitVehicle.getWorld()), "Rider is in world %s, while vehicle is in world %s", bukkitRider.getWorld().getName(), bukkitVehicle.getWorld().getName());
            net.minecraft.server.v1_9_R1.Entity vehicle = ((CraftEntity) bukkitVehicle).getHandle();
            rider.a(vehicle, true); // !! Obfuscated !!
            Packet packet = new PacketPlayOutMount(vehicle);
            for (EntityHuman human : rider.world.players) {
                ((EntityPlayer) human).playerConnection.sendPacket(packet);
            }
        }
    }

    @Override
    public boolean spawnEntity(NMSInsentientEntity wrapper, Location l) {
        EntityLiving entity = ((NMSEntityInsentientImpl) wrapper).getHandle();
        entity.spawnIn(((CraftWorld) l.getWorld()).getHandle());
        ((LivingEntity) entity.getBukkitEntity()).setCollidable(false);
        entity.setLocation(l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
        if (!l.getChunk().isLoaded()) {
            l.getChunk().load();
        }
        return entity.world.addEntity(entity, CreatureSpawnEvent.SpawnReason.CUSTOM);
    }

    @Override
    public net.techcable.sonarpet.nms.DamageSource mobAttackDamageSource(LivingEntity entity) {
        return new DamageSourceImpl(DamageSource.mobAttack(((CraftLivingEntity) entity).getHandle()));
    }

    @Override
    public net.techcable.sonarpet.nms.DamageSource wrapDamageSource(Object handle) {
        return new DamageSourceImpl((DamageSource) handle);
    }

    @Override
    public NMSEntity wrapEntity(Entity entity) {
        net.minecraft.server.v1_9_R1.Entity handle = ((CraftEntity) entity).getHandle();
        if (handle instanceof EntityPlayer) {
            return new NMSPlayerImpl((EntityPlayer) handle);
        } else if (handle instanceof EntityHorse) {
            return new NMSEntityHorseImpl((EntityHorse) handle);
        } else if (handle instanceof EntityInsentient) {
            return new NMSEntityInsentientImpl((EntityInsentient) handle);
        } else if (handle instanceof EntityLiving) {
            return new NMSLivingEntityImpl((EntityLiving) handle);
        } else {
            return new NMSEntityImpl(handle);
        }
    }

    private SonarField<SoundEffectType> STEP_SOUND_FIELD = SonarField.getField(Block.class, "stepSound", SoundEffectType.class);
    @Override
    @SuppressWarnings("deprecation") // I know about ur stupid magic value warning mom
    public BlockSoundData getBlockSoundData(Material material) {
        return new BlockSoundDataImpl(STEP_SOUND_FIELD.getValue(Block.getById(material.getId())));
    }

    @Override
    @SuppressWarnings("deprecation") // I know about ur stupid magic value warning mom
    public boolean isLiquid(Material block) {
        return Block.getById(block.getId()).getBlockData().getMaterial().isLiquid();
    }

    //
    // Utility methods
    //
    private static final ImmutableMap<SoundEffect, Sound> BUKKIT_SOUNDS;
    static {
        ImmutableMap.Builder<SoundEffect, Sound> soundsBuilder = ImmutableMap.builder();
        for (Sound bukkitSound : Sound.values()) {
            SoundEffect mojangSound = CraftSound.getSoundEffect(CraftSound.getSound(bukkitSound));
            soundsBuilder.put(mojangSound, bukkitSound);
        }
        BUKKIT_SOUNDS = soundsBuilder.build();
    }
    public static Sound toBukkitSound(SoundEffect mojangEffect) {
        Sound bukkitEffect = BUKKIT_SOUNDS.get(mojangEffect);
        if (bukkitEffect == null) throw new AssertionError("No bukkit sound effect for mojang effect: " + mojangEffect);
        return bukkitEffect;
    }
}
