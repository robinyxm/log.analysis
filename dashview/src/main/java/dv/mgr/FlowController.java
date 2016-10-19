package dv.mgr;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import dv.UserNotFoundException;
import dv.entity.AccountEntity;
import dv.entity.AccountRepository;
import dv.entity.FlowEntity;
import dv.entity.FlowRepository;
import etl.flow.Flow;
import etl.util.JsonUtil;

@RestController
@RequestMapping("/{userId}/flow")
public class FlowController {
	
	@Autowired
	FlowController(FlowRepository flowRepository, AccountRepository accountRepository) {
		this.flowRepository = flowRepository;
		this.accountRepository = accountRepository;
	}
	
	private final FlowRepository flowRepository;
	private final AccountRepository accountRepository;
	
	@RequestMapping(method = RequestMethod.POST)
	ResponseEntity<?> add(@PathVariable String userId, @RequestBody Flow input) {
		this.validateUser(userId);
		FlowEntity fe = flowRepository.save(new FlowEntity(input.getName(),
				userId, JsonUtil.toJsonString(input)));

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setLocation(ServletUriComponentsBuilder
				.fromCurrentRequest().path("/{id}")
				.buildAndExpand(fe.getName()).toUri());
		return new ResponseEntity<>(null, httpHeaders, HttpStatus.CREATED);
	}
	
	@RequestMapping(value = "/{flowId}", method = RequestMethod.GET)
	FlowEntity getFlow(@PathVariable String userId, @PathVariable String flowId) {
		this.validateUser(userId);
		return this.flowRepository.findOne(flowId);
	}
	
	private void validateUser(String userId) {
		AccountEntity ae = this.accountRepository.findOne(userId);
		if (ae==null){
			throw new UserNotFoundException(userId);
		}
	}
	
}
