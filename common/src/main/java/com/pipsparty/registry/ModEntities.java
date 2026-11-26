package com.pipsparty.registry;

import com.pipsparty.PipsParty;
import com.pipsparty.entity.AnimatronicEntity;
import com.pipsparty.entity.Character;
import com.pipsparty.entity.SecurityCameraEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.Vec3;

public final class ModEntities {
    public static final Map<Character, Reg<EntityType<AnimatronicEntity>>> ANIMATRONICS = new LinkedHashMap<>();

    static {
        for (Character c : Character.values()) {
            String name = c.name().toLowerCase();
            ANIMATRONICS.put(c, Reg.<EntityType<AnimatronicEntity>>of(Registries.ENTITY_TYPE, PipsParty.id(name), key ->
                    EntityType.Builder.<AnimatronicEntity>of((type, level) -> new AnimatronicEntity(type, level, c), MobCategory.MONSTER)
                            .sized(0.8f, 2.35f).eyeHeight(2.05f)
                            .passengerAttachments(new Vec3(0.0, 1.35, 0.0))
                            .clientTrackingRange(12).build(key)));
        }
    }

    public static final Reg<EntityType<SecurityCameraEntity>> SECURITY_CAMERA = Reg.<EntityType<SecurityCameraEntity>>of(Registries.ENTITY_TYPE,
            PipsParty.id("security_camera"), key -> EntityType.Builder.<SecurityCameraEntity>of(SecurityCameraEntity::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).eyeHeight(0.2f).clientTrackingRange(16).updateInterval(20).build(key));

    private ModEntities() {
    }

    public static Reg<EntityType<AnimatronicEntity>> of(Character c) {
        return ANIMATRONICS.get(c);
    }

    public static void init() {
    }
}
