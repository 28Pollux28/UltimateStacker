package me.pollux28.ultimatestacker.mixin;

import com.google.common.collect.ImmutableList;
import me.pollux28.ultimatestacker.MixinAnimalEntityAccess;
import me.pollux28.ultimatestacker.MixinMobEntityAccess;
import me.pollux28.ultimatestacker.UltimateStackerFabric;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(MobEntity.class)
public abstract class MixinMobEntity<T extends MobEntity> extends MixinLivingEntity implements MixinMobEntityAccess<T> {

    private static final String STACK_KEY = "stack";
    private final T mobEntity = ((T) (Object) this);
    private LinkedList<T> stack = new LinkedList<>();
    Class<T> type = (Class<T>) mobEntity.getClass();

    //todo: prevent merge if has passengers or is passenger

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;tick()V", shift = At.Shift.AFTER))
    private void injectUltimateStackerMergingEntity(CallbackInfo ci) {
        if(!this.mobEntity.world.isClient && !this.mobEntity.isPersistent()) tryMerge();
    }

    @Inject(method = "writeCustomDataToNbt",at = @At("TAIL"))
    private void injectUltimateStackerSaveData(NbtCompound nbt, CallbackInfo ci){
        if (!stack.isEmpty()) {
            NbtList nbtList = new NbtList();
            for (T entity : this.stack) {
                NbtCompound nbtCompound = new NbtCompound();
                EntityType<?> entityType = entity.getType();
                Identifier identifier = EntityType.getId(entityType);
                String string = !entityType.isSaveable() || identifier == null ? null : identifier.toString();
                if (string == null) {
                    continue;
                }
                nbtCompound.putString(Entity.ID_KEY, string);
                entity.writeNbt(nbtCompound);
                nbtList.add(nbtCompound);
            }
            if (!nbtList.isEmpty()) {
                nbt.put(STACK_KEY, nbtList);
            }
        }
    }
    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private <S extends AnimalEntity> void injectUltimateStackerReadData(NbtCompound nbt, CallbackInfo ci){
        NbtList list = nbt.getList(STACK_KEY,10);
        this.stack.addAll((EntityType.streamFromNbt(list, this.mobEntity.world)).filter(entity -> type.isInstance(entity)).map(entity-> type.cast(entity)).collect(ImmutableList.toImmutableList()));
        if(mobEntity instanceof AnimalEntity){
            S animalMobEntity = (S) mobEntity;
            Class<S> animalType = (Class<S>) animalMobEntity.getClass();
            ((MixinAnimalEntityAccess<S>)animalMobEntity).addToInLove( this.stack.stream().filter(entity -> ((S) entity).getLoveTicks()>0).map(animalType::cast).collect(Collectors.toList()));
        }
    }

    @Inject(method = "interactWithItem", at = @At("HEAD"), cancellable = true)
    private void injectKillStackWithDebugStick(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir){
        if(((MobEntity)(Object)this).world.isClient){cir.setReturnValue(ActionResult.CONSUME);}
        ItemStack itemStack = player.getStackInHand(hand);
        if(itemStack.isOf(Items.DEBUG_STICK)){
            LinkedList<T> stack = ((MixinMobEntityAccess<T>) this.mobEntity).getStack();
            if(!stack.isEmpty()){
                stack.clear();
                this.mobEntity.kill();
                cir.setReturnValue(ActionResult.success(false));
            }
        }
    }

    private void tryMerge() {
        if (stack.size()+1 < UltimateStackerFabric.CONFIG.MAX_STACK_NUMBER) {
            List<T> list = this.mobEntity.world.getEntitiesByClass(type, mobEntity.getBoundingBox().expand(0.5, 0.0, 0.5), (entity) -> entity != mobEntity && canMerge(entity));
            for(T entity : list){
                if(!canMerge(this.mobEntity)) continue;
                this.tryMerge(entity);
                if(!this.mobEntity.isRemoved()) continue;
                break;
            }
        }
    }

    private boolean canMerge(T entity) {
        boolean flag = true;
        if(entity instanceof SlimeEntity slimeEntity){
            flag &= slimeEntity.getSize() == ((SlimeEntity) this.mobEntity).getSize();
        }
        flag &= (entity.getType() == this.mobEntity.getType());
        flag &= !entity.isBaby();
        flag &= !this.mobEntity.isBaby();
        flag &= !(entity.fallDistance>0.0f);
        flag &= !(entity.hasPassengers());
        flag &= !(entity.hasVehicle());
        flag &= !(this.mobEntity.hasPassengers());
        flag &= !(this.mobEntity.hasVehicle());


        return flag && entity.isAlive() && ((MixinMobEntityAccess<T>)entity).getStack().size()+1<UltimateStackerFabric.CONFIG.MAX_STACK_NUMBER && !entity.isPersistent();
    }

    private void tryMerge(T entity){
        int stackSize1 = this.stack.size()+1;
        if(entity.fallDistance>0.0f) return;
        int stackSize2 = ((MixinMobEntityAccess<T>)entity).getStack().size()+1;
        if(!canMerge(stackSize1,stackSize2)) return;
        if(stackSize1<stackSize2){
            this.merge(entity, this.mobEntity);
        }else{
            this.merge(this.mobEntity, entity);
        }
    }

    private boolean canMerge(int stackSize1, int stackSize2){
        return !(stackSize1+stackSize2 > UltimateStackerFabric.CONFIG.MAX_STACK_NUMBER);
    }

    private <S extends AnimalEntity> void merge(T stackedEntity, T toMerge){
        ((MixinMobEntityAccess<T>)stackedEntity).addToStack(toMerge);
        ((MixinMobEntityAccess<T>)stackedEntity).addToStack(((MixinMobEntityAccess<T>)toMerge).getStack());
        if(stackedEntity instanceof AnimalEntity){
            S animalStackedEntity = (S) stackedEntity;
            S animalToMerge = (S) toMerge;
            if(animalToMerge.getLoveTicks()>0){
                ((MixinAnimalEntityAccess<S>) animalStackedEntity).addToInLove(animalToMerge);
            }
            ((MixinAnimalEntityAccess<S>) animalStackedEntity).addToInLove(((MixinAnimalEntityAccess<S>) animalToMerge).getInLove());
        }
        toMerge.remove(Entity.RemovalReason.DISCARDED);
        stackedEntity.setCustomName(new LiteralText("Stack Size : "+(((MixinMobEntityAccess)stackedEntity).getStack().size()+1)));
    }

    @Override()
    protected void stackerRemoveOW(CallbackInfo ci){
        if(this.mobEntity.deathTime==0){
//            Entity.RemovalReason reason = this.mobEntity.getRemovalReason();
//            if(reason== Entity.RemovalReason.KILLED){
                T entity = stack.poll();
                if(entity == null) return;
                T entity2 = (T) entity.getType().create(entity.getWorld());
                entity2.copyFrom(entity);
                entity2.setPos(mobEntity.getX(),mobEntity.getY()+0.01d,mobEntity.getZ());
                ((MixinMobEntityAccess<T>)entity2).setStack(this.stack);
                if(!this.stack.isEmpty()){
                    entity2.setCustomName(new LiteralText("Stack Size : "+(((MixinMobEntityAccess)entity2).getStack().size()+1)));
                }else{
                    entity2.setCustomName(null);
                }
                entity2.updatePosition(mobEntity.getX(),mobEntity.getY()+0.01d,mobEntity.getZ());
                mobEntity.world.spawnEntity(entity2);
//            }
        }
    }

//    @Override
//    protected void stackerHandleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> ci) {
//        for(T entity: stack){
//            int i = this.mixinComputeFallDamage(entity,fallDistance, damageMultiplier);
//            if (i > 0) {
////                entity.playSound(entity.getFallSound(i), 1.0f, 1.0f);
////                entity.playBlockFallSound();
//                entity.damage(damageSource, i);
//            }
//        }
//    }

    protected int mixinComputeFallDamage(T entity, float fallDistance, float damageMultiplier) {
        StatusEffectInstance statusEffectInstance = entity.getStatusEffect(StatusEffects.JUMP_BOOST);
        float f = statusEffectInstance == null ? 0.0f : (float)(statusEffectInstance.getAmplifier() + 1);
        return MathHelper.ceil((fallDistance - 3.0f - f) * damageMultiplier);
    }

    public T getMobEntity() {
        return mobEntity;
    }

    public Class<T> getType() {
        return type;
    }

    @Override
    public LinkedList<T> getStack() {
        return this.stack;
    }

    @Override
    public void setStack(LinkedList<T> list) {
        this.stack = list;
    }

    @Override
    public void addToStack(LinkedList<T> list) {
        this.stack.addAll(list);
    }

    @Override
    public void addToStack(T entity) {
        this.stack.add(entity);
    }
}
