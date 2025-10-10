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

import { Modal, Input, ListGroup, ListGroupItem } from 'components/bootstrap';
import useQuickJumpSearch from 'components/quick-jump/hooks/useQuickJumpSearch';
import StringUtils from 'util/StringUtils';
import type { SearchResultItem } from 'components/quick-jump/Types';
import useQuickJumpKeyboardNavigation from 'components/quick-jump/hooks/useQuickJumpKeyboardNavigation';
import type { QuickJumpItemProps } from 'components/quick-jump/hooks/useQuickJumpKeyboardNavigation';
import Badge from 'components/bootstrap/Badge';
import Spinner from 'components/common/Spinner';
import { Icon } from 'components/common';
import { EXTERNAL_PAGE_TYPE } from 'components/quick-jump/Constants';
import useDebouncedValue from 'hooks/useDebouncedValue';

const SearchInput = styled(Input)`
  width: 100%;
`;

const List = styled.div`
  overflow: auto;
  outline: none;
`;

const EntityType = styled.div`
  font-weight: normal;
`;

const StyledListGroupItem = styled(ListGroupItem)<{ $active?: boolean }>(({ theme, $active }) => {
  const highlightColor = theme.utils.colorLevel(theme.colors.global.contentBackground, 10);

  return css`
    position: relative;
    cursor: pointer;
    transition: background-color 0.15s ease-in-out;

    &::before {
      content: '';
      position: absolute;
      inset: 0 auto 0 0;
      width: 0;
      background-color: ${theme.colors.variant.primary};
      transition: width 0.15s ease-in-out;
    }

    ${$active &&
    css`
      background-color: ${highlightColor};

      & > .list-group-item {
        background-color: ${highlightColor};
        color: ${theme.colors.text.primary};
      }

      &::before {
        width: 4px;
      }

      a {
        color: inherit;
        text-decoration: none;
      }
    `}
  `;
});

const TitleRow = styled.div`
  display: flex;
  justify-content: space-between;
`;

const Title = styled.div``;
const LastOpened = () => <Badge bsStyle="primary">Recent</Badge>;

const ExternalIcon = styled(Icon)(
  ({ theme }) => css`
    margin-left: ${theme.spacings.xxs};
    font-size: ${theme.fonts.size.small};
    top: -1px;
    position: relative;
  `,
);

type Props = {
  onToggle: () => void;
};

const SearchResultEntry = ({
  item,
  itemProps,
  isActive,
  lastOpened,
  favorite,
}: {
  item: SearchResultItem;
  itemProps: QuickJumpItemProps;
  isActive: boolean;
  lastOpened: boolean;
  favorite: boolean;
}) => (
  <StyledListGroupItem $active={isActive} {...itemProps}>
    <TitleRow>
      <Title>
        {favorite ? <Icon name="star" type="solid" /> : null}
        {item.title}
        {item.type === EXTERNAL_PAGE_TYPE && <ExternalIcon name="open_in_new" />}
      </Title>
      {lastOpened ? <LastOpened /> : null}
    </TitleRow>
    <Badge>
      <EntityType>{StringUtils.toTitleCase(item.type, '_')}</EntityType>
    </Badge>
  </StyledListGroupItem>
);

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

const IntroContainer = styled.div`
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
  <IntroContainer>
    <Description>
      Please enter a search query to search for a page, stream, dashboard, input or other entities.{' '}
    </Description>
    <Description>
      To give you a head start, you will find a list of your <strong>last opened</strong> and <strong>favorite</strong>{' '}
      items below:
    </Description>
    <br />
  </IntroContainer>
);

const QuickJumpModal = ({ onToggle }: Props) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedSearchQuery] = useDebouncedValue(searchQuery, 500);
  const { searchResults, isLoading } = useQuickJumpSearch(debouncedSearchQuery);
  const { highlightedIndex, searchInputProps, getItemProps, onHide, handleTyping } = useQuickJumpKeyboardNavigation({
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
    <Modal onHide={onHide} show bsSize="large" scrollInContent rootProps={{ onKeyDownCapture: handleTyping }}>
      <Modal.Header>
        <Modal.Title>Quick Jump</Modal.Title>
      </Modal.Header>

      <Modal.Body>
        <SearchInput
          value={searchQuery}
          data-autofocus
          id="quick-jump-search"
          type="text"
          onChange={handleSearch}
          placeholder="Search for pages/dashboards/streams ..."
          {...searchInputProps}
        />
        {isLoading ? (
          <Spinner />
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
