package com.seeta.sdk;


/**
 * 姿态估计
 */
public class PoseEstimator implements AutoCloseable {

    public long impl = 0;

    /**
     * 后面自己添加的
     *
     * @param seting
     */
    public PoseEstimator(SeetaModelSetting seting) {
        this.construct(seting);
    }

    public PoseEstimator(String seetaModel) {
        this.construct(seetaModel);
    }

    public PoseEstimator(String model, String device, int id) {
        this.construct(model, device, id);
    }

    /**
     * 后面自己添加的
     *
     * @param seting
     */
    private native void construct(SeetaModelSetting seting);

    private native void construct(String seetaModel);

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

    public native void Estimate(SeetaImageData image, SeetaRect face, float[] yaw, float[] pitch, float[] roll);
}
