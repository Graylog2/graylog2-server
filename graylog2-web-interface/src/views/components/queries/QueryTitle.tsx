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
import type { QueryId } from 'views/logic/queries/Query';
import DashboardPageContext from 'views/components/contexts/DashboardPageContext';
import type ViewState from 'views/logic/views/ViewState';
import useAppDispatch from 'stores/useAppDispatch';
import { duplicateQuery } from 'views/logic/slices/viewSlice';

import QueryActionDropdown from './QueryActionDropdown';

const Container = styled.div`
  display: flex;
  align-items: center;
`;

const TitleWrap = styled.span<{ $active?: boolean }>(({ $active }) => css`
  padding-right: ${$active ? '6px' : '0'};
`);

type Props = {
  active: boolean,
  allowsClosing?: boolean,
  id: QueryId,
  onRemove: () => Promise<void | ViewState>,
  openEditModal: (title: string) => void,
  openCopyToDashboardModal: (isOpen: boolean) => void,
  title: string,
};

const QueryTitle = ({ active, allowsClosing, id, onRemove, openEditModal, openCopyToDashboardModal, title }: Props) => {
  const [titleValue, setTitleValue] = useState(title);
  const { setDashboardPage } = useContext(DashboardPageContext);
  const dispatch = useAppDispatch();

  useEffect(() => {
    setTitleValue(title);
  }, [title]);

  const _onDuplicate = useCallback(() => dispatch(duplicateQuery(id))
    .then((queryId) => setDashboardPage(queryId)), [dispatch, id, setDashboardPage]);

  return (
    <Container>
      <TitleWrap aria-label={titleValue} $active={active} data-testid="query-tab" data-active-query-tab={active}>
        {titleValue}
      </TitleWrap>

      {active && (
        <QueryActionDropdown>
          <MenuItem onSelect={() => openEditModal(titleValue)}>Edit Title</MenuItem>
          <MenuItem onSelect={_onDuplicate}>Duplicate</MenuItem>
          <MenuItem onSelect={() => openCopyToDashboardModal(true)}>
            Copy to Dashboard
          </MenuItem>
          <MenuItem divider />
          <MenuItem onSelect={onRemove} disabled={!allowsClosing}>Delete</MenuItem>
        </QueryActionDropdown>
      )}
    </Container>
  );
};

QueryTitle.propTypes = {
  allowsClosing: PropTypes.bool,
  onRemove: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
  openEditModal: PropTypes.func.isRequired,
};

QueryTitle.defaultProps = {
  allowsClosing: true,
};

export default QueryTitle;
