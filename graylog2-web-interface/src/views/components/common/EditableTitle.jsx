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

  state = {
    editing: false,
    onChange: () => {},
    value: this.props.value,
  };

  componentWillReceiveProps(nextProps) {
    this.setState({ value: nextProps.value });
  }

  _toggleEditing = () => {
    if (!this.props.disabled) {
      this.setState(state => ({ editing: !state.editing }));
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

  _onSubmit = () => {
    if (this.state.value !== '') {
      this.props.onChange(this.state.value);
    } else {
      this.setState({ value: this.props.value });
    }
    this.setState({ editing: false });
  };

  render() {
    const { editing, value } = this.state;
    return editing ? (
      <span>
        <form onSubmit={this._onSubmit} className={styles.inlineForm}>
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
