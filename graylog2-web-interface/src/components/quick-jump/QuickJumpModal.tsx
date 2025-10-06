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
import React, { useEffect, useRef } from 'react';
import styled from 'styled-components';

import { Modal, Input, ListGroup, ListGroupItem } from 'components/bootstrap';
import { LinkContainer } from 'components/common/router';
import useQuickJumpSearch from 'components/quick-jump/hooks/useQuickJumpSearch';

const SearchInput = styled(Input)`
  width: 100%;
`;

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
