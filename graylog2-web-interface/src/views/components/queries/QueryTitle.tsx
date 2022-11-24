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
import { useEffect, useState, useCallback, useContext } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import { MenuItem } from 'components/bootstrap';
import { QueriesActions } from 'views/stores/QueriesStore';
import type { QueryId } from 'views/logic/queries/Query';
import type { QueriesList } from 'views/actions/QueriesActions';
import DashboardPageContext from 'views/components/contexts/DashboardPageContext';
import type ViewState from 'views/logic/views/ViewState';

import QueryActionDropdown from './QueryActionDropdown';

const TitleWrap = styled.span<{ active: boolean }>(({ active }) => css`
  padding-right: ${active ? '6px' : '0'};
`);

type Props = {
  active: boolean,
  allowsClosing?: boolean,
  id: QueryId,
  onClose: () => Promise<void | ViewState>,
  openEditModal: (string) => void,
  title: string,
};

const QueryTitle = ({ active, allowsClosing, id, onClose, openEditModal, title }: Props) => {
  const [titleValue, setTitleValue] = useState(title);
  const { setDashboardPage } = useContext(DashboardPageContext);

  useEffect(() => {
    setTitleValue(title);
  }, [title]);

  const _onDuplicate = useCallback(() => QueriesActions.duplicate(id).then(
    (queryList: QueriesList) => setDashboardPage(queryList.keySeq().last()),
  ), [id, setDashboardPage]);

  return (
    <>
      <TitleWrap aria-label={titleValue} active={active}>
        {titleValue}
      </TitleWrap>

      {active && (
        <QueryActionDropdown>
          <MenuItem onSelect={() => _onDuplicate()}>Duplicate</MenuItem>
          <MenuItem onSelect={() => openEditModal(titleValue)}>Edit Title</MenuItem>
          <MenuItem divider />
          <MenuItem onSelect={onClose} disabled={!allowsClosing}>Delete</MenuItem>
        </QueryActionDropdown>
      )}
    </>
  );
};

QueryTitle.propTypes = {
  allowsClosing: PropTypes.bool,
  onClose: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
  openEditModal: PropTypes.func.isRequired,
};

QueryTitle.defaultProps = {
  allowsClosing: true,
};

export default QueryTitle;
