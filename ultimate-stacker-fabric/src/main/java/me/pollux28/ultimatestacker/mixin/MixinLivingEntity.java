package me.pollux28.ultimatestacker.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    @Redirect(method = "onDeath",
            at = @At(value= "INVOKE", target = "Lorg/apache/logging/log4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false))
    private void injectNoMessage(Logger instance, String s, Object o1, Object o2){
        if(!((LivingEntity)o1).getCustomName().asString().contains("Stack Size")){
            instance.info(s,o1,o2);
        }

    }

    @Inject(method = "updatePostDeath", at = @At(value = "HEAD"))
    protected void stackerRemoveOW(CallbackInfo ci){}

    @Inject(method = "handleFallDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;handleFallDamage(FFLnet/minecraft/entity/damage/DamageSource;)Z",shift = At.Shift.AFTER))
    protected void stackerHandleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> ci){}
}
