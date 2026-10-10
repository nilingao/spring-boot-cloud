package com.seeta.sdk;


/**
 * QualityOfBrightness
 * 人脸亮度评估器构造函数。
 */
public class QualityOfBrightness implements AutoCloseable {


    public long impl = 0;

    public QualityOfBrightness() {
        this.construct();
    }

    public QualityOfBrightness(float v0, float v1, float v2, float v3) {
        this.construct(v0, v1, v2, v3);
    }

    private native void construct();

    /**
     * @param v0 [input] param 0
     * @param v1 [input] param 1
     * @param v2 [input] param2
     * @param v3 [input] param 3
     *           [0, v0) and [v3, ~) => LOW
     *           [v0, v1) and [v2, v3) => MEDIUM
     *           [v1, v2) => HIGH
     */
    private native void construct(float v0, float v1, float v2, float v3);

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
     * @param imageData [input]image data
     * @param face      [input] face location
     * @param landmarks [input] face landmarks
     * @param score     [output] quality score
     * @return QualityLevel quality level sorted into "LOW" , "Medium" , "HIGH"
     */
    private native int checkCore(SeetaImageData imageData, SeetaRect face, SeetaPointF[] landmarks, float[] score);

    public QualityLevel check(SeetaImageData imageData, SeetaRect face, SeetaPointF[] landmarks, float[] score) {
        int index = this.checkCore(imageData, face, landmarks, score);

        QualityLevel level = QualityLevel.values()[index];
        return level;
    }

    public enum QualityLevel {
        LOW,//Quality level is low
        MEDIUM,//Quality level is medium
        HIGH,//Quality level is high
    }
}





