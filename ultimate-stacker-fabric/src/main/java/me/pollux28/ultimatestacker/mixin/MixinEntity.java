package me.pollux28.ultimatestacker.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Shadow public abstract World getWorld();

    @Inject(method = "remove", at = @At("HEAD"))
    protected void stackerRemoveOW(Entity.RemovalReason reason, CallbackInfo ci){} //todo remove if working

}
