package org.graylog2.indexer.fieldtypes;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog2.streams.StreamService;

import javax.inject.Inject;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableSet.of;
import static org.graylog2.indexer.fieldtypes.FieldTypes.Type.createType;

public class MappedFieldTypesService {
    private final StreamService streamService;
    private final IndexFieldTypesService indexFieldTypesService;
    private final FieldTypeMapper fieldTypeMapper;

    private static final FieldTypes.Type UNKNOWN_TYPE = createType("unknown", of());
    private static final String PROP_COMPOUND_TYPE = "compound";

    @Inject
    public MappedFieldTypesService(StreamService streamService, IndexFieldTypesService indexFieldTypesService, FieldTypeMapper fieldTypeMapper) {
        this.streamService = streamService;
        this.indexFieldTypesService = indexFieldTypesService;
        this.fieldTypeMapper = fieldTypeMapper;
    }

    public Set<MappedFieldTypeDTO> fieldTypesByStreamIds(Collection<String> streamIds) {
        return mergeCompoundFieldTypes(
                streamService.loadByIds(streamIds)
                        .stream()
                        .filter(Objects::nonNull)
                        .map(indexSet -> indexSet.getIndexSet().getConfig().id())
                        .flatMap(indexSetId -> this.indexFieldTypesService.findForIndexSet(indexSetId).stream())
                        .map(IndexFieldTypesDTO::fields)
                        .flatMap(Collection::stream)
                        .map(this::mapPhysicalFieldType)
        );
    }

    public MappedFieldTypeDTO mapPhysicalFieldType(FieldTypeDTO fieldType) {
        final FieldTypes.Type mappedFieldType = fieldTypeMapper.mapType(fieldType.physicalType()).orElse(UNKNOWN_TYPE);
        return MappedFieldTypeDTO.create(fieldType.fieldName(), mappedFieldType);
    }

    public Set<MappedFieldTypeDTO> mergeCompoundFieldTypes(java.util.stream.Stream<MappedFieldTypeDTO> stream) {
        return stream.collect(Collectors.groupingBy(MappedFieldTypeDTO::name, Collectors.toSet()))
                .entrySet()
                .stream()
                .map(entry -> {
                    final Set<MappedFieldTypeDTO> fieldTypes = entry.getValue();
                    final String fieldName = entry.getKey();
                    if (fieldTypes.size() == 1) {
                        return fieldTypes.iterator().next();
                    }

                    final Set<String> distinctTypes = fieldTypes.stream()
                            .map(mappedFieldTypeDTO -> mappedFieldTypeDTO.type().type())
                            .sorted()
                            .collect(Collectors.toCollection(LinkedHashSet::new));
                    final String compoundFieldType = distinctTypes.size() > 1
                            ? distinctTypes.stream().collect(Collectors.joining(",", "compound(", ")"))
                            : distinctTypes.stream().findFirst().orElse("unknown");
                    final ImmutableSet<String> commonProperties = fieldTypes.stream()
                            .map(mappedFieldTypeDTO -> mappedFieldTypeDTO.type().properties())
                            .reduce((s1, s2) -> Sets.intersection(s1, s2).immutableCopy())
                            .orElse(ImmutableSet.of());

                    final ImmutableSet<String> properties = ImmutableSet.<String>builder().addAll(commonProperties).add(PROP_COMPOUND_TYPE).build();
                    return MappedFieldTypeDTO.create(fieldName, createType(compoundFieldType, properties));

                })
                .collect(Collectors.toSet());

    }
}
