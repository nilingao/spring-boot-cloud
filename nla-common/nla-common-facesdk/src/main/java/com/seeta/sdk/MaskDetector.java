package com.seeta.sdk;


/**
 * 口罩检测器
 */
public class MaskDetector implements AutoCloseable {

    public long impl = 0;

    public MaskDetector(SeetaModelSetting setting) throws Exception {
        this.construct(setting);
    }

    private native void construct(SeetaModelSetting setting) throws Exception;

    public native void dispose();

    /** Releases the native handle once; prefer close() over direct dispose(). */
    @Override
    public synchronized void close() {
        if (impl != 0) {
            try {
                dispose();
            } finally {
                impl = 0;
            }
        }
    }

    protected void finalize() throws Throwable {
        try { close(); } finally { super.finalize(); }
    }

    /**
     * 人脸口罩检测器
     *
     * @param imageData [input]
     * @param face      [input]
     * @param score     [output]
     * @return boolean
     */
    public native boolean detect(SeetaImageData imageData, SeetaRect face, float[] score);
}
