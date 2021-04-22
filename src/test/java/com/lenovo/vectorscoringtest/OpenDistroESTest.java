package com.lenovo.vectorscoringtest;

import com.alibaba.fastjson.JSONObject;
import com.lenovo.vectorscoringtest.dao.OpenDistroRepository;
import com.lenovo.vectorscoringtest.domain.entity.ElasticKnn;
import com.lenovo.vectorscoringtest.domain.entity.OpenDistro;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.*;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static com.lenovo.vectorscoringtest.constant.Constant.*;
import static java.lang.Integer.max;

@SpringBootTest
class OpenDistroESTest {
    @Autowired
    OpenDistroRepository openDistro;

    @Autowired
    RestHighLevelClient client;

    @Autowired
    RestClient restClient;

    @Autowired
    ElasticsearchRestTemplate elasticsearchRestTemplate;

    private Logger logger = LoggerFactory.getLogger(OpenDistroESTest.class);

    Random random = new Random();
    final int dim = 128;
    final int bulkNum = 1000;

    // index settings
    final String index = OPEN_DISTRO_L2;

    // create setting
    final String RequestTimeout = "60m";

    int vectorNum = 990 * 10000;
    int maxThreadNum = 20;
    int minBatchNum = 10000;  // if use fixed thread num, set minBatchNum = 1

    // recall
    int loop = 20;
    int size = 100;
    int params_k = 100;
    String spaceType = "l2";

//    @Test
    void deleteAll() {
        openDistro.deleteAll();
    }

    @Test
    void count() throws IOException {
        RefreshRequest request = new RefreshRequest(index);
        request.indicesOptions(IndicesOptions.lenientExpandOpen());
        client.indices().refresh(request, RequestOptions.DEFAULT);

        System.out.println(openDistro.count());
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
            ElasticKnn vector = ElasticKnn.builder().build();
            vector.setEmbeddingVector(generateVectorRandom(this.dim));
            JSONObject json = JSONObject.parseObject(JSONObject.toJSONString(vector));
            request.add(new IndexRequest(index).id(uuid).source(json).opType("create"));

            bulkNum += 1;
            if (bulkNum == this.bulkNum) {
                count += bulkNum;
                request.timeout(RequestTimeout);
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
            request.timeout(RequestTimeout);
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
            } finally {
                latch.countDown();
            }
        }
    }

    void bulkRequest(int batchNum) throws IOException {
        BulkRequest request = new BulkRequest();
        int bulkNum = 0;

        for (int i = 0; i < batchNum; i++) {
            String uuid = UUID.randomUUID().toString();
            ElasticKnn vector = ElasticKnn.builder().build();
            vector.setEmbeddingVector(generateVectorRandom(this.dim));
            JSONObject json = JSONObject.parseObject(JSONObject.toJSONString(vector));
            request.add(new IndexRequest(index).id(uuid).source(json).opType("create"));

            bulkNum += 1;
            if (bulkNum == this.bulkNum) {
                request.timeout(RequestTimeout);
                client.bulk(request, RequestOptions.DEFAULT);
                request = new BulkRequest();
                bulkNum = 0;
            }
        }

        if (bulkNum > 0) {
            request.timeout(RequestTimeout);
            client.bulk(request, RequestOptions.DEFAULT);
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
    void calculateRecall() throws IOException {
        double recallCount = 0;
        for (int i = 0; i < loop; i++) {
            recallCount += getRecall();
        }

        System.out.println("average recall: " + recallCount / loop);
    }

    double getRecall() {
        float[] vector = generateVectorRandom(dim);

        String approximateSearchString = "{\n" +
                "    \"knn\": {\n" +
                "        \"embeddingVector\": {\n" +
                "            \"vector\": " + Arrays.toString(vector) + ",\n" +
                "            \"k\": " + params_k + "\n" +
                "        }\n" +
                "    }\n" +
                "}";

        Query approximateSearchQuery = new StringQuery(approximateSearchString).setPageable(PageRequest.of(0, size));
        SearchHits<OpenDistro> approximateSearchHits = elasticsearchRestTemplate.search(approximateSearchQuery, OpenDistro.class, IndexCoordinates.of(index));
        List<String> approximateResults = new ArrayList<>();
        for (SearchHit<OpenDistro> searchHit: approximateSearchHits.getSearchHits()) {
            approximateResults.add(searchHit.getId());
        }

        Map<String, Object> params = new HashMap<>();
        params.put("field","embeddingVector");
        params.put("vector", vector);
        params.put("space_type", spaceType);

        Script script = new Script(ScriptType.INLINE, "knn", "knn_score", params);
        Query exactSearchQuery = new StringQuery(QueryBuilders.scriptScoreQuery(QueryBuilders.matchAllQuery(), script).toString()).setPageable(PageRequest.of(0, size));
        SearchHits<OpenDistro> exactSearchHits = elasticsearchRestTemplate.search(exactSearchQuery, OpenDistro.class, IndexCoordinates.of(index));
        List<String> exactResults = new ArrayList<>();
        for (SearchHit<OpenDistro> searchHit: exactSearchHits.getSearchHits()) {
            exactResults.add(searchHit.getId());
        }

        int count = 0;
        for (String result: approximateResults) {
            if (exactResults.contains(result)) {
                count++;
            }
        }

        return count / (double) size;
    }

}
