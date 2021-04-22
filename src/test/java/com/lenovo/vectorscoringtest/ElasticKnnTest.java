package com.lenovo.vectorscoringtest;

import com.alibaba.fastjson.JSONObject;
import com.lenovo.vectorscoringtest.dao.ElasticKnnRepository;
import com.lenovo.vectorscoringtest.domain.bean.ElastiKnnCSV;
import com.lenovo.vectorscoringtest.domain.entity.ElasticKnn;
import com.lenovo.vectorscoringtest.domain.entity.OpenDistro;
import com.lenovo.vectorscoringtest.util.CSVUtil;
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import static com.lenovo.vectorscoringtest.constant.Constant.*;
import static java.lang.Integer.max;

@SpringBootTest
class ElasticKnnTest {
    @Autowired
    ElasticKnnRepository elasticKnnRepository;

    @Autowired
    RestHighLevelClient client;

    @Autowired
    ElasticsearchRestTemplate elasticsearchRestTemplate;

    private Logger logger = LoggerFactory.getLogger(ElasticKnnTest.class);

    // index settings
    final String index = VECTOR_128_LSH_L2;
    Random random = new Random();
    final int dim = 128;

    // create settings
    final String RequestTimeout = "60m";

    // threads settings
    int vectorNum = 1000 * 10000;
    int maxThreadNum = 20;
    int minBatchNum = 10000;  // if use fixed thread num, set minBatchNum = 1
    final int bulkNum = 1000; // es bulk request number

    // recall settings
    int loop = 20;

    // query settings
    String embeddingField = "embeddingVector";
    String exactField = "exactVector";

    String model = "lsh";
    String similarity = "l2";

    // index settings
    Integer L = 100;
    Integer k = 4;
    Integer w = 2;
    Boolean repeating = null;

    //    @Test
    void deleteAll() {
        elasticKnnRepository.deleteAll();
    }

    @Test
    void count() throws IOException {
        RefreshRequest request = new RefreshRequest(index);
        request.indicesOptions(IndicesOptions.lenientExpandOpen());
        client.indices().refresh(request, RequestOptions.DEFAULT);

        System.out.println(elasticKnnRepository.count());
    }

    @Test
    void createSyn() throws InterruptedException {
        multipleTreadsCreateVector(this.vectorNum, this.maxThreadNum, this.minBatchNum);
    }

    @Test
    void test11() {
        System.out.println(Arrays.toString(Arrays.stream(IntStream.range(0, 20).toArray()).boxed().toArray(Integer[]::new)));
    }

    @Test
    void autoTest() {
        Integer[] candidatesArray = {50, 112, 221, 442, 883, 1666, 3332, 6664};
        Integer[] probesArray = Arrays.stream(IntStream.range(0, 21).toArray()).boxed().toArray(Integer[]::new);
        int[] topKArray = {10, 50, 100};

        List<ElastiKnnCSV> resultList = new ArrayList<>();

        for (Integer candidates : candidatesArray) {
            for (Integer probes : probesArray) {
                ElastiKnnCSV.ElastiKnnCSVBuilder elastiKnnCSVBuilder = ElastiKnnCSV.builder()
                        .dim(dim)
                        .model(model)
                        .size(vectorNum)
                        .similarity(similarity)
                        .L(L)
                        .k(k)
                        .w(w)
                        .repeating(repeating)
                        .candidates(candidates)
                        .probes(probes);

                for (int topK: topKArray) {
                    String recallString = String .format("%.3f", calculateRecall(candidates, probes, topK));
                    if (topK == 10) {
                        elastiKnnCSVBuilder.top10Recall(recallString);
                    } else if (topK == 50) {
                        elastiKnnCSVBuilder.top50Recall(recallString);
                    } else if (topK == 100) {
                        elastiKnnCSVBuilder.top100Recall(recallString);
                    }
                }
                System.out.println(elastiKnnCSVBuilder.toString());
                resultList.add(elastiKnnCSVBuilder.build());
            }
        }

        String filePath = "/home/luoming/dev/vector-scoring-test/vector-scoring-test/test-file";
        String fileName = model + "_" + similarity + "_" + vectorNum + "_" + new SimpleDateFormat(DATETIME_FORMAT).format(new Date()) + ".csv";
        File file = new File(filePath, fileName);
        CSVUtil.writeCSV(resultList, file.getPath());
    }

    double calculateRecall(Integer candidates, Integer probes, int topK) {
        double recallCount = 0;
        for (int i = 0; i < loop; i++) {
            recallCount += getRecall(candidates, probes, topK);
        }

        return recallCount / loop;
    }

    double getRecall(Integer candidates, Integer probes, int topK) {
        float[] vector = generateVectorRandom(dim);

        JSONObject approximateSearchObject = new JSONObject();
        JSONObject queryObject = new JSONObject();
        JSONObject vecObject = new JSONObject();

        // values
        vecObject.put("values", vector);

        // queryObject
        queryObject.put("field", embeddingField);
        queryObject.put("vec", vecObject);
        queryObject.put("model", model);
        queryObject.put("similarity", similarity);

        if (probes != null) {
            queryObject.put("probes", probes);
        }

        if (candidates != null) {
            queryObject.put("candidates", candidates);
        }

        approximateSearchObject.put("elastiknn_nearest_neighbors", queryObject);

        Query approximateSearchQuery = new StringQuery(approximateSearchObject.toJSONString()).setPageable(PageRequest.of(0, topK));
        SearchHits<ElasticKnn> approximateSearchHits = elasticsearchRestTemplate.search(approximateSearchQuery, ElasticKnn.class, IndexCoordinates.of(index));
        List<String> approximateResults = new ArrayList<>();
        for (SearchHit<ElasticKnn> searchHit : approximateSearchHits.getSearchHits()) {
            approximateResults.add(searchHit.getId());
        }

        JSONObject exactSearchObject = new JSONObject();
        queryObject = new JSONObject();
        vecObject = new JSONObject();

        // values
        vecObject.put("values", vector);

        // queryObject
        queryObject.put("field", exactField);
        queryObject.put("vec", vecObject);
        queryObject.put("model", "exact");
        queryObject.put("similarity", similarity);

        exactSearchObject.put("elastiknn_nearest_neighbors", queryObject);

        Query exactSearchQuery = new StringQuery(exactSearchObject.toJSONString()).setPageable(PageRequest.of(0, topK));
        SearchHits<ElasticKnn> exactSearchHits = elasticsearchRestTemplate.search(exactSearchQuery, ElasticKnn.class, IndexCoordinates.of(index));
        List<String> exactResults = new ArrayList<>();
        for (SearchHit<ElasticKnn> searchHit : exactSearchHits.getSearchHits()) {
            exactResults.add(searchHit.getId());
        }

        int count = 0;
        for (String result : approximateResults) {
            if (exactResults.contains(result)) {
                count++;
            }
        }

        return count / (double) topK;
    }

    //    @Test
    void createAsync() {
        int count = 0;
        BulkRequest request = new BulkRequest();
        int bulkNum = 0;

        for (int i = 0; i < vectorNum; i++) {
            String uuid = UUID.randomUUID().toString();
            ElasticKnn vector = ElasticKnn.builder().build();
            float[] randomVector = generateVectorRandom(this.dim);
            vector.setEmbeddingVector(randomVector);
            vector.setExactVector(randomVector);

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
            float[] randomVector = generateVectorRandom(this.dim);
            vector.setEmbeddingVector(randomVector);
            vector.setExactVector(randomVector);

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
    void test3() {
    }

}
