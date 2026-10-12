package cn.com.nla.face;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.face.core.FaceImageDownloader;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 验证下载器的地址白名单与失败兜底，仅访问本机回环端口，不出外网。 */
class FaceImageDownloaderTest {

    private final FaceImageDownloader downloader = new FaceImageDownloader();

    @Test
    void malformedUrlIsRejected() {
        ServiceException e = assertThrows(ServiceException.class, () -> downloader.download("http://exa mple.com/a.png"));
        assertThat(e.getMessage()).contains("图片地址不合法");
    }

    @Test
    void nonHttpSchemeIsRejected() {
        assertThat(assertThrows(ServiceException.class, () -> downloader.download("ftp://host.com/a.png")).getMessage())
            .contains("仅支持 http/https");
        assertThat(assertThrows(ServiceException.class, () -> downloader.download("/images/local.png")).getMessage())
            .contains("仅支持 http/https");
    }

    @Test
    void connectionFailureIsReportedAsDownloadError() {
        // 127.0.0.1:1 必然拒绝连接，走 IOException 兜底分支
        ServiceException e = assertThrows(ServiceException.class, () -> downloader.download("http://127.0.0.1:1/a.png"));
        assertThat(e.getMessage()).contains("图片下载失败");
    }
}
