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

export type DescriptiveItem = {
  +id: string,
  +name: string,
  +description: string,
};

type ListOfDescriptiveItems = Immutable.List<DescriptiveItem>;

export type PaginatedListType = {
  pagination: PaginationInfo,
  list: ListOfDescriptiveItems,
};

type Props = {
  onLoad: (paginationInfo: PaginationInfo, isSubscribed: boolean) => Promise<?PaginatedListType>,
  overrideList?: PaginatedListType,
  onDeleteItem?: (DescriptiveItem) => void,
};

const pageSizes = [5, 10, 30];
export const defaultPageInfo = { page: INITIAL_PAGE, perPage: pageSizes[0], query: '', total: 0, count: 0 };

const PaginatedItemOverview = ({ onLoad, overrideList, onDeleteItem }: Props) => {
  const [items, setItems] = useState();
  const [paginationInfo, setPaginationInfo] = useState(defaultPageInfo);

  const _setResponse = (response: ?PaginatedListType) => {
    if (!response) {
      return;
    }

    const { list, pagination } = response;
    setPaginationInfo(pagination);
    setItems(list);
  };

  useEffect(() => _setResponse(overrideList), [overrideList]);

  useEffect(() => {
    let isSubscribed = true;

    onLoad(paginationInfo, isSubscribed).then((response) => {
      if (isSubscribed) {
        _setResponse(response);
      }
    });

    return () => { isSubscribed = false; };
  }, []);

  const _onPageChange = (query) => (page, perPage) => {
    const pageInfo = {
      ...paginationInfo,
      query,
      page,
      perPage,
    };
    onLoad(pageInfo, true).then(_setResponse);
  };

  const _onSearch = (query) => {
    const pageInfo = {
      ...paginationInfo,
      page: INITIAL_PAGE,
      query,
    };
    onLoad(pageInfo, true).then(_setResponse);
  };

  const result = items && items.size > 0
    ? items.toArray().map((item) => <PaginatedItem key={item.id} onDeleteItem={onDeleteItem} item={item} />)
    : <NotFound>No items found to display</NotFound>;

  return (
    <PaginatedList onChange={_onPageChange(paginationInfo.query)}
                   pageSize={paginationInfo.perPage}
                   totalItems={paginationInfo.total}
                   pageSizes={pageSizes}
                   activePage={paginationInfo.page}>
      <SearchForm onSearch={_onSearch} label="Filter" placeholder="Enter query to filter" searchButtonLabel="Filter" />
      <Container>
        {result}
      </Container>
    </PaginatedList>
  );
};

PaginatedItemOverview.defaultProps = {
  onDeleteItem: undefined,
  overrideList: undefined,
};

export default PaginatedItemOverview;
