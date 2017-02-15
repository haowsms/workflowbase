package com.migu.core.workflow.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.migu.core.workflow.util.Page;
import com.migu.core.workflow.util.PageUtil;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.NativeExecutionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.migu.core.workflow.bean.TaskInfo;
import com.migu.core.workflow.constants.Constants;

public class TaskHandlerQryService implements Constants{
	
	@Autowired
	private RepositoryService repositoryService;
	
	@Autowired
	private RuntimeService runtimeService;
	
	@Autowired
	private TaskService taskService;

	@Autowired
	private HistoryService historyService;
	/**
	 * 查询用户需要处理的任务
	 * @param processKey 流程key
	 * @param userName 用户名称
	 */
	public Map<String, Object> queryTaskByUserName(String processKey, String userName, Integer pageNo, Integer pageSize) {
		long taskSize = taskService.createTaskQuery()
				.processDefinitionKey(processKey)
				.taskCandidateUser(userName)
				.count();
		
		List<Task> lstTask = taskService.createTaskQuery()
				.processDefinitionKey(processKey)
				.taskCandidateUser(userName)
				.orderByTaskCreateTime()
				.desc()
				.listPage(((pageNo - 1) * pageSize), pageSize);
		List<TaskInfo> lstTaskInfo = new ArrayList<TaskInfo>();
		for (Task task : lstTask) {
			Map<String, Object> map = task.getProcessVariables();
			if (null == map) {
				map = new HashMap<String, Object>();
			}
			
			TaskInfo info = new TaskInfo(task.getId());
			info.setMapVar(map);
			lstTaskInfo.add(info);
		}
		
		Map<String, Object> returnMap = new HashMap<String, Object>();
		returnMap.put("totalCount", taskSize);
		returnMap.put("count", lstTaskInfo.size());
		returnMap.put("data", lstTaskInfo);
		return returnMap;
	}
	
	/**
	 * 查询任务的候选人
	 */
	public Map<String, Object> queryIdentityLink(String taskId,Integer pageNo, Integer pageSize){
		List<IdentityLink> allIdentityLinkList = taskService.getIdentityLinksForTask(taskId);
		Page<Execution> page = new Page<Execution>(pageSize);
		int[] pageParams = PageUtil.init(page,pageNo,pageSize);
		List<IdentityLink> retIdentityLinkList = allIdentityLinkList.subList(((pageNo - 1) * pageSize),((pageNo - 1) * pageSize)+pageSize);
		Map<String, Object> returnMap = new HashMap<String, Object>();
		returnMap.put("totalCount", allIdentityLinkList.size());
		returnMap.put("count", retIdentityLinkList.size());
		returnMap.put("data", retIdentityLinkList);
		return returnMap;
	}

	/**
	 * 用户参与过的还未结束的流程信息,改接口后续根据需求分解
	 *
	 * @return
	 */
	public Map<String,Object> queryTaskFlowByUserId(String userId,Integer pageNo, Integer pageSize){

		Map<String, Object> returnMap = new HashMap<String, Object>();

		Page<Execution> page = new Page<Execution>(pageSize);
		int[] pageParams = PageUtil.init(page,pageNo,pageSize);
		NativeExecutionQuery nativeExecutionQuery = runtimeService.createNativeExecutionQuery();

		String sql = "select RES.* from ACT_RU_EXECUTION RES left join ACT_HI_TASKINST ART on ART.PROC_INST_ID_ = RES.PROC_INST_ID_ "
				+ " where ART.ASSIGNEE_ = #{userId} and ACT_ID_ is not null and IS_ACTIVE_ = 'TRUE' order by START_TIME_ desc";

		nativeExecutionQuery.parameter("userId", userId);

		List<Execution>   executionList = nativeExecutionQuery.sql(sql).listPage(pageParams[0], pageParams[1]);

		// 查询流程定义对象
		Map<String, ProcessDefinition> definitionMap = new HashMap<String, ProcessDefinition>();

		Map<String, Task> taskMap = new HashMap<String, Task>();

		// 每个Execution的当前活动ID，可能为多个
		Map<String, List<String>> currentActivityMap = new HashMap<String, List<String>>();

		// 设置每个Execution对象的当前活动节点
		for (Execution execution : executionList) {
			ExecutionEntity executionEntity = (ExecutionEntity) execution;
			String processInstanceId = executionEntity.getProcessInstanceId();
			String processDefinitionId = executionEntity.getProcessDefinitionId();

			// 缓存ProcessDefinition对象到Map集合
			definitionCache(definitionMap, processDefinitionId);

			// 查询当前流程的所有处于活动状态的活动ID，如果并行的活动则会有多个
			List<String> activeActivityIds = runtimeService.getActiveActivityIds(execution.getId());
			currentActivityMap.put(execution.getId(), activeActivityIds);

			for (String activityId : activeActivityIds) {

				// 查询处于活动状态的任务
				Task task = taskService.createTaskQuery().taskDefinitionKey(activityId).executionId(execution.getId()).singleResult();

				// 调用活动
				if (task == null) {
					ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
							.superProcessInstanceId(processInstanceId).singleResult();
					task = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).singleResult();
					definitionCache(definitionMap, processInstance.getProcessDefinitionId());
				}
				taskMap.put(activityId, task);
			}
		}

		returnMap.put("taskMap", taskMap);
		returnMap.put("definitions", definitionMap);
		returnMap.put("currentActivityMap", currentActivityMap);

		page.setResult(executionList);
		page.setTotalCount(nativeExecutionQuery.sql("select count(*) from (" + sql + ")").count());
		returnMap.put("page", page);

		return returnMap;
	}

	/**
	 * 流程定义对象缓存
	 */
	private void definitionCache(Map<String, ProcessDefinition> definitionMap, String processDefinitionId) {
		if (definitionMap.get(processDefinitionId) == null) {
			ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
			processDefinitionQuery.processDefinitionId(processDefinitionId);
			ProcessDefinition processDefinition = processDefinitionQuery.singleResult();
			definitionMap.put(processDefinitionId, processDefinition);
		}
	}

	/**
	 * 根据流程实例id查询流程之前经过的活动
	 */
	public List<HistoricActivityInstance> queryBeforeFlow(String processInstanceId){

		List<HistoricActivityInstance> list =historyService
				.createHistoricActivityInstanceQuery()
				.processInstanceId(processInstanceId).orderByHistoricActivityInstanceStartTime()
				.list();

		return list;
	}


	/**
	 *  查询当前流程后续节点
	 */
	public List<PvmActivity> traceProcess(String processInstanceId) throws Exception {
		Execution execution = runtimeService.createExecutionQuery().executionId(processInstanceId).singleResult();
		Object property = PropertyUtils.getProperty(execution, "activityId");
		String activityId = "";
		if (property != null) {
			activityId = property.toString();
		}
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId)
				.singleResult();
		ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService)
				.getDeployedProcessDefinition(processInstance.getProcessDefinitionId());
		List<ActivityImpl> activitiList = processDefinition.getActivities();//获得当前任务的所有节点

		List<PvmActivity> resultList = new ArrayList<PvmActivity>();
		for (ActivityImpl activity : activitiList) {
			// 当前节点
			if (activity.getId().equals(activityId)) {
				List<PvmTransition> outTransitions = activity.getOutgoingTransitions();//获取从某个节点出来的所有线路
				for(PvmTransition tr:outTransitions){
					PvmActivity pvmActivity = tr.getDestination(); //获取线路的终点节点
					resultList.add(pvmActivity);
				}
				break;
			}
		}

		return resultList;
	}





}
