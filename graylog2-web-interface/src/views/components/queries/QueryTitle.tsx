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
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import { MenuItem } from 'components/graylog';
import { QueriesActions } from 'views/stores/QueriesStore';
import type { QueryId } from 'views/logic/queries/Query';
import ViewState from 'views/logic/views/ViewState';

import QueryActionDropdown from './QueryActionDropdown';

const TitleWrap = styled.span(({ active }) => css`
  padding-right: ${active ? '6px' : '0'};
`);

type Props = {
  active: boolean,
  id: QueryId,
  onClose: () => Promise<void> | Promise<ViewState>,
  openEditModal: (string) => void,
  title: string,
};

type State = {
  editing: boolean,
  title: string,
};

class QueryTitle extends React.Component<Props, State> {
  static propTypes = {
    onClose: PropTypes.func.isRequired,
    title: PropTypes.string.isRequired,
    openEditModal: PropTypes.func.isRequired,
  };

  constructor(props: Props) {
    super(props);

    this.state = {
      editing: false,
      title: props.title,
    };
  }

  // eslint-disable-next-line camelcase
  UNSAFE_componentWillReceiveProps(nextProps: Props) {
    /** TODO: Replace componentWillReceiveProps
     * https://reactjs.org/blog/2018/06/07/you-probably-dont-need-derived-state.html#anti-pattern-unconditionally-copying-props-to-state
     */
    this.setState({ title: nextProps.title });
  }

  _onClose = () => {
    const { onClose } = this.props;

    onClose();
  };

  _onDuplicate = (id: QueryId) => QueriesActions.duplicate(id);

  render() {
    const { editing, title } = this.state;
    const { active, id, openEditModal } = this.props;
    const isActive = !editing && active;

    return (
      <>
        <TitleWrap aria-label={title} active={isActive}>
          {title}
        </TitleWrap>

        {isActive && (
          <QueryActionDropdown>
            <MenuItem onSelect={() => this._onDuplicate(id)}>Duplicate</MenuItem>
            <MenuItem onSelect={() => openEditModal(title)}>Edit Title</MenuItem>
            <MenuItem divider />
            <MenuItem onSelect={this._onClose}>Close</MenuItem>
          </QueryActionDropdown>
        )}
      </>
    );
  }
}

export default QueryTitle;
