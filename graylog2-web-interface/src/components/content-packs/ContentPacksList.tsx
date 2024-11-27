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
import * as React from 'react';
import { useState } from 'react';
import styled, { css } from 'styled-components';

import {
  Col,
  Row,
} from 'components/bootstrap';
import {
  Pagination, PageSizeSelect, NoSearchResult, NoEntitiesExist,
} from 'components/common';
import TypeAheadDataFilter from 'components/common/TypeAheadDataFilter';
import ControlledTableList from 'components/common/ControlledTableList';
import ContentPackListItem from 'components/content-packs/components/ContentPackListItem';
import { DEFAULT_PAGINATION } from 'stores/PaginationTypes';

import type { ContentPackInstallation, ContentPackMetadata } from './Types';

type Props = {
  contentPacks?: Array<ContentPackInstallation>
  contentPackMetadata?: ContentPackMetadata
  onDeletePack?: (id: string) => void
  onInstall?: (id: string, contentPackRev: string, parameters: unknown) => void
};

const StyledPageSizeSelect = styled(PageSizeSelect)(({ theme }) => css`
  display: flex;
  align-items: center;
  gap: ${theme.spacings.xs};
  float: right;
`);

const ContentPacksList = ({ contentPacks = [], contentPackMetadata = {}, onDeletePack = () => {}, onInstall = () => {} }: Props) => {
  const [filteredContentPacks, setFilteredContentPacks] = useState(contentPacks);
  const [paginationOption, setPaginationOption] = useState(DEFAULT_PAGINATION);

  const formatItems = (items: Array<ContentPackInstallation>) => {
    const { perPage, page } = paginationOption;
    const begin = (perPage * (page - 1));
    const end = begin + perPage;
    const shownItems = items.slice(begin, end);

    return shownItems.map((item) => (
      <ContentPackListItem key={item.id}
                           pack={item}
                           contentPackMetadata={contentPackMetadata}
                           onDeletePack={onDeletePack}
                           onInstall={onInstall} />
    ));
  };

  const filterContentPacks = (filteredItems: Array<ContentPackInstallation>) => {
    setFilteredContentPacks(filteredItems);
  };

  const onItemsShownChange = (pageSize: number) => {
    setPaginationOption({ ...paginationOption, perPage: pageSize });
  };

  const onChangePage = (nextPage: number) => {
    setPaginationOption({ ...paginationOption, page: nextPage });
  };

  const numberPages = Math.ceil(filteredContentPacks.length / paginationOption.perPage);

  const pagination = (
    <Pagination totalPages={numberPages}
                currentPage={paginationOption.page}
                onChange={onChangePage} />
  );

  const pageSizeSelect = (
    <StyledPageSizeSelect onChange={onItemsShownChange}
                          pageSize={paginationOption.perPage}
                          pageSizes={[10, 25, 50, 100]} />
  );

  const noContentMessage = contentPacks.length <= 0
    ? <NoEntitiesExist>No content packs found. Please create or upload one</NoEntitiesExist>
    : <NoSearchResult>No matching content packs have been found</NoSearchResult>;

  const content = filteredContentPacks.length <= 0
    ? (<div className="has-bm">{noContentMessage}</div>)
    : (
      <ControlledTableList>
        <ControlledTableList.Header />
        {formatItems(filteredContentPacks)}
      </ControlledTableList>
    );

  return (
    <div>
      <Row className="has-bm">
        <Col md={5}>
          <TypeAheadDataFilter id="content-packs-filter"
                               label="Filter"
                               data={contentPacks}
                               displayKey="name"
                               onDataFiltered={filterContentPacks}
                               searchInKeys={['name', 'summary']}
                               filterSuggestions={[]} />
        </Col>
        <Col md={5}>
          {pagination}
        </Col>
        <Col md={2} className="text-right">
          {pageSizeSelect}
        </Col>
      </Row>
      {content}
      <Row className="row-sm">
        <Col md={5} />
        <Col md={5}>
          {pagination}
        </Col>
        <Col md={2} className="text-right">
          {pageSizeSelect}
        </Col>
      </Row>
    </div>
  );
};

export default ContentPacksList;
