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
import React, { useCallback, useMemo } from 'react';
import styled, { css } from 'styled-components';

import { Modal, Input, ListGroup, ListGroupItem } from 'components/bootstrap';
import { LinkContainer } from 'components/common/router';
import useQuickJumpSearch from 'components/quick-jump/hooks/useQuickJumpSearch';
import StringUtils from 'util/StringUtils';
import type { SearchResultItem } from 'components/quick-jump/Types';
import useLogout from 'hooks/useLogout';
import useQuickJumpKeyboardNavigation from 'components/quick-jump/hooks/useQuickJumpKeyboardNavigation';
import type { QuickJumpItemProps } from 'components/quick-jump/hooks/useQuickJumpKeyboardNavigation';
import Badge from 'components/bootstrap/Badge';

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

type Props = {
  onToggle: () => void;
};

const useActionArguments = () => {
  const logout = useLogout();

  return useMemo(() => ({ logout }), [logout]);
};

const SearchResultEntry = ({
  item,
  onToggle,
  itemProps,
  isActive,
  lastOpened,
}: {
  item: SearchResultItem;
  onToggle: () => void;
  itemProps: QuickJumpItemProps;
  isActive: boolean;
  lastOpened: boolean;
}) => {
  const actionArguments = useActionArguments();
  const isLinkItem = 'link' in item;
  const onClick = isLinkItem
    ? onToggle
    : () => {
        item.action(actionArguments);
        onToggle();
      };

  return (
    <LinkContainer to={isLinkItem ? item.link : undefined} onClick={onClick}>
      <StyledListGroupItem $active={isActive} {...itemProps}>
        <TitleRow>
          <Title>{item.title}</Title>
          {lastOpened ? <LastOpened /> : null}
        </TitleRow>
        <Badge>
          <EntityType>{StringUtils.toTitleCase(item.type, '_')}</EntityType>
        </Badge>
      </StyledListGroupItem>
    </LinkContainer>
  );
};

const QuickJumpModal = ({ onToggle }: Props) => {
  const { searchQuery, setSearchQuery, searchResults } = useQuickJumpSearch();
  const { highlightedIndex, searchInputProps, getItemProps, onHide } = useQuickJumpKeyboardNavigation({
    items: searchResults,
    onToggle,
  });

  const handleSearch = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      setSearchQuery(event.target.value);
    },
    [setSearchQuery],
  );

  return (
    <Modal onHide={onHide} show bsSize="large" scrollInContent>
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
          placeholder="Search..."
          {...searchInputProps}
        />
        <List>
          <ListGroup className="no-bm">
            {searchResults.map((item, index) => (
              <SearchResultEntry
                key={item.key || item.title}
                item={item}
                onToggle={onToggle}
                isActive={highlightedIndex === index}
                itemProps={getItemProps(index)}
                lastOpened={item.lastOpened}
              />
            ))}
          </ListGroup>
        </List>
      </Modal.Body>
    </Modal>
  );
};

export default QuickJumpModal;
