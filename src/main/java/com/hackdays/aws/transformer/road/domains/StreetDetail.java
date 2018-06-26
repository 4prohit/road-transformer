package com.hackdays.aws.transformer.road.domains;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
public class StreetDetail {

    @Id
    private String streetId;

    @JsonProperty("route")
    private String streetName;

    @JsonProperty("neighborhood")
    private String neighborhood;

    @JsonProperty("locality")
    private String locality;

    @JsonProperty("country")
    private String country;

    private Long createdOn;
}
