package io.github.sudabake.appliedagristics;
import io.github.sudabake.appliedagristics.logic.Harvests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
/** Common crop identity used by seed storage and stock targets. */
public final class Cultivations {
    public record Entry(ResourceLocation getId, Item getSeedsItem, Item getEssenceItem, Component getDisplayName, ResourceLocation biology,ItemStack seedTemplate,ItemStack outputTemplate) {
        public Entry(ResourceLocation id, Item seed, Item essence, Component name) { this(id, seed, essence, name,null,null,null); }
        public Entry(ResourceLocation id, Item seed, Item essence, Component name,ResourceLocation biology) { this(id, seed, essence, name,biology,null,null); }
        public ItemStack seedStack() { return seedTemplate!=null ? seedTemplate.copyWithCount(1) : biology == null ? new ItemStack(getSeedsItem) : io.github.sudabake.appliedagristics.biology.BioData.withIdentity(getSeedsItem, biology); }
        public ItemStack essenceStack() { return outputTemplate!=null ? outputTemplate.copyWithCount(1) : biology == null ? new ItemStack(getEssenceItem) : io.github.sudabake.appliedagristics.biology.BioData.withIdentity(getEssenceItem, biology); }
    }
    private static final Map<appeng.api.stacks.AEItemKey, Entry> ORDINARY = new HashMap<>();
    private static final Map<appeng.api.stacks.AEItemKey,ResourceLocation> VARIANTS=new HashMap<>();
    public static void invalidate() { synchronized(ORDINARY) { ORDINARY.clear(); } synchronized(VARIANTS) { VARIANTS.clear(); } }
    public static Entry seed(ItemStack stack, ServerLevel level, BlockPos pos) {
        var source=io.github.sudabake.appliedagristics.logic.CropSources.find(level,stack);
        if (source!=null) { var product=source.product(); return new Entry(id(stack,level),stack.getItem(),product.getItem(),stack.getHoverName(),null,stack.copyWithCount(1),product.copyWithCount(1)); }
        if (io.github.sudabake.appliedagristics.biology.BioData.seed(stack)) {
            var id = io.github.sudabake.appliedagristics.biology.BioData.identity(stack);
            return new Entry(io.github.sudabake.appliedagristics.biology.BioData.cropId(id), Agristics.BIO_SEED.get(), Agristics.BIO_ESSENCE.get(), BuiltInRegistries.ENTITY_TYPE.get(id).getDescription(), id);
        }
        var mystical = Crops.seed(stack);
        if (mystical != null) return new Entry(id(stack,level), mystical.getSeedsItem(), mystical.getEssenceItem(), mystical.getDisplayName(),null,stack.copyWithCount(1),new ItemStack(mystical.getEssenceItem()));
        if (!Harvests.seed(stack,level)) return null;
        synchronized (ORDINARY) {
            var key=appeng.api.stacks.AEItemKey.of(stack.copyWithCount(1)); var cached = ORDINARY.get(key); if (cached != null) return cached;
            var drops = Harvests.harvest(level, pos, stack.copyWithCount(1), new ItemStack(Blocks.FARMLAND));
            var edibleSeed = stack.get(net.minecraft.core.component.DataComponents.FOOD) != null
                ? drops.stream().filter(d -> d.is(stack.getItem())).findFirst().orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
            var product = !edibleSeed.isEmpty() ? edibleSeed : drops.stream().filter(d -> !d.is(stack.getItem())).findFirst().orElse(drops.isEmpty() ? ItemStack.EMPTY : drops.getFirst());
            if (product.isEmpty()) return null;
            var entry = new Entry(id(stack,level), stack.getItem(), product.getItem(), stack.getHoverName(),null,stack.copyWithCount(1),product.copyWithCount(1));
            ORDINARY.put(key, entry); return entry;
        }
    }
    public static Entry byId(ResourceLocation id, ServerLevel level, BlockPos pos) {
        var entity = io.github.sudabake.appliedagristics.biology.BioData.entityId(id);
        if (entity != null) return seed(io.github.sudabake.appliedagristics.biology.BioData.withIdentity(Agristics.BIO_SEED.get(), entity), level, pos);
        var mystical = com.blakebr0.mysticalagriculture.registry.CropRegistry.getInstance().getCropById(id);
        return seed(new ItemStack(mystical != null ? mystical.getSeedsItem() : BuiltInRegistries.ITEM.get(id)), level, pos);
    }
    public static ResourceLocation id(ItemStack stack) {
        if (io.github.sudabake.appliedagristics.biology.BioData.seed(stack)) return io.github.sudabake.appliedagristics.biology.BioData.cropId(io.github.sudabake.appliedagristics.biology.BioData.identity(stack));
        var mystical = Crops.seed(stack); return mystical != null ? mystical.getId() : BuiltInRegistries.ITEM.getKey(stack.getItem());
    }
    public static ResourceLocation id(ItemStack stack,net.minecraft.world.level.Level level) {
        var base=id(stack);
        if (stack.getComponentsPatch().isEmpty() || io.github.sudabake.appliedagristics.biology.BioData.seed(stack) || level==null) return base;
        synchronized(VARIANTS) {
            return VARIANTS.computeIfAbsent(appeng.api.stacks.AEItemKey.of(stack.copyWithCount(1)),key->{
                var json=ItemStack.CODEC.encodeStart(level.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),stack.copyWithCount(1)).getOrThrow();
                try {
                    var digest=java.security.MessageDigest.getInstance("SHA-256").digest(canonical(json).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    return ResourceLocation.fromNamespaceAndPath(Agristics.ID,"variant/"+base.getNamespace()+"/"+base.getPath()+"/"+java.util.HexFormat.of().formatHex(digest));
                } catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
            });
        }
    }
    private static com.google.gson.JsonElement canonical(com.google.gson.JsonElement value) {
        if (value.isJsonObject()) { var result=new com.google.gson.JsonObject(); new java.util.TreeMap<>(value.getAsJsonObject().asMap()).forEach((k,v)->result.add(k,canonical(v))); return result; }
        if (value.isJsonArray()) { var result=new com.google.gson.JsonArray(); value.getAsJsonArray().forEach(v->result.add(canonical(v))); return result; }
        return value;
    }
    private Cultivations() {}
}
