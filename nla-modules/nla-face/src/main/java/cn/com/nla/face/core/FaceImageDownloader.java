package cn.com.nla.face.core;

import cn.com.nla.common.core.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 人员图片下载器：按 imgUrl 拉取图片字节供特征提取。
 * 仅允许 http/https，响应限 10 MB、超时 10 秒，防止大文件与慢源拖垮写入链路。
 *
 * @author TZY
 */
@Slf4j
@Component
public class FaceImageDownloader {

    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    /**
     * 下载图片内容。
     *
     * @param imgUrl 图片地址，必须是 http/https
     * @return 图片字节
     */
    public byte[] download(String imgUrl) {
        URI uri;
        try {
            uri = URI.create(imgUrl);
        } catch (IllegalArgumentException e) {
            throw new ServiceException("图片地址不合法");
        }
        String scheme = uri.getScheme();
        if (uri.getHost() == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw new ServiceException("图片地址仅支持 http/https");
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2) {
                throw new ServiceException("图片下载失败，HTTP 状态码: " + response.statusCode());
            }
            return readLimited(response.body());
        } catch (IOException e) {
            log.warn("图片下载失败: {}", imgUrl, e);
            throw new ServiceException("图片下载失败");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("图片下载被中断");
        }
    }

    /**
     * 读取响应体并限制总大小，超限即中断连接。
     *
     * @param body 响应输入流
     * @return 图片字节
     * @throws IOException 读取异常
     */
    private byte[] readLimited(InputStream body) throws IOException {
        try (body; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int len;
            while ((len = body.read(buffer)) != -1) {
                total += len;
                if (total > MAX_BYTES) {
                    throw new ServiceException("图片超过 10MB 限制");
                }
                out.write(buffer, 0, len);
            }
            if (total == 0) {
                throw new ServiceException("图片内容为空");
            }
            return out.toByteArray();
        }
    }
}
