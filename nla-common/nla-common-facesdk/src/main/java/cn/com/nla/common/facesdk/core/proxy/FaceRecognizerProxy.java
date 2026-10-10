package cn.com.nla.common.facesdk.core.proxy;

import cn.com.nla.common.facesdk.core.pool.FaceRecognizerPool;
import cn.com.nla.common.facesdk.core.pool.SeetaConfSetting;
import com.seeta.sdk.FaceRecognizer;
import com.seeta.sdk.SeetaImageData;
import com.seeta.sdk.SeetaPointF;


/**
 * 人脸特征提取评估器
 */
public class FaceRecognizerProxy implements AutoCloseable {

    private FaceRecognizerPool pool;


    private FaceRecognizerProxy() {
    }


    public FaceRecognizerProxy(SeetaConfSetting<FaceRecognizer> config) {
        this.pool = new FaceRecognizerPool(config);
    }


    public float[] extract(SeetaImageData image, SeetaPointF[] points) {

        float[] features = null;
        FaceRecognizer faceRecognizer = null;

        try {
            faceRecognizer = pool.borrowObject();
            features = new float[faceRecognizer.GetExtractFeatureSize()];
            if (!faceRecognizer.Extract(image, points, features))
                throw new IllegalStateException("Face feature extraction failed");

        } catch (Exception e) {
            throw new IllegalStateException("Face SDK operation failed", e);
        } finally {

            if (faceRecognizer != null) {
                pool.returnObject(faceRecognizer);
            }
        }

        return features;
    }

    public int getExtractFeatureSize() {

        FaceRecognizer faceRecognizer = null;

        try {
            faceRecognizer = pool.borrowObject();
            if (faceRecognizer != null) {
                return faceRecognizer.GetExtractFeatureSize();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Face SDK operation failed", e);
        } finally {
            if (faceRecognizer != null) {
                pool.returnObject(faceRecognizer);
            }
        }

        throw new IllegalStateException("Face recognizer is unavailable");
    }

    public float calculateSimilarity(float[] features1, float[] features2) {

        float score = -1f;
        FaceRecognizer faceRecognizer = null;

        try {
            faceRecognizer = pool.borrowObject();
            score = faceRecognizer.CalculateSimilarity(features1, features2);

        } catch (Exception e) {
            throw new IllegalStateException("Face SDK operation failed", e);
        } finally {
            if (faceRecognizer != null) {
                pool.returnObject(faceRecognizer);
            }
        }

        return score;
    }

    public float cosineSimilarity(float[] leftVector, float[] rightVector) {
        double dotProduct = 0;
        for (int i = 0; i < leftVector.length; i++) {
            dotProduct += leftVector[i] * rightVector[i];
        }
        double d1 = 0.0d;
        for (float value : leftVector) {
            d1 += Math.pow(value, 2);
        }
        double d2 = 0.0d;
        for (float value : rightVector) {
            d2 += Math.pow(value, 2);
        }
        double cosineSimilarity;
        if (d1 <= 0.0 || d2 <= 0.0) {
            cosineSimilarity = 0.0;
        } else {
            cosineSimilarity = (dotProduct / (Math.sqrt(d1) * Math.sqrt(d2)));
        }
        return (float) cosineSimilarity;
    }
    @Override
    public void close() { pool.close(); }
}
