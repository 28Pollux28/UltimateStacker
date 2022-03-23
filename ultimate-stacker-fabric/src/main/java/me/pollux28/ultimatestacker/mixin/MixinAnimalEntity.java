package me.pollux28.ultimatestacker.mixin;

import me.pollux28.ultimatestacker.MixinAnimalEntityAccess;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mixin(AnimalEntity.class)
public abstract class MixinAnimalEntity<T extends AnimalEntity> extends MixinMobEntity<T> implements MixinAnimalEntityAccess<T> {
    List<T> inLove = new ArrayList<>();

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(method = "interactMob", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/entity/player/PlayerEntity;getStackInHand(Lnet/minecraft/util/Hand;)Lnet/minecraft/item/ItemStack;",shift = At.Shift.AFTER),locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void injectBreedingStack(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir, ItemStack itemStack){
        T animalEntity = super.getMobEntity();
        if(!animalEntity.world.isClient && !super.getStack().isEmpty() && animalEntity.isBreedingItem(itemStack)){
            if(animalEntity.getBreedingAge()==0 && animalEntity.canEat()){
                this.mixinEat(player, itemStack);
                animalEntity.lovePlayer(player);
                animalEntity.emitGameEvent(GameEvent.MOB_INTERACT, animalEntity.getCameraBlockPos());
                cir.setReturnValue(ActionResult.SUCCESS);
            }else{
                for (T animalE : super.getStack()) {
                    if (animalE.getBreedingAge() == 0 && animalE.canEat()) {
                        this.mixinEat(player, itemStack);
                        animalE.lovePlayer(player);
                        animalE.emitGameEvent(GameEvent.MOB_INTERACT, animalEntity.getCameraBlockPos());
                        this.inLove.add(animalE);
                        cir.setReturnValue(ActionResult.SUCCESS);
                        break;
                    }
                }
                //noinspection UnnecessaryReturnStatement
                return;
            }
        }

    }
    @Inject(method = "tickMovement", at= @At(value = "INVOKE",target = "Lnet/minecraft/entity/passive/PassiveEntity;tickMovement()V",shift = At.Shift.AFTER))
    private void injectBreedTick(CallbackInfo ci){
        T animalEntity = super.getMobEntity();
        Iterator<T> iterator = inLove.iterator();
        while(iterator.hasNext()){
            T t = iterator.next();
            int loveTicks = t.getLoveTicks();
            if(loveTicks >0) t.setLoveTicks(loveTicks-1);
            if(t.getLoveTicks() <= 0){
                iterator.remove();
            }
        }
        World world = super.getMobEntity().world;
        if(!world.isClient && animalEntity.getLoveTicks()>0 && !inLove.isEmpty()){
            T t1 = inLove.get(0);
            animalEntity.breed((ServerWorld) world,t1);
            inLove.remove(t1);
        }
        while(!world.isClient && inLove.size()>=2){
            T t1 = inLove.get(0);
            t1.refreshPositionAndAngles(animalEntity.getBlockPos(),animalEntity.getYaw(),animalEntity.getPitch());
            T t2 = inLove.get(1);
            t1.breed((ServerWorld) world,t2);
            inLove.remove(t1);
            inLove.remove(t2);
        }

    }

    public void mixinEat(PlayerEntity player, ItemStack stack) {
        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
    }

    @Override
    public List<T> getInLove() {
        return this.inLove;
    }

    @Override
    public void setInLove(List<T> inLove) {
        this.inLove = inLove;
    }

    @Override
    public void addToInLove(T animalEntity) {
        inLove.add(animalEntity);
    }

    @Override
    public void addToInLove(List<T> animalEntityList) {
        inLove.addAll(animalEntityList);
    }
}
