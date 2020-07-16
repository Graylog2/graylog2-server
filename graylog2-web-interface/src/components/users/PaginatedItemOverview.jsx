// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';
import styled from 'styled-components';
import * as Immutable from 'immutable';

import { PaginatedList, SearchForm } from 'components/common';

import PaginatedItem from './PaginatedItem';

const Container = styled.div`
  margin-top: 10px;
`;

export type PaginationInfo = {
  total: number,
  count: number,
  page: number,
  perPage: number,
  query: string,
};

export interface DescriptiveItem {
  get id(): string,
  get name(): string,
  get description(): string,
}

type ListOfDescriptiveItems = Immutable.List<DescriptiveItem>;

export type PaginatedListType = {
  pagination: PaginationInfo,
  list: ListOfDescriptiveItems,
};

type Props = {
  onLoad: (PaginationInfo) => Promise<PaginatedListType>,
};

const PaginatedItemOverview = ({ onLoad }: Props) => {
  const [items, setItems] = useState();
  const [paginationInfo, setPaginationInfo] = useState({
    count: 0,
    total: 0,
    page: 1,
    perPage: 5,
    query: '',
  });

  const _setResponse = ({ list, pagination }: PaginatedListType) => {
    setPaginationInfo(pagination);
    setItems(list);
  };

  useEffect(() => {
    onLoad(paginationInfo).then(_setResponse);
  }, []);

  const _onPageChange = (query) => (page, perPage) => {
    const pageInfo = {
      ...paginationInfo,
      query,
      page,
      perPage,
    };
    onLoad(pageInfo).then(_setResponse);
  };

  const _onSearch = (query) => {
    const pageInfo = {
      ...paginationInfo,
      page: 1,
      query,
    };
    onLoad(pageInfo).then(_setResponse);
  };

  return (
    <PaginatedList onChange={_onPageChange(paginationInfo.query)}
                   pageSize={paginationInfo.perPage}
                   totalItems={paginationInfo.total}
                   pageSizes={[5, 10, 30]}
                   activePage={paginationInfo.page}>
      <SearchForm onSearch={_onSearch} />
      <Container>
        {items && items.toArray().map((item) => <PaginatedItem key={item.id} item={item} />) }
      </Container>
    </PaginatedList>
  );
};

export default PaginatedItemOverview;
