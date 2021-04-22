package com.lenovo.vectorscoringtest;

import com.alibaba.fastjson.JSONObject;
import com.lenovo.vectorscoringtest.dao.FastKnnRepository;
import com.lenovo.vectorscoringtest.domain.entity.FastKnn;
import com.lenovo.vectorscoringtest.util.Base64Util;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static com.lenovo.vectorscoringtest.constant.Constant.VECTOR_512;
import static java.lang.Integer.max;

@SpringBootTest
class FastKnnTest {
    @Autowired
    FastKnnRepository vectorRepository;

    @Autowired
    RestHighLevelClient client;

    @Autowired
    ElasticsearchRestTemplate elasticsearchRestTemplate;

    private Logger logger = LoggerFactory.getLogger(FastKnnTest.class);

    Random random = new Random();
    final int dim = 512;
    final int bulkNum = 10000;

    int vectorNum = 2;
    int maxThreadNum = 2;
    int minBatchNum = 1;  // if use fixed thread num, set minBatchNum = 1

//    @Test
    void deleteAll() {
        vectorRepository.deleteAll();
    }

//    @Test
    void count() throws IOException {
        RefreshRequest request = new RefreshRequest(VECTOR_512);
        request.indicesOptions(IndicesOptions.lenientExpandOpen());
        client.indices().refresh(request, RequestOptions.DEFAULT);

        System.out.println(vectorRepository.count());
    }

    @Test
    void createSyn() throws InterruptedException {
        multipleTreadsCreateVector(this.vectorNum, this.maxThreadNum, this.minBatchNum);
    }

//    @Test
    void createAsync() {
        int count = 0;
        BulkRequest request = new BulkRequest();
        int bulkNum = 0;

        for (int i = 0; i < vectorNum; i++) {
            String uuid = UUID.randomUUID().toString();
            FastKnn vector = FastKnn.builder().build();
            vector.setDocumentId(uuid);
            vector.setEmbeddingVector(Base64Util.convertArrayToBase64(generateVectorRandom(this.dim)));
            JSONObject json = JSONObject.parseObject(JSONObject.toJSONString(vector));
            request.add(new IndexRequest(VECTOR_512).id(uuid).source(json).opType("create"));

            bulkNum += 1;
            if (bulkNum == this.bulkNum) {
                count += bulkNum;
                request.timeout("5m");
                client.bulkAsync(
                        request,
                        RequestOptions.DEFAULT,
                        new ActionListener<BulkResponse>() {
                            @Override
                            public void onResponse(BulkResponse bulkResponse) {
                            }
                            @Override
                            public void onFailure(Exception e) {
                                e.printStackTrace();
                            }
                        }
                );
                request = new BulkRequest();
                bulkNum = 0;
            }
        }

        if (bulkNum > 0) {
            count += bulkNum;
            request.timeout("5m");
            client.bulkAsync(
                    request,
                    RequestOptions.DEFAULT,
                    new ActionListener<BulkResponse>() {
                        @Override
                        public void onResponse(BulkResponse bulkResponse) {
                        }
                        @Override
                        public void onFailure(Exception e) {
                            e.printStackTrace();
                        }
                    }
            );
        }

        System.out.println(count);
    }

    class CreateWorker implements Runnable {
        CountDownLatch latch;
        int batchNum;

        public CreateWorker(CountDownLatch latch, int batchNum) {
            this.latch = latch;
            this.batchNum = batchNum;
        }

        @Override
        public synchronized void run() {
            logger.info("thread is running...");

            try {
                bulkRequest(batchNum);
            } catch (Exception e) {
                e.printStackTrace();
            }
            latch.countDown();
        }
    }

    void bulkRequest(int batchNum) throws IOException {
        BulkRequest request = new BulkRequest();
        int bulkNum = 0;

        for (int i = 0; i < batchNum; i++) {
            String uuid = UUID.randomUUID().toString();
            FastKnn vector = FastKnn.builder().build();
            vector.setDocumentId(uuid);
//            vector.setEmbeddingVector(Base64Util.convertArrayToBase64(generateVectorRandom(this.dim)));
            JSONObject json = JSONObject.parseObject(JSONObject.toJSONString(vector));
            request.add(new IndexRequest(VECTOR_512).id(uuid).source(json).opType("create"));

            bulkNum += 1;
            if (bulkNum == this.bulkNum) {
                request.timeout("5m");
//                client.bulk(request, RequestOptions.DEFAULT);
                request = new BulkRequest();
                bulkNum = 0;
            }
        }

        if (bulkNum > 0) {
            request.timeout("5m");
//            client.bulk(request, RequestOptions.DEFAULT);
        }

        System.out.println("done: " + batchNum);
    }

    void multipleTreadsCreateVector(int vectorNum, int maxThreadNum, int minBatchNum) throws InterruptedException {
        int threadNum = max(1, vectorNum / max(vectorNum / maxThreadNum, minBatchNum));
        int eachBatch = vectorNum / threadNum;

        CountDownLatch latch = new CountDownLatch(threadNum);
        for (int i = 0; i < threadNum; i++) {
            if (i == threadNum - 1) { // last thread
                eachBatch = vectorNum - eachBatch * i;
            }

            if (eachBatch <= 0) {
                break;
            }

            Thread thread = new Thread(new CreateWorker(latch, eachBatch));
            thread.start();
        }

        latch.await();
    }

    @Test
    void functionScoreQuery() {
        QueryBuilder existQueryBuilder = QueryBuilders.boolQuery().filter(QueryBuilders.existsQuery("embeddingVector"));
        FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(existQueryBuilder);
        functionScoreQueryBuilder.boostMode(CombineFunction.REPLACE);

        Query query = new StringQuery("").setPageable(PageRequest.of(0, 10));
    }

    @Test
    void getTestVector() {
        System.out.println(Arrays.toString(generateVectorRandom(dim)));
    }

    public float[] generateVectorRandom(int dim) {
        float[] vector = new float[dim];
        for (int i = 0; i < dim; i++) {
            vector[i] = random.nextFloat();
        }
        return vector;
    }

    @Test
    void test3() {
        System.out.println(991 % 9);
    }

}
