package io.github.zekerzhayard.jm_csl_bridge.textures;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface IThreadDownloadImageData {
    Map<IThreadDownloadImageData, ISkinImageCallback> callbacks = new ConcurrentHashMap<>();

    default void setCallback(ISkinImageCallback callback) {
        callbacks.put(this, callback);
    }

    default void removeCallback() {
        callbacks.remove(this);
    }

    default void onSkinChanging(BufferedImage bufferedImage) {
        ISkinImageCallback callback = callbacks.get(this);
        if (callback != null) {
            callback.onSkinChanging(bufferedImage);
            this.removeCallback();
        }
    }

    interface ISkinImageCallback {
        void onSkinChanging(BufferedImage bufferedImage);
    }
}
