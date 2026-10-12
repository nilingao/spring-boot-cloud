package cn.com.nla.video;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.model.ProtocolResult;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.core.demo.VideoRestResult;
import cn.com.nla.common.video.core.redis.subscribe.result.DeferredResultHolder;
import cn.com.nla.common.video.core.service.video.DeviceVoService;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.sip.cmd.SIPCommander;
import cn.com.nla.video.command.VideoCommandDispatcher;
import cn.com.nla.video.command.VideoCommandKeys;
import cn.com.nla.video.command.VideoCommandNotifier;
import cn.com.nla.video.domain.vo.VideoCommandResultVo;
import cn.com.nla.video.domain.vo.VideoCommandTicketVo;
import cn.com.nla.video.service.impl.VideoPtzServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

/**
 * SSE 指令回执契约：受理凭据、路由键去重、回执归一化与超时兜底，不启动 SIP/ZLM/Redis。
 *
 * <p>DeferredResultHolder 用内存路由桩替代 Redis 发布订阅，模拟协议侧 ACK 完整走一遍
 * 「受理 → 回执 → SSE 推送」链路；云台指令同时锁定指令码映射与设备缺失时的快速失败。</p>
 */
class VideoCommandContractTest {

    private static final long TEST_USER_ID = 1L;

    private DeferredResultHolder holder;
    private VideoCommandNotifier notifier;
    private Map<String, VideoRestResult<Object>> results;
    private Set<String> pendingKeys;
    private List<VideoCommandResultVo> receipts;
    private VideoCommandDispatcher dispatcher;

    @BeforeEach
    void prepareDispatcherAndInMemoryRoute() {
        VideoSpringSupport.ensureInstalled();
        results = new ConcurrentHashMap<>();
        pendingKeys = ConcurrentHashMap.newKeySet();
        receipts = new CopyOnWriteArrayList<>();
        holder = Mockito.mock(DeferredResultHolder.class);
        doAnswer(invocation -> pendingKeys.contains(invocation.getArgument(0)))
            .when(holder).exist(anyString(), isNull());
        doAnswer(invocation -> {
            pendingKeys.add(invocation.getArgument(0));
            results.put(invocation.getArgument(1), invocation.getArgument(2));
            return null;
        }).when(holder).put(anyString(), anyString(), any(VideoRestResult.class));
        doAnswer(invocation -> {
            pendingKeys.remove(invocation.getArgument(0));
            VideoRestResult<Object> result = results.get(invocation.getArgument(1));
            if (result != null) {
                result.complete(invocation.getArgument(2));
            }
            return null;
        }).when(holder).invokeResult(anyString(), anyString(), any());
        notifier = Mockito.mock(VideoCommandNotifier.class);
        doAnswer(invocation -> {
            receipts.add((VideoCommandResultVo) invocation.getArgument(1));
            return null;
        }).when(notifier).push(any(), any(VideoCommandResultVo.class));
        dispatcher = new FixedUserDispatcher(holder, notifier, TEST_USER_ID);
    }

    @Test
    void dispatchReturnsTicketAndPushesNormalizedSuccessReceipt() {
        String key = VideoCommandKeys.play("设备一", "通道一");
        VideoCommandTicketVo ticket = dispatcher.dispatch("play", key, handle -> handle.success("流地址"));
        assertEquals("play", ticket.getAction());
        assertEquals(key, ticket.getKey());
        assertEquals(VideoCommandDispatcher.DEFAULT_TIMEOUT_MILLIS, ticket.getTimeout());
        assertEquals(32, ticket.getRequestId().length());
        verify(holder).put(key, ticket.getRequestId(), results.get(ticket.getRequestId()));
        awaitReceipts(1);
        VideoCommandResultVo receipt = receipts.getFirst();
        assertEquals(ticket.getRequestId(), receipt.getRequestId());
        assertEquals("play", receipt.getAction());
        assertEquals(key, receipt.getKey());
        assertEquals(Boolean.TRUE, receipt.getSuccess());
        assertEquals(RespCode.CODE_0.getValue(), receipt.getCode());
        assertEquals("成功", receipt.getMessage());
        assertEquals("流地址", receipt.getData());
    }

    @Test
    void dispatchNormalizesMapAndRawPayloads() {
        dispatcher.dispatch("record-info", VideoCommandKeys.recordInfo("设备一", 1),
            handle -> handle.complete(Map.of("code", 0, "msg", "ok", "data", List.of("录像一"))));
        dispatcher.dispatch("device-info", VideoCommandKeys.deviceInfo("设备一", "通道一"),
            handle -> handle.complete("裸载荷"));
        awaitReceipts(2);
        assertEquals("ok", receipts.get(0).getMessage());
        assertEquals(List.of("录像一"), receipts.get(0).getData());
        assertEquals(RespCode.CODE_0.getValue(), receipts.get(1).getCode());
        assertEquals("裸载荷", receipts.get(1).getData());
    }

    @Test
    void dispatchFailureReceiptKeepsProtocolCodeAndMessage() {
        dispatcher.dispatch("device-status", VideoCommandKeys.deviceStatus("设备一"),
            handle -> handle.fail(RespCode.CODE_2.getValue(), "设备无应答"));
        awaitReceipts(1);
        assertEquals(Boolean.FALSE, receipts.getFirst().getSuccess());
        assertEquals(RespCode.CODE_2.getValue(), receipts.getFirst().getCode());
        assertEquals("设备无应答", receipts.getFirst().getMessage());
    }

    @Test
    void dispatchTimesOutWithConfiguredFallbackResult() throws InterruptedException {
        Supplier<Object> timeoutResult = () -> ProtocolResult.result(RespCode.CODE_2.getValue(), "查询超时");
        VideoCommandTicketVo ticket = dispatcher.dispatch("record-query", VideoCommandKeys.recordInfo("设备二", 2),
            80L, timeoutResult, handle -> { });
        assertEquals(80L, ticket.getTimeout());
        awaitReceipts(1);
        assertEquals(Boolean.FALSE, receipts.getFirst().getSuccess());
        assertEquals(RespCode.CODE_2.getValue(), receipts.getFirst().getCode());
        assertEquals("查询超时", receipts.getFirst().getMessage());
    }

    @Test
    void dispatchWithoutFallbackTimesOutAsProtocolError() throws InterruptedException {
        dispatcher.dispatch("record-query", VideoCommandKeys.recordInfo("设备二", 2), 80L, (Supplier<Object>) null,
            handle -> { });
        awaitReceipts(1);
        assertEquals(Boolean.FALSE, receipts.getFirst().getSuccess());
        assertEquals(RespCode.CODE_2.getValue(), receipts.getFirst().getCode());
        assertEquals("Video request timed out", receipts.getFirst().getMessage());
    }

    @Test
    void dispatchRejectsDuplicateKeyWhilePending() {
        String key = VideoCommandKeys.deviceControl("设备一", "通道一");
        dispatcher.dispatch("device-control", key, handle -> { });
        ServiceException conflict = assertThrows(ServiceException.class,
            () -> dispatcher.dispatch("device-control", key, handle -> { }));
        assertTrue(conflict.getMessage().contains("相同指令"));
    }

    @Test
    void dispatchFallsBackToFailureWhenSenderThrows() {
        dispatcher.dispatch("ptz", VideoCommandKeys.deviceControl("设备三", "通道一"),
            handle -> {
                throw new IllegalStateException("SIP 未初始化");
            });
        awaitReceipts(1);
        assertEquals(Boolean.FALSE, receipts.getFirst().getSuccess());
        assertEquals(RespCode.CODE_2.getValue(), receipts.getFirst().getCode());
        assertEquals("命令发送失败", receipts.getFirst().getMessage());
    }

    @Test
    void dispatchWithoutLoginUserStillDeliversReceipt() {
        new FixedUserDispatcher(holder, notifier, null)
            .dispatch("device-info", VideoCommandKeys.deviceInfo("设备四", "通道一"), handle -> handle.success("ok"));
        awaitReceipts(1);
        verify(notifier).push(isNull(), any(VideoCommandResultVo.class));
    }

    @Test
    void ptzCommandsTranslateToSipFrontEndCmd() throws Exception {
        var sipCommander = Mockito.mock(SIPCommander.class);
        var sipServer = Mockito.mock(SipServer.class);
        var deviceService = resetDeviceStub("设备一");
        var ptz = new VideoPtzServiceImpl(sipServer, sipCommander, dispatcher);

        ptz.ptz("设备一", "通道一", "left", 10, 20, 4);
        ptz.ptz("设备一", "通道一", "stop", null, null, null);
        ptz.addPreset("设备一", "通道一", 5);
        ptz.speedCruisePoint("设备一", "通道一", 3, 1000);
        ptz.frontEndCommand("设备一", "通道一", 0xA2, 1, 2, 3);
        verify(sipCommander).frontEndCmd(sipServer, deviceService.findDeviceGbId("设备一"), "通道一", 2, 10, 20, 4,
            null, null);
        verify(sipCommander).frontEndCmd(sipServer, deviceService.findDeviceGbId("设备一"), "通道一", 0, 0, 0, 0,
            null, null);
        verify(sipCommander).frontEndCmd(sipServer, deviceService.findDeviceGbId("设备一"), "通道一", 0x81, 1, 5, 0,
            null, null);
        verify(sipCommander).frontEndCmd(sipServer, deviceService.findDeviceGbId("设备一"), "通道一", 0x86, 3,
            1000 & 0xFF, 1000 >> 8, null, null);
        verify(sipCommander).frontEndCmd(sipServer, deviceService.findDeviceGbId("设备一"), "通道一", 0xA2, 1, 2, 3,
            null, null);
        // 参数越界在业务层快速失败，不下发 SIP
        assertTrue(assertThrows(ServiceException.class,
            () -> ptz.frontEndCommand("设备一", "通道一", 0xA2, 256, 2, 3)).getMessage().contains("parameter1"));
    }

    @Test
    void presetQueryFailsReceiptWhenDeviceMissing() throws Exception {
        var sipCommander = Mockito.mock(SIPCommander.class);
        var sipServer = Mockito.mock(SipServer.class);
        // 复位共享桩后不设置返回值，findDeviceGbId 默认返回 null
        Mockito.reset(VideoSpringSupport.mock(DeviceVoService.class));
        var ptz = new VideoPtzServiceImpl(sipServer, sipCommander, dispatcher);

        VideoCommandTicketVo ticket = ptz.presetQuery("不存在", "通道一");
        awaitReceipts(1);
        assertEquals(32, ticket.getRequestId().length());
        assertEquals(Boolean.FALSE, receipts.getFirst().getSuccess());
        assertEquals(RespCode.CODE_2.getValue(), receipts.getFirst().getCode());
        assertTrue(receipts.getFirst().getMessage().contains("未找到"));
        verify(sipCommander, Mockito.never()).presetQuery(Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any());
    }

    /**
     * 复位共享的 DeviceVoService 桩并按国标编号返回构造的设备视图。
     *
     * @param deviceId 需要可命中的设备国标编号
     * @return 桩实例
     */
    private DeviceVoService resetDeviceStub(String deviceId) {
        DeviceVoService deviceService = VideoSpringSupport.mock(DeviceVoService.class);
        DeviceVo deviceVo = new DeviceVo();
        deviceVo.setDeviceId(deviceId);
        Mockito.reset(deviceService);
        Mockito.when(deviceService.findDeviceGbId(deviceId)).thenReturn(deviceVo);
        return deviceService;
    }

    /**
     * 等待 SSE 回执到达，超时 5 秒视为丢失。
     *
     * @param expected 期望的回执条数
     */
    private void awaitReceipts(int expected) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (receipts.size() < expected && System.nanoTime() < deadline) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        assertEquals(expected, receipts.size());
    }

    /**
     * 固定发起用户的调度器，绕开 sa-token 登录上下文。
     */
    private static final class FixedUserDispatcher extends VideoCommandDispatcher {

        private final Long userId;

        private FixedUserDispatcher(DeferredResultHolder holder, VideoCommandNotifier notifier, Long userId) {
            super(holder, notifier);
            this.userId = userId;
        }

        @Override
        protected Long currentUserId() {
            return userId;
        }
    }
}
