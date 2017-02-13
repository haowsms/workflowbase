package com.migu.core.workflow.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.junit.Test;
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
	
	/**
	 * 查询用户需要处理的任务
	 * @param processKey 流程key
	 * @param userName 用户名称
	 */
	public Map<String, Object> queryTaskByUserName(String processKey, String userName, int startIndex, int pageSize) {
		long taskSize = taskService.createTaskQuery()
				.processDefinitionKey(processKey)
				.taskCandidateUser(userName)
				.count();
		
		List<Task> lstTask = taskService.createTaskQuery()
				.processDefinitionKey(processKey)
				.taskCandidateUser(userName)
				.orderByTaskCreateTime()
				.desc()
				.listPage(startIndex, pageSize);
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
	public void getIdentityLink(){
		String taskId = "9804";
		List<IdentityLink> IdentityLinkList = taskService.getIdentityLinksForTask(taskId);
		for(IdentityLink identityLink : IdentityLinkList){
			System.out.println("候选人："+ identityLink.getUserId());
			System.out.println("流程实例ID："+ identityLink.getProcessInstanceId());
		}
	}
	
	// 查询未结束的任务的来源链
	
	// 查询运行任务的出口列表
	
}
