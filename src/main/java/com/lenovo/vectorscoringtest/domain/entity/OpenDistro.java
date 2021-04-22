package com.lenovo.vectorscoringtest.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.elasticsearch.common.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import static com.lenovo.vectorscoringtest.constant.Constant.*;

@Document(indexName = OPEN_DISTRO_L2, shards = 5)
@Setting(settingPath = "json/open-distro-l2-settings.json")
@Mapping(mappingPath = "json/open-distro-mappings.json")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OpenDistro {
    @Id
    @Nullable
    @Field(type = FieldType.Keyword)
    private String documentId;

    private float[] embeddingVector;
}
