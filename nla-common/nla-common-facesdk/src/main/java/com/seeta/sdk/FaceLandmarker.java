package com.seeta.sdk;


/**
 * 人脸特征点检测器
 */
public class FaceLandmarker implements AutoCloseable {

    public long impl = 0;

    public FaceLandmarker(SeetaModelSetting setting) {
        this.construct(setting);
    }

    public FaceLandmarker(String model, String device, int id) {
        this.construct(model, device, id);
    }

    private native void construct(SeetaModelSetting seeting);

    private native void construct(String model, String device, int id);

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

    public native int number();

    public native void mark(SeetaImageData imageData, SeetaRect seetaRect, SeetaPointF[] pointFS);

    public native void mark(SeetaImageData imageData, SeetaRect seetaRect, SeetaPointF[] pointFS, int[] masks);
}
