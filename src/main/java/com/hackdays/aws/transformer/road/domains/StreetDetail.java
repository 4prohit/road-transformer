package com.hackdays.aws.transformer.road.domains;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
@Entity
public class StreetDetail {

    @Id
    @GeneratedValue
    private Integer locationId;

    private Integer roadId;

    private String cctvId;

    private String roadName;

    private String latitude;

    private String longitude;

    private String country;

    private Long createdOn;
}
