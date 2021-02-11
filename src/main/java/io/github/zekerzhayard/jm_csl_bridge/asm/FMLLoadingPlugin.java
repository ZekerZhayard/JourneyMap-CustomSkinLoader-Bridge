package io.github.zekerzhayard.jm_csl_bridge.asm;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Map;
import javax.annotation.Nullable;

import customskinloader.forge.TransformerManager;
import customskinloader.forge.loader.LaunchWrapper;
import io.github.zekerzhayard.jm_csl_bridge.asm.transformers.EntityDisplayTrransformer;
import io.github.zekerzhayard.jm_csl_bridge.asm.transformers.FakeSkinManagerTransformer;
import io.github.zekerzhayard.jm_csl_bridge.asm.transformers.IgnSkinTransformer;
import io.github.zekerzhayard.jm_csl_bridge.asm.transformers.RadarDrawStepFactoryTransformer;
import io.github.zekerzhayard.jm_csl_bridge.asm.transformers.TextureCacheTransformer;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.commons.lang3.ArrayUtils;

@IFMLLoadingPlugin.Name("JourneyMap_CustomSkinLoader_Bridge")
@IFMLLoadingPlugin.SortingIndex(-20)
public class FMLLoadingPlugin implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        try {
            Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            field.setAccessible(true);
            MethodHandles.Lookup implLookup = (MethodHandles.Lookup) field.get(null);

            Field transformers = LaunchWrapper.class.getDeclaredField("TRANFORMERS");
            implLookup
                .findStaticSetter(transformers.getDeclaringClass(), transformers.getName(), transformers.getType())
                .invokeWithArguments(
                    (Object) ArrayUtils.addAll(
                        (TransformerManager.IMethodTransformer[]) implLookup.findStaticGetter(transformers.getDeclaringClass(), transformers.getName(), transformers.getType()).invokeWithArguments(),
                        new FakeSkinManagerTransformer.Lambda$loadProfileTextures$1Transformer(),
                        new FakeSkinManagerTransformer.LoadSkinTransformer(),
                        new RadarDrawStepFactoryTransformer.PrepareStepsTransformer()
                    )
                );

            Field class_transformers = LaunchWrapper.class.getDeclaredField("CLASS_TRANSFORMERS");
            implLookup
                .findStaticSetter(class_transformers.getDeclaringClass(), class_transformers.getName(), class_transformers.getType())
                .invokeWithArguments(
                    (Object) ArrayUtils.addAll(
                        (TransformerManager.IClassTransformer[]) implLookup.findStaticGetter(class_transformers.getDeclaringClass(), class_transformers.getName(), class_transformers.getType()).invokeWithArguments(),
                        new EntityDisplayTrransformer(),
                        new IgnSkinTransformer(),
                        new TextureCacheTransformer()
                    )
                );
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
