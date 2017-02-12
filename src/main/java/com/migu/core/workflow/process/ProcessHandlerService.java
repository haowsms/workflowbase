package com.migu.core.workflow.process;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.DeploymentBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProcessHandlerService {

	@Autowired
	private RepositoryService repositoryService;
	
	/**
	 * 部署一个新的流程
	 * @param name 流程名称
	 * @param category 流程类别
	 * @param bpmnPath 流程图classpath下的路径
	 * @param bpmnPng 流程图片classpath下的路径
	 */
	public void startProcess(String name, String category, String bpmnPath, String bpmnPng){
		//创建部署对象
		DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
		//加载流程的配置文件和图片
		deploymentBuilder.addClasspathResource("diagrams/activit_test.bpmn")
						 .name("请假流程")
						 .category("请假")
						 .addClasspathResource("diagrams/activit_test.png");
		//部署流程
		deploymentBuilder.deploy();
	}
}
