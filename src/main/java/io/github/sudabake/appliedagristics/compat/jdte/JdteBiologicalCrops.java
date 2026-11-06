package io.github.sudabake.appliedagristics.compat.jdte;

import com.mojang.logging.LogUtils;
import io.github.sudabake.appliedagristics.Agristics;
import io.github.sudabake.appliedagristics.Balance;
import io.github.sudabake.appliedagristics.biology.BioData;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Optional access to JDTE's public record/HarvestGenerator API, without a runtime dependency.
 * Reflection is resolved once per definition class; no JDTE class is linked when it is absent.
 */
public final class JdteBiologicalCrops {
    public interface Definition {
        ItemStack agristics$getSeed();
        void agristics$setSeed(ItemStack seed);
    }
    public interface BatchHost {}
    private record Api(Constructor<?> constructor,Method[] accessors,int outputs,int loot,int generator,Class<?> generatorType,boolean batch) {}
    private static final AtomicBoolean WARNED=new AtomicBoolean();
    private static final ClassValue<Optional<Api>> APIS=new ClassValue<>() {
        @Override protected Optional<Api> computeValue(Class<?> type) {
            try {
                if(!type.isRecord()) return Optional.empty();
                var fields=type.getRecordComponents();var types=new Class<?>[fields.length];var methods=new Method[fields.length];
                int outputs=-1,loot=-1,generator=-1;
                for(int i=0;i<fields.length;i++) {
                    types[i]=fields[i].getType();methods[i]=fields[i].getAccessor();
                    switch(fields[i].getName()) { case "outputs"->outputs=i;case "useLootTable"->loot=i;case "harvestGenerator"->generator=i; }
                }
                if(outputs<0 || loot<0 || generator<0 || !types[generator].isInterface()) return Optional.empty();
                var harvest=types[generator].getMethod("generate",ServerLevel.class,net.minecraft.core.BlockPos.class,ItemStack.class);
                if(!List.class.isAssignableFrom(harvest.getReturnType())) return Optional.empty();
                return Optional.of(new Api(type.getConstructor(types),methods,outputs,loot,generator,types[generator],supportsBatch(type)));
            } catch(ReflectiveOperationException | LinkageError exception) { return Optional.empty(); }
        }
    };
    private static boolean supportsBatch(Class<?> definition) {
        if (!Definition.class.isAssignableFrom(definition)) return false;
        try {
            for (var name : List.of("GreenhouseBE", "LargeGreenhouseBE")) {
                var host=Class.forName("com.jdte.common.blockentities."+name,false,definition.getClassLoader());
                if (!BatchHost.class.isAssignableFrom(host)
                    || host.getDeclaredMethod("generateSingleHarvest",ServerLevel.class,definition,ItemStack.class).getReturnType()!=List.class)
                    return false;
            }
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) { return false; }
    }
    public static List<ItemStack> harvest(ServerLevel level,ItemStack seed) {
        var id=BioData.identity(seed);
        if(id==null) return List.of();
        var result=new ArrayList<ItemStack>();result.add(BioData.withIdentity(Agristics.BIO_ESSENCE.get(),id));
        if(level.random.nextDouble()<Balance.FALLBACK_SEED_CHANCE.get()) result.add(seed.copyWithCount(1));
        return result;
    }
    public static Object adapt(ItemStack seed,Object definition) {
        if(definition==null || !BioData.seed(seed) || BioData.identity(seed)==null) return definition;
        try {
            var api=APIS.get(definition.getClass()).orElseThrow(()->new ReflectiveOperationException("Unsupported JDTE crop definition"));
            var values=new Object[api.accessors.length];
            for(int i=0;i<values.length;i++) values[i]=api.accessors[i].invoke(definition);
            // Never replace an existing specialized harvest callback or an explicit recipe.
            if(values[api.generator]!=null) return definition;
            var stored=seed.copyWithCount(1);
            values[api.outputs]=List.of(BioData.withIdentity(Agristics.BIO_ESSENCE.get(),BioData.identity(stored)));
            values[api.loot]=false;
            if (api.batch) {
                var adapted=api.constructor.newInstance(values);
                ((Definition)adapted).agristics$setSeed(stored);
                return adapted;
            }
            values[api.generator]=Proxy.newProxyInstance(api.generatorType.getClassLoader(),new Class<?>[]{api.generatorType},(proxy,method,args)-> {
                if(method.getDeclaringClass()==Object.class) return switch(method.getName()) {
                    case "toString"->"AppliedAgristicsBiologicalHarvest";case "hashCode"->System.identityHashCode(proxy);case "equals"->proxy==args[0];default->null;
                };
                if(method.getName().equals("generate") && args!=null && args.length==3 && args[0] instanceof ServerLevel level) return harvest(level,stored);
                throw new UnsupportedOperationException("Unsupported JDTE harvest method: "+method.getName());
            });
            return api.constructor.newInstance(values);
        } catch(ReflectiveOperationException | LinkageError | IllegalArgumentException exception) {
            if(WARNED.compareAndSet(false,true)) LogUtils.getLogger().warn("Applied Agristics: JDTE biological crop adapter is unavailable; preserving the original provider",exception);
            return definition;
        }
    }
    private JdteBiologicalCrops() {}
}
