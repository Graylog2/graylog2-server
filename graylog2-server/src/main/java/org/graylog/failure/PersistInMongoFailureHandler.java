package org.graylog.failure;

import org.graylog2.indexer.IndexFailureService;

import javax.inject.Inject;

public class PersistInMongoFailureHandler implements FailureHandler {

    private final IndexFailureService indexFailureService;

    @Inject
    public PersistInMongoFailureHandler(IndexFailureService indexFailureService) {
        this.indexFailureService = indexFailureService;
    }


    @Override
    public void handle(Failure failure) {
        indexFailureService.saveWithoutValidation(((IndexingFailure) failure).getInternalFailure());
    }

    @Override
    public boolean supports(Failure failure) {
        return failure instanceof IndexingFailure;
    }
}
