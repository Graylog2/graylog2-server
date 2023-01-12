package org.graylog.datanode.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

	@GetMapping("/")
	public String index() {
		return "Greetings from data node!";
	}

}
