package cn.com.tzy.springbootactiviti.service.impl;

import cn.com.tzy.spingbootstartermybatis.core.tenant.context.TenantContextHolder;
import cn.com.tzy.springbootactiviti.config.activiti.resolver.TaskCandidateResolver;
import cn.com.tzy.springbootactiviti.config.init.SpringContextConfig;
import cn.com.tzy.springbootactiviti.model.impl.WorkflowContextImpl;
import cn.com.tzy.springbootactiviti.oa.OaInterface;
import cn.com.tzy.springbootactiviti.service.ActivitiService;
import cn.com.tzy.springbootactiviti.utils.OAEnum;
import cn.com.tzy.springbootcomm.common.model.PageModel;
import cn.com.tzy.springbootcomm.common.vo.PageResult;
import cn.com.tzy.springbootcomm.common.vo.RespCode;
import cn.com.tzy.springbootcomm.common.vo.RestResult;
import cn.com.tzy.springbootcomm.constant.NotNullMap;
import cn.com.tzy.springbootcomm.utils.AppUtils;
import cn.com.tzy.springbootcomm.utils.JwtUtils;
import cn.com.tzy.springbootentity.dome.bean.User;
import cn.com.tzy.springbootentity.param.activiti.impl.CommentEntity;
import cn.com.tzy.springbootfeignbean.api.bean.UserServiceFeign;
import cn.com.tzy.springbootfeignbean.api.sys.DictionaryItemServiceFeign;
import de.odysseus.el.ExpressionFactoryImpl;
import de.odysseus.el.util.SimpleContext;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.*;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.el.ExpressionManager;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
@Service
public class ActivitiServcieImpl extends BaseWorkflowService implements ActivitiService {


    @Autowired
    private UserServiceFeign userServiceFeign;
    @Autowired
    private DictionaryItemServiceFeign dictionaryItemServiceFeign;
    @Autowired
    private ExpressionManager expressionManager;
    @Autowired
    private ProcessEngineConfigurationImpl processEngineConfiguration;
    @Autowired
    private TaskCandidateResolver taskCandidateResolver;


    @Override
    public RestResult<?> statsUserOa() {
        Long userId = JwtUtils.getUserId();
        NotNullMap map = new NotNullMap();
        map.putLong("userNeedCount",getProcessEngine().getTaskService().createTaskQuery().taskTenantId(TenantContextHolder.getTenantId().toString()).taskCandidateOrAssigned(String.valueOf(userId)).count());
        map.putLong("userLaunchCount",getProcessEngine().getHistoryService().createHistoricProcessInstanceQuery().processInstanceTenantId(TenantContextHolder.getTenantId().toString()).startedBy(String.valueOf(userId)).count());
        map.putLong("userAlreadyCount",getProcessEngine().getHistoryService().createHistoricTaskInstanceQuery().taskTenantId(TenantContextHolder.getTenantId().toString()).finished().taskAssignee(String.valueOf(userId)).count());
        map.putLong("totalNeedCount",getProcessEngine().getTaskService().createTaskQuery().taskTenantId(TenantContextHolder.getTenantId().toString()).count());
        map.putLong("totalAlreadyCount",getProcessEngine().getHistoryService().createHistoricProcessInstanceQuery().processInstanceTenantId(TenantContextHolder.getTenantId().toString()).finished().count());
        return RestResult.result(RespCode.CODE_0.getValue(),"查询成功",map);
    }

    @Override
    public PageResult findUserNeedList(PageModel pageModel) {
        Long userId = JwtUtils.getUserId();
        PageResult result = null;
        try {
            long count = getProcessEngine().getTaskService().createTaskQuery().taskTenantId(TenantContextHolder.getTenantId().toString()).taskCandidateOrAssigned(String.valueOf(userId)).count();
            List<Task> tasks = getProcessEngine().getTaskService().createTaskQuery()
                    .includeProcessVariables()
                    .taskTenantId(TenantContextHolder.getTenantId().toString())
                    .orderByTaskCreateTime().desc()
                    .taskCandidateOrAssigned(String.valueOf(userId)).listPage(pageModel.getStartRow(), pageModel.getPageSize());
            //批量获取流程实例，避免在循环中逐条查询
            Map<String, ProcessInstance> instanceMap = findProcessInstanceMap(tasks.stream()
                    .map(Task::getProcessInstanceId).collect(Collectors.toSet()));
            List<NotNullMap> maps = new ArrayList<>();
            for (Task task: tasks) {
                ProcessInstance processInstance = instanceMap.get(task.getProcessInstanceId());
                NotNullMap map = new NotNullMap();
                map.put("instanceId",task.getProcessInstanceId());
                map.put("instanceName",processInstance == null ? null : processInstance.getName());
                map.put("isSuspended",processInstance != null && processInstance.isSuspended());
                map.put("taskId",task.getId());
                map.put("taskName",task.getName());
                map.put("assignee",task.getAssignee());
                map.put("businessKey",task.getBusinessKey());
                map.put("processVariables",task.getProcessVariables());
                map.put("processDefinitionId",task.getProcessDefinitionId());
                map.put("tackComment",findCommentTaskEntity(task.getId()));
                map.put("instanceComment",findCommentInstanceEntity(task.getProcessInstanceId()));
                map.putDateTime("createTime",task.getCreateTime());
                map.putDateTime("claimTime",task.getClaimTime());
                map.putDateTime("dueDate",task.getDueDate());
                maps.add(map);
            }
            result = PageResult.result(RespCode.CODE_0.getValue(),null,maps,(int) count);
        } catch (Exception e) {
            log.error("获取待办流程错误:",e);
            result = PageResult.result(RespCode.CODE_2.getValue(),"获取待办流程错误");
        }
        return result;
    }

    @Override
    public PageResult findUserLaunchList(PageModel pageModel) {
        Long userId = JwtUtils.getUserId();
        PageResult result = null;
        try {
            long count = getProcessEngine().getHistoryService().createHistoricProcessInstanceQuery()
                    .processInstanceTenantId(TenantContextHolder.getTenantId().toString())
                    .startedBy(String.valueOf(userId))
                    .count();
            List<HistoricProcessInstance> historicProcessInstances = getProcessEngine().getHistoryService().createHistoricProcessInstanceQuery()
                    .startedBy(String.valueOf(userId))
                    .processInstanceTenantId(TenantContextHolder.getTenantId().toString())
                    .includeProcessVariables()
                    .orderByProcessInstanceStartTime().desc()
                    .listPage(pageModel.getStartRow(), pageModel.getPageSize());
            //批量获取每个实例最新的任务节点，避免在循环中逐条查询
            Map<String, HistoricTaskInstance> taskInstanceMap = findLatestInstanceTaskMap(historicProcessInstances.stream()
                    .map(HistoricProcessInstance::getId).collect(Collectors.toList()));
            //仅未结束的实例需要查询运行时挂起状态，同样批量获取
            Map<String, ProcessInstance> runningInstanceMap = findProcessInstanceMap(historicProcessInstances.stream()
                    .filter(instance -> instance.getEndTime() == null)
                    .map(HistoricProcessInstance::getId).collect(Collectors.toSet()));
            List<NotNullMap> maps = new ArrayList<>();
            for (HistoricProcessInstance instance: historicProcessInstances) {
                NotNullMap map = new NotNullMap();
                HistoricTaskInstance taskInstance = taskInstanceMap.get(instance.getId());
                map.put("instanceId",instance.getId());
                map.put("instanceName",instance.getName());
                map.put("taskId",taskInstance == null ? null : taskInstance.getId());
                map.put("taskName",taskInstance == null ? null : taskInstance.getName());
                map.put("isSuspended",false);
                ProcessInstance processInstance = runningInstanceMap.get(instance.getId());
                if(processInstance != null){
                    map.put("isSuspended",processInstance.isSuspended());
                }
                map.put("tackComment",taskInstance == null ? null : findCommentTaskEntity(taskInstance.getId()));
                map.put("instanceComment",findCommentInstanceEntity(instance.getId()));
                map.put("processVariables", instance.getProcessVariables());
                map.put("processDefinitionName",instance.getProcessDefinitionName());
                map.put("processDefinitionId",instance.getProcessDefinitionId());
                map.put("businessKey",instance.getBusinessKey());
                map.putDateTime("startTime",instance.getStartTime());
                map.putDateTime("endTime",instance.getEndTime());
                maps.add(map);
            }
            result = PageResult.result(RespCode.CODE_0.getValue(),null,maps,(int) count);
        } catch (Exception e) {
            log.error("获取发起流程信息错误:",e);
            result = PageResult.result(RespCode.CODE_2.getValue(),"获取发起流程信息错误");
        }

        return result;
    }

    @Override
    public PageResult findUserAlreadyList(PageModel pageModel) {
        Long userId = JwtUtils.getUserId();
        PageResult result = null;
        try {
            long count = getProcessEngine().getHistoryService().createHistoricTaskInstanceQuery()
                    .finished()
                    .taskTenantId(TenantContextHolder.getTenantId().toString())
                    .taskAssignee(String.valueOf(userId))
                    .count();
            List<HistoricTaskInstance> historicTaskInstances = getProcessEngine().getHistoryService().createHistoricTaskInstanceQuery()
                    .finished()
                    .taskTenantId(TenantContextHolder.getTenantId().toString())
                    .includeProcessVariables()
                    .taskAssignee(String.valueOf(userId))
                    .orderByHistoricTaskInstanceStartTime().desc()
                    .listPage(pageModel.getStartRow(), pageModel.getPageSize());

            //批量获取历史流程实例，避免在循环中逐条查询
            Map<String, HistoricProcessInstance> instanceMap = findHistoricProcessInstanceMap(historicTaskInstances.stream()
                    .map(HistoricTaskInstance::getProcessInstanceId).collect(Collectors.toSet()));
            List<NotNullMap> maps = new ArrayList<>();
            for (HistoricTaskInstance task: historicTaskInstances) {
                NotNullMap map = new NotNullMap();
                HistoricProcessInstance processInstance = instanceMap.get(task.getProcessInstanceId());
                map.put("instanceId",task.getProcessInstanceId());
                map.put("instanceName",processInstance == null ? null : processInstance.getName());
                map.put("taskId",task.getId());
                map.put("taskName",task.getName());
                map.put("processVariables", task.getProcessVariables());
                map.put("processDefinitionName",processInstance == null ? null : processInstance.getProcessDefinitionName());
                map.put("processDefinitionId",processInstance == null ? null : processInstance.getProcessDefinitionId());
                map.put("tackComment",findCommentTaskEntity(task.getId()));
                map.put("instanceComment",findCommentInstanceEntity(task.getProcessInstanceId()));
                map.put("businessKey",processInstance == null ? null : processInstance.getBusinessKey());
                map.putDateTime("startTime",task.getStartTime());
                map.putDateTime("endTime",task.getEndTime());
                maps.add(map);
            }
            result = PageResult.result(RespCode.CODE_0.getValue(),null,maps,(int) count);
        } catch (Exception e) {
            log.error("获取历史流程错误:",e);
            result = PageResult.result(RespCode.CODE_2.getValue(),"获取历史流程错误");
        }
        return result;
    }

    @Override
    public PageResult findRepositoryList(PageModel pageModel) {
        PageResult result = null;
        try {
            long count = getProcessEngine().getRepositoryService().createProcessDefinitionQuery()
                    .latestVersion()
                    .processDefinitionTenantId(TenantContextHolder.getTenantId().toString())
                    .count();
            List<ProcessDefinition> processDefinitionList = getProcessEngine().getRepositoryService().createProcessDefinitionQuery()
                    .latestVersion()
                    .processDefinitionTenantId(TenantContextHolder.getTenantId().toString())
                    //必须显式指定排序，否则分页结果顺序不稳定，翻页会出现重复或遗漏
                    .orderByProcessDefinitionKey().asc()
                    .listPage(pageModel.getStartRow(), pageModel.getPageSize());
            List<NotNullMap> maps = new ArrayList<>();
            for (ProcessDefinition processDefinition: processDefinitionList) {
                NotNullMap map = new NotNullMap();
                map.put("id",processDefinition.getId());
                map.put("key",processDefinition.getKey());
                map.put("version",processDefinition.getVersion());
                map.put("deploymentId",processDefinition.getDeploymentId());
                map.put("isSuspended",processDefinition.isSuspended());
                map.put("deploymentName",processDefinition.getName());
                map.put("diagramResourceName",processDefinition.getDiagramResourceName());
                maps.add(map);
            }
            result = PageResult.result(RespCode.CODE_0.getValue(),null,maps,(int) count);
        } catch (Exception e) {
            log.error("获取流程定义列表错误:",e);
            result = PageResult.result(RespCode.CODE_2.getValue(),"获取流程定义列表错误");
        }
        return result;
    }

    @Override
    public PageResult findNeedList(PageModel pageModel) {
        PageResult result = null;
        try {
            long count = getProcessEngine().getTaskService().createTaskQuery().taskTenantId(TenantContextHolder.getTenantId().toString()).count();
            List<Task> tasks = getProcessEngine().getTaskService().createTaskQuery()
                    .includeProcessVariables()
                    .taskTenantId(TenantContextHolder.getTenantId().toString())
                    .orderByTaskCreateTime().desc()
                    .listPage(pageModel.getStartRow(), pageModel.getPageSize());
            //批量获取流程实例，避免在循环中逐条查询
            Map<String, ProcessInstance> instanceMap = findProcessInstanceMap(tasks.stream()
                    .map(Task::getProcessInstanceId).collect(Collectors.toSet()));
            List<NotNullMap> maps = new ArrayList<>();
            for (Task task: tasks) {
                ProcessInstance processInstance = instanceMap.get(task.getProcessInstanceId());
                NotNullMap map = new NotNullMap();
                map.put("instanceId",task.getProcessInstanceId());
                map.put("instanceName",processInstance == null ? null : processInstance.getName());
                map.put("isSuspended",processInstance != null && processInstance.isSuspended());
                map.put("taskId",task.getId());
                map.put("taskName",task.getName());
                map.put("assignee",task.getAssignee());
                map.put("businessKey",task.getBusinessKey());
                map.put("processVariables",task.getProcessVariables());
                map.put("processDefinitionId",task.getProcessDefinitionId());
                map.put("processDefinitionName",processInstance == null ? null : processInstance.getProcessDefinitionName());
                map.put("tackComment",findCommentTaskEntity(task.getId()));
                map.put("instanceComment",findCommentInstanceEntity(task.getProcessInstanceId()));
                map.putDateTime("createTime",task.getCreateTime());
                map.putDateTime("claimTime",task.getClaimTime());
                map.putDateTime("dueDate",task.getDueDate());
                maps.add(map);
            }
            result = PageResult.result(RespCode.CODE_0.getValue(),null,maps,(int) count);
        } catch (Exception e) {
            log.error("获取待办流程错误:",e);
            result = PageResult.result(RespCode.CODE_2.getValue(),"获取待办流程错误");
        }

        return result;
    }

    @Override
    public PageResult findAlreadyList(PageModel pageModel) {
        PageResult result = null;
        try {
            long count = getProcessEngine().getHistoryService().createHistoricProcessInstanceQuery()
                    .finished()
                    .processInstanceTenantId(TenantContextHolder.getTenantId().toString())
                    .count();
            List<HistoricProcessInstance> historicProcessInstances = getProcessEngine().getHistoryService().createHistoricProcessInstanceQuery()
                    .finished()
                    .processInstanceTenantId(TenantContextHolder.getTenantId().toString())
                    .includeProcessVariables()
                    .orderByProcessInstanceStartTime().desc()
                    .listPage(pageModel.getStartRow(), pageModel.getPageSize());
            //批量获取每个实例最新的任务节点，避免在循环中逐条查询
            Map<String, HistoricTaskInstance> instanceTaskMap = findLatestInstanceTaskMap(historicProcessInstances.stream()
                    .map(HistoricProcessInstance::getId).collect(Collectors.toList()));
            List<NotNullMap> maps = new ArrayList<>();
            for (HistoricProcessInstance historicProcessInstance: historicProcessInstances) {
                NotNullMap map = new NotNullMap();
                HistoricTaskInstance instanceTask = instanceTaskMap.get(historicProcessInstance.getId());
                map.put("instanceId",historicProcessInstance.getId());
                map.put("instanceName",historicProcessInstance.getName());
                map.put("taskId",instanceTask == null ? null : instanceTask.getId());
                map.put("taskName",instanceTask == null ? null : instanceTask.getName());
                map.put("processVariables", instanceTask == null ? null : instanceTask.getProcessVariables());
                map.put("processDefinitionName",historicProcessInstance.getProcessDefinitionName());
                map.put("processDefinitionId",historicProcessInstance.getProcessDefinitionId());
                map.put("tackComment",instanceTask == null ? null : findCommentTaskEntity(instanceTask.getId()));
                map.put("instanceComment",findCommentInstanceEntity(historicProcessInstance.getId()));
                map.put("businessKey",historicProcessInstance.getBusinessKey());
                map.putDateTime("startTime",historicProcessInstance.getStartTime());
                map.putDateTime("endTime",historicProcessInstance.getEndTime());
                maps.add(map);
            }
            result = PageResult.result(RespCode.CODE_0.getValue(),null,maps,(int) count);
        } catch (Exception e) {
            log.error("获取历史任务错误:",e);
            result = PageResult.result(RespCode.CODE_2.getValue(),"获取历史任务错误");
        }
        return result;
    }

    @Override
    public RestResult<?> findHistoricalInstanceIdList(String instanceId) {
        RestResult<?> result = null;
        try {
            //--------------------------------------------另一种写法-------------------------
            List<HistoricActivityInstance> historicActivityInstanceList = getProcessEngine().getHistoryService().createHistoricActivityInstanceQuery()
                    .processInstanceId(instanceId)
                    .activityTenantId(TenantContextHolder.getTenantId().toString())
                    .orderByHistoricActivityInstanceStartTime().asc()
                    .orderByHistoricActivityInstanceEndTime().asc()
                    .list();
            List<NotNullMap> maps = historicActivityInstanceList.stream().map(activityInstance -> {
                return new NotNullMap() {{
                    putString("activityName", activityInstance.getActivityName());
                    putString("processInstanceId", activityInstance.getProcessInstanceId());
                    putString("processDefinitionId", activityInstance.getProcessDefinitionId());
                    put("tackComment", findCommentTaskEntity(activityInstance.getTaskId()));
                    putDateTime("startTime", activityInstance.getStartTime());
                    putDateTime("endTime", activityInstance.getEndTime());
                }};
            }).collect(Collectors.toList());
            result = RestResult.result(RespCode.CODE_0.getValue(),null,maps);
        } catch (Exception e) {
            log.error("获取历史任务失败:",e);
            result = RestResult.result(RespCode.CODE_2.getValue(),"获取历史任务失败");
        }
        return result;
    }

    @SneakyThrows
    @Override
    public RestResult<?> findInstanceIdDetail(String instanceId) {

        //获取实例信息
        HistoricProcessInstance instance = getProcessEngine().getHistoryService().createHistoricProcessInstanceQuery()
                .includeProcessVariables()
                .processInstanceTenantId(TenantContextHolder.getTenantId().toString())
                .processInstanceId(instanceId)
                .singleResult();

        if(instance == null){
            return RestResult.result(RespCode.CODE_2.getValue(),"未获取到流程实例");
        }
        //获取节点信息
        HistoricTaskInstance historicTaskInstance = findInstanceTask(instance.getId());

        NotNullMap map = new NotNullMap();
        map.put("taskId", historicTaskInstance.getId());
        map.put("name", historicTaskInstance.getName());
        map.put("assignee",historicTaskInstance.getAssignee());
        map.put("processInstanceId", historicTaskInstance.getProcessInstanceId());
        map.put("processDefinitionId", instance.getProcessDefinitionId());
        map.put("processVariables",instance.getProcessVariables());
        map.put("businessKey", instance.getBusinessKey());
        map.put("processDefinitionKey",  instance.getProcessDefinitionKey());
        map.putDateTime("createTime", historicTaskInstance.getCreateTime());
        map.putDateTime("startTime", historicTaskInstance.getStartTime());
        map.putDateTime("endTime", historicTaskInstance.getEndTime());
        map.putDateTime("claimTime", historicTaskInstance.getClaimTime());
        map.putDateTime("dueDate", historicTaskInstance.getDueDate());
        map.put("tackComment",findCommentTaskEntity(historicTaskInstance.getId()));
        /**
         * 获取基本信息
         */
        String[] split = historicTaskInstance.getProcessDefinitionId().split(":");
        Class<? extends OaInterface> clesses = OAEnum.get(split[0]);
        OaInterface bean= (OaInterface) SpringContextConfig.getBean(clesses);
        map.put("info",  bean.findObject(instance.getBusinessKey()));
        return RestResult.result(RespCode.CODE_0.getValue(),null,map);
    }

    @Override
    public RestResult<?> findRepositoryXml(String processDefinitionId) {
        BpmnModel bpmnModel = getProcessEngine().getRepositoryService().getBpmnModel(processDefinitionId);
        //创建转换对象
        BpmnXMLConverter converter = new BpmnXMLConverter();
        //把bpmnModel对象转换成字符
        byte[] bytes = converter.convertToXML(bpmnModel);
        String xmlContenxt = new String(bytes);
        return RestResult.result(RespCode.CODE_0.getValue(),null,xmlContenxt);
    }
    /**
     * 获取实例参数
     */
    private CommentEntity findCommentInstanceEntity(String instanceId){
        CommentEntity instanceComment =null;
        List<Comment> instanceComments = getProcessEngine().getTaskService().getProcessInstanceComments(instanceId);
        if(!instanceComments.isEmpty()){
            instanceComment = AppUtils.decodeJson2(instanceComments.get(0).getFullMessage(),CommentEntity.class);
        }
        return instanceComment;
    }

    /**
     * 获取节点参数
     */
    private CommentEntity findCommentTaskEntity(String taskId){
        CommentEntity tackComment =null;
        List<Comment> taskComments = getProcessEngine().getTaskService().getTaskComments(taskId);
        if(!taskComments.isEmpty()){
            tackComment = AppUtils.decodeJson2(taskComments.get(0).getFullMessage(),CommentEntity.class);
        }
        return tackComment;
    }
    /**
     * 根据实例编号获取当前实例信息及状态
     */
    private HistoricTaskInstance findInstanceTask(String instanceId){
        List<HistoricTaskInstance> list = getProcessEngine().getHistoryService().createHistoricTaskInstanceQuery()
                .processInstanceId(instanceId)
                .orderByHistoricTaskInstanceStartTime().desc()
                .listPage(0,1);
        return list.get(0);
    }

    /**
     * 批量获取运行时流程实例，用于替代列表循环内的逐条查询
     * @param instanceIds 流程实例编号集合
     * @return 流程实例编号与运行时实例的映射，入参为空时返回空映射
     */
    private Map<String, ProcessInstance> findProcessInstanceMap(Set<String> instanceIds){
        if(instanceIds == null || instanceIds.isEmpty()){
            return Collections.emptyMap();
        }
        return getProcessEngine().getRuntimeService().createProcessInstanceQuery()
                .processInstanceIds(instanceIds)
                .list().stream()
                .collect(Collectors.toMap(ProcessInstance::getId, Function.identity(), (first, second) -> first));
    }

    /**
     * 批量获取历史流程实例，用于替代列表循环内的逐条查询
     * @param instanceIds 流程实例编号集合
     * @return 流程实例编号与历史实例的映射，入参为空时返回空映射
     */
    private Map<String, HistoricProcessInstance> findHistoricProcessInstanceMap(Set<String> instanceIds){
        if(instanceIds == null || instanceIds.isEmpty()){
            return Collections.emptyMap();
        }
        return getProcessEngine().getHistoryService().createHistoricProcessInstanceQuery()
                .processInstanceIds(instanceIds)
                .list().stream()
                .collect(Collectors.toMap(HistoricProcessInstance::getId, Function.identity(), (first, second) -> first));
    }

    /**
     * 批量获取每个流程实例下最新的任务节点，用于替代列表循环内的逐条查询
     * @param instanceIds 流程实例编号集合
     * @return 流程实例编号与其最新任务节点的映射，入参为空时返回空映射
     */
    private Map<String, HistoricTaskInstance> findLatestInstanceTaskMap(List<String> instanceIds){
        if(instanceIds == null || instanceIds.isEmpty()){
            return Collections.emptyMap();
        }
        //按开始时间倒序，键冲突时保留先出现的记录，即每个实例最新的任务节点
        return getProcessEngine().getHistoryService().createHistoricTaskInstanceQuery()
                .processInstanceIdIn(instanceIds)
                .orderByHistoricTaskInstanceStartTime().desc()
                .list().stream()
                .collect(Collectors.toMap(HistoricTaskInstance::getProcessInstanceId, Function.identity(), (first, second) -> first));
    }

    @Override
    void addCommand(String instances, String task, String userId,CommentEntity comment) {
        RestResult<?> result = userServiceFeign.getInfo(Long.valueOf(userId));
        RestResult<?> resultDepartment = userServiceFeign.findUserConnectDepartment(Long.valueOf(userId));
        User user = AppUtils.convertValue2(result.getData(), User.class);
        List<Map> departmentList =AppUtils.convertValue2(resultDepartment.getData(),new TypeReference<List<Map>>(){});
        if(comment == null ){
            comment = new CommentEntity();
        }
        comment.setUserId(user.getId());
        comment.setUserName(user.getUserName());
        if(!departmentList.isEmpty()){
            comment.setDepartmentId(Long.parseLong(String.valueOf(departmentList.get(0).get("departmentId"))));
            comment.setDepartmentName(String.valueOf(departmentList.get(0).get("departmentName")));
        }
        getProcessEngine().getTaskService().deleteComments(task,instances);
        getProcessEngine().getTaskService().addComment(task,instances,AppUtils.encodeJson2(comment));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public RestResult<?> deleteProcessInstance(String processInstanceId,Boolean isUser,String memo) {
        Long userId = JwtUtils.getUserId();
        if(isUser){
            ProcessInstance processInstance = getProcessEngine().getRuntimeService().createProcessInstanceQuery().processInstanceTenantId(TenantContextHolder.getTenantId().toString()).processInstanceId(processInstanceId).singleResult();
            if(processInstance == null){
                return RestResult.result(RespCode.CODE_2.getValue(),"未获取到流程实例");
            }else if(!String.valueOf(userId).equals(processInstance.getStartUserId())){
                return RestResult.result(RespCode.CODE_2.getValue(),"流程创建人与当前人不符，无法删除");
            }
            String[] split = processInstance.getProcessDefinitionId().split(":");
            Class<? extends OaInterface> clesses = OAEnum.get(split[0]);
            OaInterface bean= (OaInterface) SpringContextConfig.getBean(clesses);
            bean.deleteId(processInstance.getBusinessKey());
        }
       return super.deleteProcessInstance(processInstanceId,memo);
    }

    @Override
    public RestResult<?> findNextTaskUser(String taskId,Map<String, Object> vars) {
        Task task = getProcessEngine().getTaskService().createTaskQuery().taskTenantId(TenantContextHolder.getTenantId().toString()).taskId(taskId).singleResult();
        if(task== null){
            return RestResult.result(RespCode.CODE_2.getValue(),"未获取任务节点信息");
        }
        HistoricTaskInstance taskInstance = getProcessEngine().getHistoryService().createHistoricTaskInstanceQuery()
                .taskId(task.getId()).singleResult();
        ProcessDefinition processDefinition = getProcessEngine().getRepositoryService().getProcessDefinition(taskInstance.getProcessDefinitionId());
        Execution execution = getProcessEngine().getRuntimeService().createExecutionQuery().executionId(taskInstance.getExecutionId()).singleResult();

        return findProcessDefinitionIdNextTaskUser(task.getProcessInstanceId(),execution,processDefinition,vars);
    }

    /**
     * 根据流程定义模型信息与当前节点信息获取下一个审批人
     * @param execution
     * @param processDefinition
     * @return
     */
    private RestResult<?> findProcessDefinitionIdNextTaskUser(String processInstanceId,Execution execution,ProcessDefinition processDefinition,Map<String, Object> vars){
        // 获取的下个节点不一定是userTask的任务节点，所以要判断是否是任务节点
        NotNullMap map= new NotNullMap();
        map.putInteger("operationUserType",1);//可操作用户
        map.put("userList",new NotNullMap());//用户信息
        FlowNode nextFlowNode = findNextFlowNode(execution,processDefinition,vars);
        if(!(nextFlowNode instanceof UserTask)){
            return RestResult.result(RespCode.CODE_0.getValue(),"下个节点为系统选择用户",map);
        }
        // TODO: 2022/2/21 下个用户节点
        UserTask userTask = (UserTask) nextFlowNode;
        String assignee = userTask.getAssignee();
        List<String> userIdList = new ArrayList<>();
        List<Object> userResolve =new ArrayList<>();
        if(StringUtils.isNotEmpty(assignee)){
            if(assignee.contains("${assignments.resolve(execution,'")){
                assignee = assignee.replace("${assignments.resolve(execution,'","").replace("')}","");
                List<Map > taskCandidateDefine =(List<Map>) AppUtils.decodeJson2(assignee,List.class);
                userResolve.addAll(taskCandidateDefine);
            }else if(assignee.contains("{")){
                //变量中值
                assignee= assignee.replace("${","").replace("}","");
                String variableValue = getVariableValue(assignee, processInstanceId);
                if(StringUtils.isNotEmpty(variableValue)){
                    userIdList.add(variableValue);
                }
            }else {
                userIdList.add(assignee);
            }
        }
        List<String> candidateUsers = userTask.getCandidateUsers();
        for (String candidateUser : candidateUsers) {
            if(candidateUser.contains("${assignments.resolve(execution,'")){
                candidateUser = candidateUser.replace("${assignments.resolve(execution,'","").replace("')}","");
                List<Map > taskCandidateDefine =(List<Map>) AppUtils.decodeJson2(candidateUser,List.class);
                userResolve.addAll(taskCandidateDefine);
            }else if(candidateUser.contains("{")){
                candidateUser= candidateUser.replace("${","").replace("}","");
                String variableValue = getVariableValue(candidateUser, processInstanceId);
                if(StringUtils.isNotEmpty(variableValue)){
                    userIdList.add(variableValue);
                }
                //变量中值
            }else {
                userIdList.add(candidateUser);
            }
        }
        if(userTask.hasMultiInstanceLoopCharacteristics()){
            //此为用户 并行多实例节点 返回的是参与人不能选择.
            // ${assignments.resolve(execution,'[{"dimension":"dept","values":["6"]}]')}
            MultiInstanceLoopCharacteristics loopCharacteristics = userTask.getLoopCharacteristics();
            String inputDataItem = loopCharacteristics.getInputDataItem();
            if(inputDataItem.contains("{")){
                map.putInteger("operationUserType",0);//不可操作用户
                inputDataItem = inputDataItem.replace("${assignments.resolve(execution,'","").replace("')}","");
                List<Map> taskCandidateDefine = (List<Map>) AppUtils.decodeJson2(inputDataItem,List.class);
                userResolve.addAll(taskCandidateDefine);
            }
        }
        if(!userResolve.isEmpty()){
            userIdList.addAll(taskCandidateResolver.resolve(WorkflowContextImpl.builder().currentUserId("1").build(),userResolve));
        }
        if(!userIdList.isEmpty()){
            List<Long> collect = userIdList.stream().map(var -> Long.valueOf(var)).collect(Collectors.toList());
            RestResult<?> restResult = userServiceFeign.findUserIdList(collect);
            if(restResult.getCode()==RespCode.CODE_0.getValue()){
                map.put("userList",restResult.getData());//用户信息
            }
        }
        return RestResult.result(RespCode.CODE_0.getValue(),null,map);
    }



    /**
     * 根据当前节点获取下个节点信息
     * @param execution
     * @param processDefinition
     * @return
     */
    private FlowNode findNextFlowNode(Execution execution,ProcessDefinition processDefinition,Map<String, Object> vars){
        FlowNode userFlowNode = null;
        String activityId = execution.getActivityId();
        //根据活动节点获取当前的组件信息
        FlowNode flowNode = getFlowNode(processDefinition.getId(), activityId);
        //获取该节点之后的流向
        List<SequenceFlow> sequenceFlowList = flowNode.getOutgoingFlows();
        if(sequenceFlowList.size() > 1){
            activityId = getNextActivityId(sequenceFlowList,vars);
            return getFlowNode(processDefinition.getId(), activityId);
        }else if (sequenceFlowList.size() == 1) {
            // 只有1条出线,直接取得下个节点
            SequenceFlow sequenceFlow = sequenceFlowList.get(0);
            FlowElement flowElement = sequenceFlow.getTargetFlowElement();
            if (flowElement instanceof UserTask) {
                // 下个节点为UserTask时
                userFlowNode = (UserTask) flowElement;
                return userFlowNode;
            } else if (flowElement instanceof ExclusiveGateway) {
                // 下个节点为排它网关时
                ExclusiveGateway exclusiveGateway = (ExclusiveGateway) flowElement;
                List<SequenceFlow> outgoingFlows = exclusiveGateway.getOutgoingFlows();
                // 遍历网关的出线得到下个activityId
                activityId = getNextActivityId( outgoingFlows,vars);
                return getFlowNode(processDefinition.getId(), activityId);
            }else {
                return null;
            }
        }
        else {
            // 没有出线，则表明是结束节点
            return null;
        }
    }

    /**
     * 根据活动节点和流程定义ID获取该活动节点的组件信息
     */
    private  FlowNode getFlowNode(String processDefinitionId, String flowElementId) {
        BpmnModel bpmnModel = getProcessEngine().getRepositoryService().getBpmnModel(processDefinitionId);
        FlowElement flowElement = bpmnModel.getMainProcess().getFlowElement(flowElementId);
        return  (FlowNode) flowElement;
    }

    /**
     * 根据el表达式取得满足条件的下一个activityId
     * @param outgoingFlows
     * @param vars
     * @return
     */
    private  String getNextActivityId( List<SequenceFlow> outgoingFlows,
                                        Map<String, Object> vars) {
        String activityId = null;
        // 遍历出线
        for (SequenceFlow outgoingFlow : outgoingFlows) {
            // 取得线上的条件
            String conditionExpression = outgoingFlow.getConditionExpression();
            String variableName = "";
            // 判断网关条件里是否包含变量名
            for (String s : vars.keySet()) {
                if (conditionExpression.contains(s)) {
                    // 找到网关条件里的变量名
                    variableName = s;
                }
            }
            // 判断el表达式是否成立
            if (isCondition(variableName, conditionExpression, String.valueOf(vars.get(variableName)))) {
                // 取得目标节点
                FlowNode targetFlowElement = (FlowNode) outgoingFlow.getTargetFlowElement();
                activityId = targetFlowElement.getId();
                break;
            }
        }
        return activityId;
    }

    /**
     * 取得流程变量的值
     *
     * @param variableName      变量名
     * @param processInstanceId 流程实例Id
     * @return
     */
    private String getVariableValue(String variableName, String processInstanceId) {
        Execution execution = getProcessEngine().getRuntimeService()
                .createExecutionQuery().processInstanceId(processInstanceId).list().get(0);
        Object object = getProcessEngine().getRuntimeService().getVariable(execution.getId(), variableName);
        return object == null ? "" : object.toString();
    }

    /**
     * 根据key和value判断el表达式是否通过
     *
     * @param key   el表达式key
     * @param el    el表达式
     * @param value el表达式传入值
     * @return
     */
    private static boolean isCondition(String key, String el, String value) {
        ExpressionFactory factory = new ExpressionFactoryImpl();
        SimpleContext context = new SimpleContext();
        context.setVariable(key, factory.createValueExpression(value, String.class));
        ValueExpression e = factory.createValueExpression(context, el, boolean.class);
        return (Boolean) e.getValue(context);
    }

}
