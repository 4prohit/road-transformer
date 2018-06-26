package com.hackdays.aws.transformer.road.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ImageDetailDTO {

    private String comments;

//    @NotNull
    private String latitude;

//    @NotNull
    private String longitude;

}
