package com.lenovo.vectorscoringtest.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.elasticsearch.common.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import static com.lenovo.vectorscoringtest.constant.Constant.*;

@Document(indexName = VECTOR_128_LSH_L2, shards = 5)
@Setting(settingPath = "json/elasticknn-settings.json")
@Mapping(mappingPath = "json/elasticknn-mappings-lsh-l2.json")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ElasticKnn {
    @Id
    @Nullable
    @Field(type = FieldType.Keyword)
    private String documentId;

    private float[] embeddingVector;

    private float[] exactVector;
}
