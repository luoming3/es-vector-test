package com.lenovo.vectorscoringtest.dao;

import com.lenovo.vectorscoringtest.domain.entity.ElasticKnn;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;


public interface ElasticKnnRepository extends ElasticsearchRepository<ElasticKnn, String> {

}
