/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React, { useEffect, useMemo } from 'react';
import { useQueryClient } from '@tanstack/react-query';

import QueryHelper from 'components/common/QueryHelper';
import type { Stream } from 'stores/streams/StreamsStore';
import StreamsStore from 'stores/streams/StreamsStore';
import { StreamRulesStore } from 'stores/streams/StreamRulesStore';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import { keyFn, fetchStreams, KEY_PREFIX } from 'components/streams/hooks/useStreams';
import {
  DEFAULT_LAYOUT,
  ADDITIONAL_ATTRIBUTES, COLUMNS_ORDER,
} from 'components/streams/StreamsOverview/Constants';
import FilterValueRenderers from 'components/streams/StreamsOverview/FilterValueRenderers';
import useTableElements from 'components/streams/StreamsOverview/hooks/useTableComponents';
import PaginatedEntityTable from 'components/common/PaginatedEntityTable';

import CustomColumnRenderers from './ColumnRenderers';

const useRefetchStreamsOnStoreChange = (refetchStreams: () => void) => {
  useEffect(() => {
    StreamsStore.onChange(() => refetchStreams());
    StreamRulesStore.onChange(() => refetchStreams());

    return () => {
      StreamsStore.unregister(() => refetchStreams());
      StreamRulesStore.unregister(() => refetchStreams());
    };
  }, [refetchStreams]);
};

type Props = {
  indexSets: Array<IndexSet>
}

const StreamsOverview = ({ indexSets }: Props) => {
  const queryClient = useQueryClient();
  const { entityActions, expandedSections, bulkActions } = useTableElements({ indexSets });
  useRefetchStreamsOnStoreChange(() => queryClient.invalidateQueries(KEY_PREFIX));

  const columnRenderers = useMemo(() => CustomColumnRenderers(indexSets), [indexSets]);

  return (
    <PaginatedEntityTable<Stream> humanName="streams"
                                  columnsOrder={COLUMNS_ORDER}
                                  additionalAttributes={ADDITIONAL_ATTRIBUTES}
                                  queryHelpComponent={<QueryHelper entityName="stream" />}
                                  entityActions={entityActions}
                                  tableLayout={DEFAULT_LAYOUT}
                                  fetchEntities={fetchStreams}
                                  keyFn={keyFn}
                                  expandedSectionsRenderer={expandedSections}
                                  bulkSelection={{ actions: bulkActions }}
                                  entityAttributesAreCamelCase={false}
                                  filterValueRenderers={FilterValueRenderers}
                                  columnRenderers={columnRenderers} />
  );
};

StreamsOverview.propTypes = {
  indexSets: PropTypes.array.isRequired,
};

export default StreamsOverview;
