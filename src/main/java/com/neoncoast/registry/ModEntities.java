package com.neoncoast.registry;

import com.neoncoast.NeonCoast;
import com.neoncoast.npc.CitizenEntity;
import com.neoncoast.npc.PoliceOfficerEntity;
import net.minecraft.world.entity.Avatar;
import com.neoncoast.vehicle.CarEntity;
import com.neoncoast.vehicle.CarType;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.Vec3;

public final class ModEntities {
    // driver sits on the left (+x), behind the steering wheel; sports cars sit lower
    public static final EntityType<CarEntity> SEDAN = car("sedan", new Vec3(0.4, 0.25, -0.1));
    public static final EntityType<CarEntity> SPORTS_CAR = car("sports_car", new Vec3(0.4, 0.05, -0.25));
    public static final EntityType<CarEntity> POLICE_CAR = car("police_car", new Vec3(0.4, 0.25, -0.1));
    public static final EntityType<PoliceOfficerEntity> POLICE_OFFICER = Registry.register(BuiltInRegistries.ENTITY_TYPE, NeonCoast.id("police_officer"),
            EntityType.Builder.of(PoliceOfficerEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f).eyeHeight(1.62f).vehicleAttachment(Avatar.DEFAULT_VEHICLE_ATTACHMENT)
                    .clientTrackingRange(10).build(key("police_officer")));
    public static final EntityType<CitizenEntity> CITIZEN = Registry.register(BuiltInRegistries.ENTITY_TYPE, NeonCoast.id("citizen"),
            EntityType.Builder.of(CitizenEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f).eyeHeight(1.62f).vehicleAttachment(Avatar.DEFAULT_VEHICLE_ATTACHMENT)
                    .clientTrackingRange(10).build(key("citizen")));

    private static net.minecraft.resources.ResourceKey<EntityType<?>> key(String name) {
        return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, NeonCoast.id(name));
    }

    private ModEntities() {
    }

    private static EntityType<CarEntity> car(String name, Vec3 seat) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, NeonCoast.id(name),
                EntityType.Builder.of(CarEntity::new, MobCategory.MISC)
                        .sized(1.9f, 1.5f).passengerAttachments(seat)
                        .clientTrackingRange(12).updateInterval(2).build(key(name)));
    }

    public static CarType carTypeOf(EntityType<?> type) {
        if (type == SPORTS_CAR) return CarType.SPORTS;
        if (type == POLICE_CAR) return CarType.POLICE;
        return CarType.SEDAN;
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(SEDAN, CarEntity.createCarAttributes());
        FabricDefaultAttributeRegistry.register(SPORTS_CAR, CarEntity.createCarAttributes());
        FabricDefaultAttributeRegistry.register(POLICE_CAR, CarEntity.createCarAttributes());
        FabricDefaultAttributeRegistry.register(POLICE_OFFICER, PoliceOfficerEntity.createPoliceAttributes());
        FabricDefaultAttributeRegistry.register(CITIZEN, CitizenEntity.createCitizenAttributes());
    }
}
