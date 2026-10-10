package com.seeta.sdk;


/**
 * 性别估计器
 */
public class GenderPredictor implements AutoCloseable {

    public long impl = 0;

    public GenderPredictor(SeetaModelSetting setting) {
        this.construct(setting);
    }

    private native void construct(SeetaModelSetting setting);

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

    public native int GetCropFaceWidth();

    public native int GetCropFaceHeight();

    public native int GetCropFaceChannels();

    public native boolean CropFace(SeetaImageData image, SeetaPointF[] points, SeetaImageData face);

    private native boolean PredictGenderCore(SeetaImageData face, int[] gender_index);

    private native boolean PredictGenderWithCropCore(SeetaImageData image, SeetaPointF[] points, int[] gender_index);

    public boolean PredictGender(SeetaImageData face, GENDER[] gender) {
        int[] gender_index = new int[1];
        boolean result = PredictGenderCore(face, gender_index);
        if (gender.length > 0) {
            gender[0] = GENDER.values()[gender_index[0]];
        }

        return result;
    }

    public boolean PredictGenderWithCrop(SeetaImageData image, SeetaPointF[] points, GENDER[] gender) {
        int[] gender_index = new int[1];
        boolean result = PredictGenderWithCropCore(image, points, gender_index);
        if (gender.length > 0) {
            gender[0] = GENDER.values()[gender_index[0]];
        }

        return result;
    }

    public native void set(Property property, double value);

    public native double get(Property property);

    public enum Property {
        PROPERTY_NUMBER_THREADS(4),
        PROPERTY_ARM_CPU_MODE(5);

        private int value;

        private Property(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum GENDER {
        MALE,
        FEMALE
    }
}
