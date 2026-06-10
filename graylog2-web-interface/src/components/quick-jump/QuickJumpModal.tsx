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
import React, { useCallback, useState } from 'react';
import styled, { css } from 'styled-components';

import { Modal, Input, ListGroup } from 'components/bootstrap';
import useQuickJumpSearch from 'components/quick-jump/hooks/useQuickJumpSearch';
import type { SearchResultItem } from 'components/quick-jump/Types';
import type { QuickJumpItemProps } from 'components/quick-jump/hooks/useQuickJumpKeyboardNavigation';
import useQuickJumpKeyboardNavigation from 'components/quick-jump/hooks/useQuickJumpKeyboardNavigation';
import Spinner from 'components/common/Spinner';
import useDebouncedValue from 'hooks/useDebouncedValue';

import SearchResultEntry from './SearchResultEntry';

const SearchInput = styled(Input)`
  width: 100%;
`;

const List = styled.div`
  overflow: auto;
  outline: none;
`;

type Props = {
  onToggle: () => void;
};

type SearchResultsProps = {
  searchResults: SearchResultItem[];
  highlightedIndex: number;
  getItemProps: (index: number) => QuickJumpItemProps;
};
const SearchResults = ({ searchResults, highlightedIndex, getItemProps }: SearchResultsProps) => (
  <List>
    <ListGroup className="no-bm">
      {searchResults.map((item, index) => (
        <SearchResultEntry
          key={item.key || item.title}
          item={item}
          isActive={highlightedIndex === index}
          itemProps={getItemProps(index)}
          lastOpened={item.last_opened}
          favorite={item.favorite}
        />
      ))}
    </ListGroup>
  </List>
);

const Explanation = styled.div`
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  width: 100%;
`;
const Description = styled.div(
  ({ theme }) => css`
    color: ${theme.colors.text.secondary};
    margin: 0;
  `,
);

const QuickJumpIntro = () => (
  <Explanation>
    <Description>
      Please enter a search query to search for a page, stream, dashboard, input or other entities.{' '}
    </Description>
    <Description>
      To give you a head start, you will find a list of your <strong>last opened</strong> and <strong>favorite</strong>{' '}
      items below:
    </Description>
    <br />
  </Explanation>
);

const NoResultsFound = () => (
  <Explanation>
    <Description>No search results found. Please try different search terms.</Description>
  </Explanation>
);

const SearchInputContainer = styled.div`
  display: inline-block;
  position: relative;
`;
const InputFeedback = styled.div`
  position: absolute;
  right: 0;
  top: 0;
  display: flex;
  min-height: 34px;
  padding-right: 3px;
`;

const QuickJumpModal = ({ onToggle }: Props) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedSearchQuery] = useDebouncedValue(searchQuery, 500);
  const { searchResults, isLoading } = useQuickJumpSearch(debouncedSearchQuery);
  const { highlightedIndex, modalProps, searchInputProps, getItemProps, onHide } = useQuickJumpKeyboardNavigation({
    items: searchResults,
    onToggle,
    searchQuery,
  });

  const hasEmptySearchQuery = debouncedSearchQuery.trim() === '';

  const handleSearch = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      setSearchQuery(event.target.value);
    },
    [setSearchQuery],
  );

  return (
    <Modal onHide={onHide} show bsSize="large" scrollInContent rootProps={modalProps}>
      <Modal.Header>
        <Modal.Title>Quick Jump</Modal.Title>
      </Modal.Header>

      <Modal.Body>
        <SearchInputContainer>
          <SearchInput
            value={searchQuery}
            data-autofocus
            id="quick-jump-search"
            type="text"
            onChange={handleSearch}
            placeholder="Search for pages/dashboards/streams ..."
            {...searchInputProps}
          />
          <InputFeedback>{isLoading && <Spinner delay={0} text="" />}</InputFeedback>
        </SearchInputContainer>
        {!isLoading && searchResults.length === 0 ? (
          <NoResultsFound />
        ) : (
          <>
            {hasEmptySearchQuery && <QuickJumpIntro />}
            <SearchResults
              searchResults={searchResults}
              getItemProps={getItemProps}
              highlightedIndex={highlightedIndex}
            />
          </>
        )}
      </Modal.Body>
    </Modal>
  );
};

export default QuickJumpModal;
