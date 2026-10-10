package com.seeta.sdk;


/**
 * 人脸检测器 检测到的每个人脸位置，用矩形表示。
 *
 * @author YaoCai Lin
 */
public class FaceDetector implements AutoCloseable {

    public long impl = 0;

    public FaceDetector(SeetaModelSetting setting) throws Exception {
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

    public native SeetaRect[] Detect(SeetaImageData image);

    public native void set(Property property, double value);

    public native double get(Property property);

    public enum Property {
        PROPERTY_MIN_FACE_SIZE(0),
        PROPERTY_THRESHOLD(1),
        PROPERTY_MAX_IMAGE_WIDTH(2),
        PROPERTY_MAX_IMAGE_HEIGHT(3),
        PROPERTY_NUMBER_THREADS(4),
        PROPERTY_ARM_CPU_MODE(0x101);

        private int value;

        private Property(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
