package cn.com.nla.common.facesdk.core.proxy;

import cn.com.nla.common.facesdk.core.pool.QualityOfLBNPool;
import cn.com.nla.common.facesdk.core.pool.SeetaConfSetting;
import com.seeta.sdk.QualityOfLBN;
import com.seeta.sdk.SeetaImageData;
import com.seeta.sdk.SeetaPointF;

public class QualityOfLBNProxy implements AutoCloseable {

    private QualityOfLBNPool pool;

    private QualityOfLBNProxy() {
    }

    public QualityOfLBNProxy(SeetaConfSetting<QualityOfLBN> setting) {
        pool = new QualityOfLBNPool(setting);
    }


    public LBNClass detect(SeetaImageData imageData, SeetaPointF[] points) {
        int[] light = new int[1];
        int[] blur = new int[1];
        int[] noise = new int[1];

        QualityOfLBN qualityOfLBN = null;
        try {
            qualityOfLBN = pool.borrowObject();
            qualityOfLBN.Detect(imageData, points, light, blur, noise);
        } catch (Exception e) {
            throw new IllegalStateException("Face SDK operation failed", e);
        } finally {
            if (qualityOfLBN != null) {
                pool.returnObject(qualityOfLBN);
            }
        }

        return new LBNClass(light, blur, noise);
    }

    public class LBNClass {

        private QualityOfLBN.LIGHTSTATE lightstate;
        private QualityOfLBN.BLURSTATE blurstate;
        private QualityOfLBN.NOISESTATE noisestate;

        public LBNClass(int[] light, int[] blur, int[] noise) {
            this.lightstate = QualityOfLBN.LIGHTSTATE.values()[light[0]];
            this.blurstate = QualityOfLBN.BLURSTATE.values()[blur[0]];
            this.noisestate = QualityOfLBN.NOISESTATE.values()[noise[0]];
        }

        public QualityOfLBN.LIGHTSTATE getLightstate() {
            return lightstate;
        }

        public void setLightstate(QualityOfLBN.LIGHTSTATE lightstate) {
            this.lightstate = lightstate;
        }

        public QualityOfLBN.BLURSTATE getBlurstate() {
            return blurstate;
        }

        public void setBlurstate(QualityOfLBN.BLURSTATE blurstate) {
            this.blurstate = blurstate;
        }

        public QualityOfLBN.NOISESTATE getNoisestate() {
            return noisestate;
        }

        public void setNoisestate(QualityOfLBN.NOISESTATE noisestate) {
            this.noisestate = noisestate;
        }
    }

    @Override
    public void close() { pool.close(); }
}
