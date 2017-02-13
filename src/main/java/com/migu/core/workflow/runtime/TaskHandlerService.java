package com.migu.core.workflow.runtime;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Task;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.migu.core.workflow.bean.TaskInfo;
import com.migu.core.workflow.constants.Constants;

public class TaskHandlerService implements Constants{
	
	@Autowired
	private RepositoryService repositoryService;
	
	@Autowired
	private RuntimeService runtimeService;
	
	@Autowired
	private TaskService taskService;
	
	/**
	 * 开启一个新的流程
	 * @param processKey 流程key
	 * @param paramMap 流程自定义变量(k-v键值对)
	 * @throws Exception 流程不存在异常
	 */
	public void startProcess(String processKey, Map<String, Object> paramMap) throws Exception {
		ProcessDefinition processDefinition = this.repositoryService.createProcessDefinitionQuery()
				.processDefinitionKey(processKey)
				.latestVersion()
				.singleResult();
		if (null == processDefinition) {
			throw new Exception("流程" + processKey + "不存在");
		}
		
		paramMap = paramMap == null ? new HashMap<String, Object>() : paramMap;
		paramMap.put(PROCESS_DEFINITION_ID, processDefinition.getId());
		this.runtimeService.startProcessInstanceByKey(processKey, paramMap);
	}

	/**
	 * 任务接管, 把组任务变成个人任务， 
	 * 注：任务被接管后组任务成员就不能再查询到任务
	 * @param processId 流程id, 启动流程时初始化"re_proc_def"的值
	 * @param userName 用户名
	 * @return 任务信息
	 */
	public TaskInfo assignTask(String processId, String userName){
		Task task = this.taskService.createTaskQuery()
				.processDefinitionId(processId)
				.taskCandidateUser(userName)
				.singleResult();
		if (null == task) {
			return null;
		}
		
		this.taskService.claim(task.getId(), userName);
		return new TaskInfo(task);
	}
	
	/**
	 * 组任务归还，归还后组任务成员可以查询到组任务
	 * @param processId 流程id, 启动流程时初始化"re_proc_def"的值
	 * @param userName 用户名
	 * @param taskId 任务id
	 * @return 返回任务信息, 不为null 表示归还成功.
	 */
	public TaskInfo returnTask(String processId, String userName, String taskId){
		Task task = this.taskService.createTaskQuery()
				.processDefinitionId(processId)
				.taskAssignee(userName)
				.taskId(taskId)
				.singleResult();
		if (null == task) {
			return null;
		}
		
		this.taskService.claim(task.getId(), null);
		return new TaskInfo(task);
	}
	
	/**
	 * 审批任务
	 */
	/**
	 * 经理审批直接通过
	 */
	@Test
	public void completeTask(String processId, String userName, String taskId, Map<String, Object> begn){
		Task task = taskService.createTaskQuery()
				.processDefinitionId(processId)
				.taskAssignee(userName)
				.taskId(taskId)
				.singleResult();
		/**
		 * 通过流程变量的指定来决定走那条路线,一定要放到map中，在调用complete把map放到流程变量之中
		 */
		Map<String, Object> map = new HashMap<String,Object>();
		String outcome = "经理审批通过";
		map.put("outcome", outcome);
		taskService.complete(task.getId());
	}

}
