import React from 'react';
import PropTypes from 'prop-types';

import styles from './EditableTitle.css';

export default class EditableTitle extends React.Component {
  static propTypes = {
    disabled: PropTypes.bool,
    onChange: PropTypes.func,
    value: PropTypes.string.isRequired,
  };

  static defaultProps = {
    disabled: false,
    onChange: () => {},
  };

  constructor(props) {
    super(props);
    const { value } = props;
    this.state = {
      editing: false,
      value,
    };
  }

  _toggleEditing = () => {
    const { disabled } = this.props;
    if (!disabled) {
      this.setState((state) => ({ editing: !state.editing }));
    }
  };

  _onBlur = () => {
    this._toggleEditing();
    this._onSubmit();
  };

  _onChange = (evt) => {
    evt.preventDefault();
    this.setState({ value: evt.target.value });
  };

  _onSubmit = (e) => {
    e.preventDefault();
    const { value } = this.state;
    const { onChange, value: propsValue } = this.props;
    if (value !== '') {
      onChange(value);
    } else {
      this.setState({ value: propsValue });
    }
    this.setState({ editing: false });
  };

  render() {
    const { editing, value } = this.state;
    return editing ? (
      <span>
        <form onSubmit={this._onSubmit} className={styles.inlineForm}>
          {/* eslint-disable-next-line jsx-a11y/no-autofocus */}
          <input autoFocus
                 type="text"
                 value={value}
                 onBlur={this._onBlur}
                 onChange={this._onChange} />
        </form>
      </span>
    ) : <span onDoubleClick={this._toggleEditing} title="Double click the title to edit it.">{value}</span>;
  }
}
