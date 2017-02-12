package com.migu.core.workflow.runtime;

import java.util.Map;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;

public class TaskHandlerService {
	
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
	 */
	public void startProcess(String processKey, Map<String, Object> paramMap) {
		this.runtimeService.startProcessInstanceByKey(processKey, paramMap);
	}
	
	/**
	 * 根据流程定义的ID来查询流程定义对象
	 */
	public void queryProcessDefinitionById() {
		String processDefinitionId = "activit_leave:1:4";
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()//创建流程定义查询对象
						.processDefinitionId(processDefinitionId)//根据id来查询
						.singleResult();//得到唯一结果
		System.out.println("流程定义的Id："+processDefinition.getId());
		System.out.println("流程定义的key："+processDefinition.getKey());
		System.out.println("流程定义的名字："+processDefinition.getName());
		System.out.println("流程定义的资源文件名字："+processDefinition.getResourceName());
	}
}
