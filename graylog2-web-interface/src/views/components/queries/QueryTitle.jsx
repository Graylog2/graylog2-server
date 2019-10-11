// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { MenuItem } from 'components/graylog';

import { QueriesActions } from 'views/stores/QueriesStore';
import { ViewActions } from 'views/stores/ViewStore';
import type { QueryId } from 'views/logic/queries/Query';

import QueryActionDropdown from './QueryActionDropdown';

type Props = {
  active: boolean,
  id: QueryId,
  onChange: (string) => void,
  onClose: () => void,
  value: string,
};

type State = {
  editing: boolean,
  value: string,
};

class QueryTitle extends React.Component<Props, State> {
  static propTypes = {
    onChange: PropTypes.func.isRequired,
    onClose: PropTypes.func.isRequired,
    value: PropTypes.string.isRequired,
  };

  state = {
    editing: false,
    // eslint-disable-next-line react/destructuring-assignment
    value: this.props.value,
  };

  componentWillReceiveProps(nextProps: Props) {
    this.setState({ value: nextProps.value });
  }

  _toggleEditing = () => {
    this.setState(state => ({ editing: !state.editing }));
  };

  // eslint-disable-next-line no-undef
  _onChange = (evt: SyntheticInputEvent<HTMLInputElement>) => {
    evt.preventDefault();
    evt.stopPropagation();
    this.setState({ value: evt.target.value });
  };

  _onClose = () => {
    const { onClose } = this.props;
    onClose();
  };

  _onSubmit = () => {
    const { value } = this.state;
    if (value !== '') {
      const { onChange } = this.props;
      onChange(value);
    } else {
      // eslint-disable-next-line react/destructuring-assignment
      this.setState({ value: this.props.value });
    }
    this.setState({ editing: false });
  };

  _onDuplicate = (id: QueryId) => {
    QueriesActions.duplicate(id)
      .then(newQuery => ViewActions.selectQuery(newQuery.id));
  };

  render() {
    const { editing, value } = this.state;
    const { active, id } = this.props;
    const valueField = editing ? (
      <form onSubmit={this._onSubmit}>
        <input type="text"
               value={value}
               onFocus={e => e.target.select()}
               onBlur={this._toggleEditing}
               onChange={this._onChange} />
      </form>
    ) : (
      <span onDoubleClick={this._toggleEditing}>
        {value}
      </span>
    );
    return (
      <span>
        {valueField}{' '}
        {!editing && active && (
          <QueryActionDropdown>
            <MenuItem onSelect={() => this._onDuplicate(id)}>Duplicate</MenuItem>
            <MenuItem divider />
            <MenuItem onSelect={this._onClose}>Close</MenuItem>
          </QueryActionDropdown>
        )}
      </span>
    );
  }
}

export default QueryTitle;
