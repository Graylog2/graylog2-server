package org.graylog.storage.elasticsearch7.testing;

import com.fasterxml.jackson.databind.JsonNode;
import org.graylog.testing.elasticsearch.BulkIndexRequest;
import org.graylog.testing.elasticsearch.Client;

public class ElasticsearchClientES7 implements Client {
    @Override
    public void createIndex(String index) {

    }

    @Override
    public void createIndex(String index, int shards, int replicas) {

    }

    @Override
    public String createRandomIndex(String prefix) {
        return null;
    }

    @Override
    public void deleteIndices(String... indices) {

    }

    @Override
    public void closeIndex(String index) {

    }

    @Override
    public boolean indicesExists(String... indices) {
        return false;
    }

    @Override
    public void addAliasMapping(String indexName, String alias) {

    }

    @Override
    public JsonNode getMapping(String... indices) {
        return null;
    }

    @Override
    public JsonNode getTemplate(String templateName) {
        return null;
    }

    @Override
    public JsonNode getTemplates() {
        return null;
    }

    @Override
    public void putTemplate(String templateName, Object source) {

    }

    @Override
    public void deleteTemplates(String... templates) {

    }

    @Override
    public void waitForGreenStatus(String... indices) {

    }

    @Override
    public void refreshNode() {

    }

    @Override
    public void bulkIndex(BulkIndexRequest bulkIndexRequest) {

    }

    @Override
    public void cleanUp() {

    }
}
