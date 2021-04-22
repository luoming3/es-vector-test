package com.lenovo.vectorscoringtest.dao;

import com.lenovo.vectorscoringtest.domain.entity.FastKnn;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;


public interface FastKnnRepository extends ElasticsearchRepository<FastKnn, String> {
}
