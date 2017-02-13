package com.migu.core.workflow.bean;

import java.util.Map;

import org.activiti.engine.task.Task;

/**
 * 任务对象。
 * @author haow
 *
 */
public class TaskInfo {
	
	// 封装工作流中的任务id
	private String id;
	
	// 封装工作流中的任务变量
	private Map<String, Object> mapVar;
	
	public TaskInfo(Task task){
		this.id = task.getId();
		this.mapVar = task.getProcessVariables();
	}

	public TaskInfo(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Map<String, Object> getMapVar() {
		return mapVar;
	}

	public void setMapVar(Map<String, Object> mapVar) {
		this.mapVar = mapVar;
	}
}
