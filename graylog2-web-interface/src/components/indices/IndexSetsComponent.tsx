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
import React, { useEffect, useState, useCallback, useRef } from 'react';

import type { PaginationQueryParameterResult } from 'hooks/usePaginationQueryParameter';
import ButtonToolbar from 'components/bootstrap/ButtonToolbar';
import { Link, LinkContainer } from 'components/common/router';
import { Button, Col, DropdownButton, Input, Label, MenuItem, Row } from 'components/bootstrap';
import { EntityList, EntityListItem, PaginatedList, SearchForm, Spinner } from 'components/common';
import Routes from 'routing/Routes';
import StringUtils from 'util/StringUtils';
import NumberUtils from 'util/NumberUtils';
import { useStore } from 'stores/connect';
import { IndexSetDeletionForm, IndexSetDetails } from 'components/indices';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import type { IndexSetsStoreState, IndexSet, IndexSetStats } from 'stores/indices/IndexSetsStore';
import { IndexSetsActions, IndexSetsStore } from 'stores/indices/IndexSetsStore';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

const IndexSetsComponent = () => {
  const DEFAULT_PAGE_NUMBER = 1;
  const DEFAULT_PAGE_SIZE = 10;
  const SEARCH_MIN_TERM_LENGTH = 3;
  const { indexSetsCount, indexSets, indexSetStats, globalIndexSetStats } = useStore<IndexSetsStoreState>(IndexSetsStore);
  const { page, resetPage } : PaginationQueryParameterResult = usePaginationQueryParameter();
  const sendTelemetry = useSendTelemetry();

  const [currentPageNumber, setCurrentPageNumber] = useState<number>(DEFAULT_PAGE_NUMBER);
  const [currentPageSize, setCurrentPageSize] = useState<number>(DEFAULT_PAGE_SIZE);
  const [statsEnabled, setStatsEnabled] = useState<boolean>(false);
  const [searchTerm, setSearchTerm] = useState<string>(undefined);

  const formsRef = useRef<{[key: string]: { open:() => void }}>();

  const loadData = useCallback((pageNumber: number, limit: number) => {
    setCurrentPageNumber(pageNumber);
    setCurrentPageSize(limit);

    if (searchTerm) {
      IndexSetsActions.searchPaginated(searchTerm, (pageNumber - 1) * limit, limit, statsEnabled);
    } else {
      IndexSetsActions.listPaginated((pageNumber - 1) * limit, limit, statsEnabled);
    }
  }, [statsEnabled, searchTerm]);

  useEffect(() => {
    loadData(page, DEFAULT_PAGE_SIZE);
  }, [loadData, page]);

  useEffect(() => {
    if (statsEnabled) {
      IndexSetsActions.stats();
    }
  }, [statsEnabled]);

  const onChangePaginatedList = (newPage: number, newSize: number) => {
    loadData(newPage, newSize);
  };

  const onSearch = (query) => {
    if (query && query.length >= SEARCH_MIN_TERM_LENGTH) {
      setSearchTerm(query);
    } else if (!query || query.length === 0) {
      setSearchTerm(query);
    }
  };

  const onSearchReset = () => { setSearchTerm(undefined); };

  const onToggleStats = () => { setStatsEnabled(!statsEnabled); };

  const onSetDefault = (indexSet: IndexSet) => {
    return () => {
      sendTelemetry('click', {
        appSection: 'index_sets',
        eventElement: 'set-default-index-set',
      });

      IndexSetsActions.setDefault(indexSet).then(() => loadData(currentPageNumber, currentPageSize));
    };
  };

  const onDelete = (indexSet: IndexSet) => {
    return () => {
      formsRef.current[`index-set-deletion-form-${indexSet.id}`].open();
    };
  };

  const deleteIndexSet = (indexSet: IndexSet, deleteIndices: boolean) => {
    resetPage();

    sendTelemetry('submit_form', {
      appSection: 'index_sets',
      eventElement: 'delete-index-set',
    });

    IndexSetsActions.delete(indexSet, deleteIndices).then(() => {
      loadData(DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE);
    });
  };

  const formatStatsString = (stats: IndexSetStats) => {
    if (!statsEnabled || !stats) {
      return 'N/A';
    }

    const indices = `${NumberUtils.formatNumber(stats.indices)} ${StringUtils.pluralize(stats.indices, 'index', 'indices')}`;
    const documents = `${NumberUtils.formatNumber(stats.documents)} ${StringUtils.pluralize(stats.documents, 'document', 'documents')}`;
    const size = NumberUtils.formatBytes(stats.size);

    return `${indices}, ${documents}, ${size}`;
  };

  const formatIndexSet = (indexSet: IndexSet) => {
    const actions = (
      <ButtonToolbar>
        <LinkContainer to={Routes.SYSTEM.INDEX_SETS.CONFIGURATION(indexSet.id)}>
          <Button>Edit</Button>
        </LinkContainer>
        <DropdownButton title="More Actions" id={`index-set-dropdown-${indexSet.id}`} pullRight>
          <MenuItem onSelect={onSetDefault(indexSet)}
                    disabled={!indexSet.can_be_default || indexSet.default}>Set as default
          </MenuItem>
          <MenuItem divider />
          <MenuItem onSelect={onDelete(indexSet)}>Delete</MenuItem>
        </DropdownButton>
      </ButtonToolbar>
    );

    const content = (
      <Col md={12}>
        <IndexSetDetails indexSet={indexSet} />

        <IndexSetDeletionForm ref={
          (elem) => { formsRef.current = { ...formsRef.current, [`index-set-deletion-form-${indexSet.id}`]: elem }; }
}
                              indexSet={indexSet}
                              onDelete={deleteIndexSet} />
      </Col>
    );

    const indexSetTitle = (
      <Link to={Routes.SYSTEM.INDEX_SETS.SHOW(indexSet.id)}>
        {indexSet.title}
      </Link>
    );

    const isDefault = indexSet.default ? <Label key={`index-set-${indexSet.id}-default-label`} bsStyle="primary">default</Label> : '';
    const isReadOnly = !indexSet.writable ? <Label key={`index-set-${indexSet.id}-readOnly-label`} bsStyle="info">read only</Label> : '';
    let { description } = indexSet;

    if (indexSet.default) {
      description += `${description.endsWith('.') ? '' : '.'} Graylog will use this index set by default.`;
    }

    let statsString;
    const stats = indexSetStats[indexSet.id];

    if (stats) {
      statsString = formatStatsString(stats);
    }

    return (
      <EntityListItem key={`index-set-${indexSet.id}`}
                      title={indexSetTitle}
                      titleSuffix={<span>{statsString} {isDefault} {isReadOnly}</span>}
                      description={description}
                      actions={actions}
                      contentRow={content} />
    );
  };

  const isLoading = !indexSets;

  if (isLoading) {
    return <Spinner />;
  }

  return (
    <>
      <Row>
        <Col md={1}>
          <Input id="enable-stats"
                 type="checkbox"
                 checked={statsEnabled}
                 onClick={onToggleStats}
                 label="Calculate stats" />

        </Col>
        <Col md={3}>
          <SearchForm onSearch={onSearch}
                      label="Search Index Sets"
                      queryWidth={300}
                      wrapperClass="has-bm"
                      onReset={onSearchReset}
                      placeholder="Enter query to search" />

        </Col>
      </Row>
      <Row>
        <Col md={1}>
          <h4><strong>Total:</strong> {formatStatsString(globalIndexSetStats)}</h4>

        </Col>

      </Row>

      <Row>
        <Col md={12}>

          <hr style={{ marginBottom: 0 }} />

          <PaginatedList pageSize={DEFAULT_PAGE_SIZE}
                         totalItems={indexSetsCount}
                         onChange={onChangePaginatedList}
                         showPageSizeSelect={false}>
            <EntityList bsNoItemsStyle="info"
                        noItemsText="There are no index sets to display"
                        items={indexSets.map((indexSet) => formatIndexSet(indexSet))} />
          </PaginatedList>
        </Col>
      </Row>
    </>
  );
};

export default IndexSetsComponent;
