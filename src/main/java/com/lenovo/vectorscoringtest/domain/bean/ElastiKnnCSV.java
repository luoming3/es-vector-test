package com.lenovo.vectorscoringtest.domain.bean;

import com.opencsv.bean.CsvBindByPosition;
import lombok.*;

@Data
@Builder
@ToString
public class ElastiKnnCSV {
    // index settings
    private Integer size;
    private Integer dim;
    private String model;
    private String similarity;
    private Integer L;
    private Integer k;
    private Integer w;
    private Boolean repeating;

    // query settings
    private Integer candidates;
    private Integer probes;

    // recall
    private String top10Recall;
    private String top50Recall;
    private String top100Recall;

}
