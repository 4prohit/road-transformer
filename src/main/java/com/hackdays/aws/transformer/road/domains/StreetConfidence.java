package com.hackdays.aws.transformer.road.domains;

import lombok.Data;

import javax.persistence.*;
import java.util.Map;

@Data
@Entity
public class StreetConfidence {

    @Id
    @GeneratedValue
    private Integer confidenceId;

    @ManyToOne
    @JoinColumn(name = "street_id")
    private StreetDetail streetDetail;

    private Float score;

    @ElementCollection
    private Map<String, Float> labels;

    private String s3FilePath;

    private String uploadedBy;

    private Long createdOn;
}
