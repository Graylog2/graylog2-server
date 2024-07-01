package org.graylog2.streams.filters;

import com.mongodb.client.MongoCollection;
import jakarta.inject.Inject;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;

import java.util.Optional;
import java.util.function.Predicate;

import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.database.utils.MongoUtils.insertedId;
import static org.graylog2.shared.utilities.StringUtils.f;
import static org.graylog2.shared.utilities.StringUtils.requireNonBlank;

public class StreamOutputFilterService {
    static final String COLLECTION = "stream_output_filters";

    private final MongoCollection<StreamOutputFilterRuleDTO> collection;
    private final MongoPaginationHelper<StreamOutputFilterRuleDTO> paginationHelper;
    private final MongoUtils<StreamOutputFilterRuleDTO> utils;

    @Inject
    public StreamOutputFilterService(MongoCollections mongoCollections) {
        this.collection = mongoCollections.collection(COLLECTION, StreamOutputFilterRuleDTO.class);
        this.paginationHelper = mongoCollections.paginationHelper(collection);
        this.utils = mongoCollections.utils(collection);
    }

    public PaginatedList<StreamOutputFilterRuleDTO> findPaginated(Bson query,
                                                                  Bson sort,
                                                                  int perPage,
                                                                  int page,
                                                                  Predicate<String> permissionSelector) {
        return paginationHelper.filter(query)
                .sort(sort)
                .perPage(perPage)
                .page(page, dto -> permissionSelector.test(dto.id()));
    }

    public Optional<StreamOutputFilterRuleDTO> findById(String id) {
        return utils.getById(id);
    }

    public StreamOutputFilterRuleDTO create(StreamOutputFilterRuleDTO dto) {
        final var result = collection.insertOne(dto);

        return utils.getById(insertedId(result))
                .orElseThrow(() -> new IllegalArgumentException(f("Couldn't insert document: %s", dto)));
    }

    public StreamOutputFilterRuleDTO update(StreamOutputFilterRuleDTO dto) {
        collection.replaceOne(idEq(requireNonBlank(dto.id(), "id can't be blank")), dto);

        return utils.getById(dto.id())
                .orElseThrow(() -> new IllegalArgumentException(f("Couldn't find updated document: %s", dto)));
    }
}
