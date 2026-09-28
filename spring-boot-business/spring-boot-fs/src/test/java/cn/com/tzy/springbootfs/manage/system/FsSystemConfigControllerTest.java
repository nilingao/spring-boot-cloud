package cn.com.tzy.springbootfs.manage.system;

import cn.com.tzy.springbootcomm.common.vo.PageResult;
import cn.com.tzy.springbootcomm.common.vo.RespCode;
import cn.com.tzy.springbootcomm.common.vo.RestResult;
import cn.com.tzy.springbootfs.controller.api.fs.manage.system.*;
import cn.com.tzy.springbootfs.service.manage.system.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 1 回归测试：验证六个系统配置页面的 Controller 路径、HTTP 方法和参数约定一致。
 */
class FsSystemConfigControllerTest {

    private MockMvc mockMvc;

    private PlatformManageService platformService;
    private FsGatewayManageService fsGatewayService;
    private RouteGatewayManageService routeGatewayService;
    private RouteGroupManageService routeGroupService;
    private RouteCallManageService routeCallService;

    @BeforeEach
    void setUp() {
        platformService = mock(PlatformManageService.class);
        fsGatewayService = mock(FsGatewayManageService.class);
        routeGatewayService = mock(RouteGatewayManageService.class);
        routeGroupService = mock(RouteGroupManageService.class);
        routeCallService = mock(RouteCallManageService.class);

        mockMvc = MockMvcBuilders.standaloneSetup(
                new PlatformManageController(platformService),
                new FsGatewayManageController(fsGatewayService),
                new RouteGatewayManageController(routeGatewayService),
                new RouteGroupManageController(routeGroupService),
                new RouteCallManageController(routeCallService)
        ).build();
    }

    // ---- /api/fs/platform ----

    @Test
    void platformPageUsesPost() throws Exception {
        when(platformService.page(any())).thenReturn(PageResult.result(RespCode.CODE_0.getValue(), "ok"));
        mockMvc.perform(post("/api/fs/platform/page").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void platformDetailUsesGet() throws Exception {
        when(platformService.detail(1L)).thenReturn(RestResult.result(RespCode.CODE_0.getValue(), "ok"));
        mockMvc.perform(get("/api/fs/platform/detail").param("id", "1")).andExpect(status().isOk());
    }

    @Test
    void platformRemoveUsesDelete() throws Exception {
        when(platformService.remove(1L)).thenReturn(RestResult.result(RespCode.CODE_0.getValue(), "ok"));
        mockMvc.perform(delete("/api/fs/platform/remove").param("id", "1")).andExpect(status().isOk());
    }

    // ---- /api/fs/media_server ----

    // ---- /api/fs/fs_gateway ----

    @Test
    void fsGatewayPageUsesPost() throws Exception {
        when(fsGatewayService.page(any())).thenReturn(PageResult.result(RespCode.CODE_0.getValue(), "ok"));
        mockMvc.perform(post("/api/fs/fs_gateway/page").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void fsGatewaySelectedUsesPost() throws Exception {
        when(fsGatewayService.selected(any())).thenReturn(RestResult.result(RespCode.CODE_0.getValue(), "ok"));
        mockMvc.perform(post("/api/fs/fs_gateway/selected")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"id\":1,\"selected\":\"1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void fsGatewayRemoveUsesDelete() throws Exception {
        when(fsGatewayService.remove(1L)).thenReturn(RestResult.result(RespCode.CODE_0.getValue(), "ok"));
        mockMvc.perform(delete("/api/fs/fs_gateway/remove").param("id", "1")).andExpect(status().isOk());
    }

    // ---- /api/fs/route_gateway ----

    @Test
    void routeGatewayPageUsesPost() throws Exception {
        when(routeGatewayService.page(any())).thenReturn(PageResult.result(RespCode.CODE_0.getValue(), "ok"));
        mockMvc.perform(post("/api/fs/route_gateway/page").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void routeGatewayStatusUsesPost() throws Exception {
        when(routeGatewayService.status(any())).thenReturn(RestResult.result(RespCode.CODE_0.getValue(), "ok"));
        mockMvc.perform(post("/api/fs/route_gateway/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"id\":1,\"status\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    void routeGatewayRemoveUsesDelete() throws Exception {
        when(routeGatewayService.remove(1L)).thenReturn(RestResult.result(RespCode.CODE_0.getValue(), "ok"));
        mockMvc.perform(delete("/api/fs/route_gateway/remove").param("id", "1")).andExpect(status().isOk());
    }

    // ---- /api/fs/route_group ----

    @Test
    void routeGroupPageUsesPost() throws Exception {
        when(routeGroupService.page(any())).thenReturn(PageResult.result(RespCode.CODE_0.getValue(), "ok"));
        mockMvc.perform(post("/api/fs/route_group/page").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void routeGroupSaveGatewaysUsesPost() throws Exception {
        when(routeGroupService.saveGateways(any())).thenReturn(RestResult.result(RespCode.CODE_0.getValue(), "ok"));
        mockMvc.perform(post("/api/fs/route_group/save_gateways")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"routeGroupId\":1,\"gatewayIds\":[]}"))
                .andExpect(status().isOk());
    }

    @Test
    void routeGroupRemoveUsesDelete() throws Exception {
        when(routeGroupService.remove(1L)).thenReturn(RestResult.result(RespCode.CODE_0.getValue(), "ok"));
        mockMvc.perform(delete("/api/fs/route_group/remove").param("id", "1")).andExpect(status().isOk());
    }

    // ---- /api/fs/route_call ----

    @Test
    void routeCallPageUsesPost() throws Exception {
        when(routeCallService.page(any())).thenReturn(PageResult.result(RespCode.CODE_0.getValue(), "ok"));
        mockMvc.perform(post("/api/fs/route_call/page").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void routeCallStatusUsesPost() throws Exception {
        when(routeCallService.status(any())).thenReturn(RestResult.result(RespCode.CODE_0.getValue(), "ok"));
        mockMvc.perform(post("/api/fs/route_call/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"id\":1,\"status\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    void routeCallRemoveUsesDelete() throws Exception {
        when(routeCallService.remove(1L)).thenReturn(RestResult.result(RespCode.CODE_0.getValue(), "ok"));
        mockMvc.perform(delete("/api/fs/route_call/remove").param("id", "1")).andExpect(status().isOk());
    }
}
