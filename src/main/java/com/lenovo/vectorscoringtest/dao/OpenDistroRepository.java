package com.lenovo.vectorscoringtest.dao;

import com.lenovo.vectorscoringtest.domain.entity.OpenDistro;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;


public interface OpenDistroRepository extends ElasticsearchRepository<OpenDistro, String> {
}
