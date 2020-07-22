// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';
import styled, { css, type StyledComponent } from 'styled-components';
import * as Immutable from 'immutable';

import PaginatedList, { INITIAL_PAGE } from 'components/common/PaginatedList';
import { type ThemeInterface } from 'theme';
import { SearchForm } from 'components/common';

import PaginatedItem from './PaginatedItem';

const Container = styled.div`
  margin-top: 10px;
`;

const NotFound: StyledComponent<{}, ThemeInterface, HTMLSpanElement> = styled.span(({ theme }) => css`
  margin-left: 5px;
  color: ${theme.colors.gray[50]};
`);

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
  onLoad: (PaginationInfo) => Promise<?PaginatedListType>,
};

const PaginatedItemOverview = ({ onLoad }: Props) => {
  const pageSizes = [5, 10, 30];
  const [items, setItems] = useState();
  const [paginationInfo, setPaginationInfo] = useState({
    count: 0,
    total: 0,
    page: INITIAL_PAGE,
    perPage: pageSizes[0],
    query: '',
  });

  const _setResponse = (response: ?PaginatedListType) => {
    if (!response) {
      return;
    }

    const { list, pagination } = response;
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
      page: INITIAL_PAGE,
      query,
    };
    onLoad(pageInfo).then(_setResponse);
  };

  const result = items && items.size > 0
    ? items.toArray().map((item) => <PaginatedItem key={item.id} item={item} />)
    : <NotFound>No items found to display</NotFound>;

  return (
    <PaginatedList onChange={_onPageChange(paginationInfo.query)}
                   pageSize={paginationInfo.perPage}
                   totalItems={paginationInfo.total}
                   pageSizes={pageSizes}
                   activePage={paginationInfo.page}>
      <SearchForm onSearch={_onSearch} />
      <Container>
        {result}
      </Container>
    </PaginatedList>
  );
};

export default PaginatedItemOverview;
