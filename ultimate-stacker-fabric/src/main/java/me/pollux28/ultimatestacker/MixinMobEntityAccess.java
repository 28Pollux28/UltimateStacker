package me.pollux28.ultimatestacker;

import net.minecraft.entity.mob.MobEntity;

import java.util.LinkedList;

public interface MixinMobEntityAccess<T extends MobEntity> {
    LinkedList<T> getStack();
    void setStack(LinkedList<T> list);
    void addToStack(LinkedList<T> list);
    void addToStack(T entity);
}
