package com.hackdays.aws.transformer.road.domains;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class Subscription {

	@Id
    @GeneratedValue
    private Integer id;
	
	private String email;
	
	private Integer locationId;
}
