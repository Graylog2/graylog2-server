// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { MenuItem } from 'components/graylog';
import { QueriesActions } from 'views/stores/QueriesStore';
import type { QueryId } from 'views/logic/queries/Query';
import ViewState from 'views/logic/views/ViewState';

import QueryActionDropdown from './QueryActionDropdown';

const TitleWrap = styled.span(({ active }) => `
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

  componentWillReceiveProps(nextProps: Props) {
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
