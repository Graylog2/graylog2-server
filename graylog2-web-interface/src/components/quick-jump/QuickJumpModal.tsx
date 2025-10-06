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

import { Modal, Input } from 'components/bootstrap';

const SearchInput = styled(Input)`
  width: 100%;
`;

const useQuickJumpSearch = () => {
  const [searchQuery, setSearchQuery] = useState('');

  const handleSearch = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearchQuery(event.target.value);
  };

  return {
    searchQuery,
    setSearchQuery: handleSearch,
  };
};

type Props = {
  onToggle: () => void;
};

const QuickJumpModal = ({ onToggle }: Props) => {
  const { searchQuery, setSearchQuery } = useQuickJumpSearch();

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
        Input List
      </Modal.Body>
    </Modal>
  );
};

export default QuickJumpModal;
