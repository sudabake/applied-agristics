package io.github.sudabake.appliedagristics.compat.jade;

import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.biology.BioCropBlock;
import io.github.sudabake.appliedagristics.biology.BioCropEntity;
import io.github.sudabake.appliedagristics.biology.BioData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

/** Loaded by Jade's plugin discovery only; Jade remains compile-only and optional. */
@WailaPlugin(Agristics.ID)
public final class AgristicsJade implements IWailaPlugin {
    @Override public void register(IWailaCommonRegistration registration) { registration.registerBlockDataProvider(Crop.INSTANCE,BioCropEntity.class); }
    @Override public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Crop.INSTANCE,BioCropBlock.class);
        registration.registerBlockIcon(Crop.INSTANCE,BioCropBlock.class);
    }
    public enum Crop implements IBlockComponentProvider,IServerDataProvider<BlockAccessor> {
        INSTANCE;
        private static final String KEY="appliedagristics:species";
        @Override public ResourceLocation getUid() { return ResourceLocation.fromNamespaceAndPath(Agristics.ID,"bio_crop_identity"); }
        @Override public int getDefaultPriority() { return 1000; }
        @Override public void appendServerData(CompoundTag data,BlockAccessor accessor) {
            if(accessor.getBlockEntity() instanceof BioCropEntity crop && crop.identity()!=null) data.putString(KEY,crop.identity().toString());
        }
        private ResourceLocation species(BlockAccessor accessor) {
            var id=ResourceLocation.tryParse(accessor.getServerData().getString(KEY));
            return id!=null?id:accessor.getBlockEntity() instanceof BioCropEntity crop?crop.identity():null;
        }
        @Override public void appendTooltip(ITooltip tooltip,BlockAccessor accessor,IPluginConfig config) {
            var id=species(accessor);
            if(id==null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return;
            var name=Component.translatable("gui.appliedagristics.crop_species",BuiltInRegistries.ENTITY_TYPE.get(id).getDescription());
            if(!tooltip.replace(JadeIds.CORE_OBJECT_NAME,name)) tooltip.add(0,name,JadeIds.CORE_OBJECT_NAME);
            // Jade's native CropBlock provider already displays the real AGE_7 progress.
        }
        @Override public IElement getIcon(BlockAccessor accessor,IPluginConfig config,IElement original) {
            var id=species(accessor);
            return id==null?original:IElementHelper.get().item(BioData.withIdentity(Agristics.BIO_SEED.get(),id));
        }
    }
}
