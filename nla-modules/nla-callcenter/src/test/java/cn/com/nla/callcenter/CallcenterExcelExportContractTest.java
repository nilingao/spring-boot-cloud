package cn.com.nla.callcenter;

import cn.com.nla.callcenter.controller.FsGatewayController;
import cn.com.nla.callcenter.controller.PlatformController;
import cn.com.nla.callcenter.controller.RouteCallController;
import cn.com.nla.callcenter.controller.RouteGatewayController;
import cn.com.nla.callcenter.controller.RouteGroupController;
import cn.com.nla.callcenter.domain.bo.GateWayBo;
import cn.com.nla.callcenter.domain.bo.PlatformBo;
import cn.com.nla.callcenter.domain.bo.RouteCallBo;
import cn.com.nla.callcenter.domain.bo.RouteGatewayBo;
import cn.com.nla.callcenter.domain.bo.RouteGroupBo;
import cn.com.nla.callcenter.domain.vo.GateWayVo;
import cn.com.nla.callcenter.domain.vo.PlatformVo;
import cn.com.nla.callcenter.domain.vo.RouteCallVo;
import cn.com.nla.callcenter.domain.vo.RouteGatewayVo;
import cn.com.nla.callcenter.domain.vo.RouteGroupVo;
import cn.com.nla.callcenter.service.IGateWayService;
import cn.com.nla.callcenter.service.IPlatformService;
import cn.com.nla.callcenter.service.IRouteCallService;
import cn.com.nla.callcenter.service.IRouteGatewayService;
import cn.com.nla.callcenter.service.IRouteGroupService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifies the five callcenter Excel controller exports write downloadable XLSX responses. */
class CallcenterExcelExportContractTest {

    @Test
    void exportsWriteValidXlsxAttachmentsForAllCallcenterManagementDomains() throws Exception {
        assertExport("FS注册网关",
            response -> controllerForGateway(response).export(new GateWayBo(), response));
        assertExport("FreeSWITCH 平台",
            response -> controllerForPlatform(response).export(new PlatformBo(), response));
        assertExport("呼叫路由",
            response -> controllerForRouteCall(response).export(new RouteCallBo(), response));
        assertExport("路由网关",
            response -> controllerForRouteGateway(response).export(new RouteGatewayBo(), response));
        assertExport("路由组",
            response -> controllerForRouteGroup(response).export(new RouteGroupBo(), response));
    }

    private static FsGatewayController controllerForGateway(HttpServletResponse response) {
        IGateWayService service = mock(IGateWayService.class);
        when(service.queryList(any())).thenReturn(List.of(new GateWayVo()));
        return new FsGatewayController(service);
    }

    private static PlatformController controllerForPlatform(HttpServletResponse response) {
        IPlatformService service = mock(IPlatformService.class);
        when(service.queryList(any())).thenReturn(List.of(new PlatformVo()));
        return new PlatformController(service);
    }

    private static RouteCallController controllerForRouteCall(HttpServletResponse response) {
        IRouteCallService service = mock(IRouteCallService.class);
        when(service.queryList(any())).thenReturn(List.of(new RouteCallVo()));
        return new RouteCallController(service);
    }

    private static RouteGatewayController controllerForRouteGateway(HttpServletResponse response) {
        IRouteGatewayService service = mock(IRouteGatewayService.class);
        when(service.queryList(any())).thenReturn(List.of(new RouteGatewayVo()));
        return new RouteGatewayController(service);
    }

    private static RouteGroupController controllerForRouteGroup(HttpServletResponse response) {
        IRouteGroupService service = mock(IRouteGroupService.class);
        when(service.queryList(any())).thenReturn(List.of(new RouteGroupVo()));
        return new RouteGroupController(service);
    }

    private static void assertExport(String sheetName, ExportInvocation invocation) throws Exception {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new ByteArrayServletOutputStream(body));

        invocation.export(response);

        verify(response).setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
        org.mockito.ArgumentCaptor<String> disposition = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(org.mockito.ArgumentMatchers.eq("Content-disposition"), disposition.capture());
        assertThat(disposition.getValue()).contains(".xlsx");
        assertThat(body.size()).isGreaterThan(0);
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(body.toByteArray()), StandardCharsets.UTF_8)) {
            assertThat(zip.getNextEntry()).as("XLSX is an OOXML ZIP package").isNotNull();
        }
        assertThat(sheetName).isNotBlank();
    }

    @FunctionalInterface
    private interface ExportInvocation {
        void export(HttpServletResponse response);
    }

    private static final class ByteArrayServletOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream delegate;

        private ByteArrayServletOutputStream(ByteArrayOutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
        }

        @Override
        public void write(int value) throws IOException {
            delegate.write(value);
        }
    }
}
