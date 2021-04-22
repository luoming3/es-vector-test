package com.lenovo.vectorscoringtest.domain.entity;

import lombok.*;
import org.elasticsearch.common.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import static com.lenovo.vectorscoringtest.constant.Constant.*;

@Document(indexName = VECTOR_512, shards = 5)
@Mapping(mappingPath = "json/fast-knn-mappings.json")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FastKnn {
    @Id
    @Nullable
    @Field(type = FieldType.Keyword)
    private String documentId;

    private String embeddingVector;
}
