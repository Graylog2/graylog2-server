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
import React, { useState } from 'react';
import styled from 'styled-components';
import type * as Immutable from 'immutable';

import { Modal, Input, ListGroup, ListGroupItem } from 'components/bootstrap';
import usePluginEntities from 'hooks/usePluginEntities';
import { isPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';
import useActivePerspective from 'components/perspectives/hooks/useActivePerspective';
import { DEFAULT_PERSPECTIVE } from 'components/perspectives/contexts/PerspectivesProvider';
import { LinkContainer } from 'components/common/router';

const SearchInput = styled(Input)`
  width: 100%;
`;

const matchesPerspective = (activePerspective: string, itemPerspective: string) =>
  activePerspective === DEFAULT_PERSPECTIVE ? !itemPerspective : itemPerspective === activePerspective;

const matchesPermission = (userPermissions: Immutable.List<string>, itemPermissions: Array<string>) =>
  isPermitted(userPermissions, itemPermissions);

const useMainNavigationItems = () => {
  const currentUser = useCurrentUser();
  const allNavigationItems = usePluginEntities('navigation') as any;
  const { activePerspective } = useActivePerspective();

  const navigationLinks = allNavigationItems.filter((item) => !item.children);
  const dropdownLinks = allNavigationItems
    .filter((item) => item.children)
    .flatMap((item) =>
      item.children.map((child) => ({ ...child, description: `${item.description} / ${child.description}` })),
    );

  return [...navigationLinks, ...dropdownLinks]
    .filter((item) => {
      if (!matchesPermission(currentUser.permissions, item.permissions)) {
        return false;
      }

      if (!matchesPerspective(activePerspective.id, item.perspective)) {
        return false;
      }

      return true;
    })
    .map((item) => ({ type: 'page', link: item.path, title: item.description }));
};

const useQuickJumpSearch = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const allNavItems = useMainNavigationItems();

  // {
  //   type: 'page'
  //   link: '/search'
  //   title: 'Search'
  // }

  const handleSearch = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearchQuery(event.target.value);
  };

  return {
    searchQuery,
    searchResults: allNavItems,
    setSearchQuery: handleSearch,
  };
};

const List = styled.div`
  overflow: auto;
  max-height: calc(90vh - 150px);
`;

type Props = {
  onToggle: () => void;
};

const QuickJumpModal = ({ onToggle }: Props) => {
  const { searchQuery, setSearchQuery, searchResults } = useQuickJumpSearch();

  return (
    <Modal onHide={onToggle} show bsSize="large">
      <Modal.Header>
        <Modal.Title>Quick Jump</Modal.Title>
      </Modal.Header>

      <Modal.Body>
        <SearchInput
          value={searchQuery}
          id="quick-jump-search"
          type="text"
          onChange={setSearchQuery}
          placeholder="Search..."
          /* eslint-disable-next-line jsx-a11y/no-autofocus */
          autoFocus
        />
        <List>
          <ListGroup>
            {searchResults.map((item) => (
              <LinkContainer to={item.link} key={item.title} onClick={onToggle}>
                <ListGroupItem>{item.title}</ListGroupItem>
              </LinkContainer>
            ))}
          </ListGroup>
        </List>
      </Modal.Body>
    </Modal>
  );
};

export default QuickJumpModal;
