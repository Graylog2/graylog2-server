// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { MenuItem } from 'components/graylog';

import { QueriesActions } from 'views/stores/QueriesStore';
import type { QueryId } from 'views/logic/queries/Query';
import ViewState from 'views/logic/views/ViewState';

import QueryActionDropdown from './QueryActionDropdown';

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

  state = {
    editing: false,
    // eslint-disable-next-line react/destructuring-assignment
    title: this.props.title,
  };

  componentWillReceiveProps(nextProps: Props) {
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
    return (
      <span title={title}>
        {title}
        {!editing && active && (
          <QueryActionDropdown>
            <MenuItem onSelect={() => this._onDuplicate(id)}>Duplicate</MenuItem>
            <MenuItem onSelect={() => openEditModal(title)}>Edit Title</MenuItem>
            <MenuItem divider />
            <MenuItem onSelect={this._onClose}>Close</MenuItem>
          </QueryActionDropdown>
        )}
      </span>
    );
  }
}

export default QueryTitle;
