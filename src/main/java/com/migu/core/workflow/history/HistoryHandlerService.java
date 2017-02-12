package com.migu.core.workflow.history;

import org.activiti.engine.HistoryService;
import org.springframework.beans.factory.annotation.Autowired;

public class HistoryHandlerService {
	
	@Autowired
	private HistoryService historyService;
}
