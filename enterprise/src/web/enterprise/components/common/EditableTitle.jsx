import React from 'react';
import PropTypes from 'prop-types';

import styles from './EditableTitle.css';

export default class EditableTitle extends React.Component {
  static propTypes = {
    onChange: PropTypes.func.isRequired,
    onClose: PropTypes.func.isRequired,
    value: PropTypes.string.isRequired,
  };

  state = {
    editing: false,
    value: this.props.value,
  };

  componentWillReceiveProps(nextProps) {
    this.setState({ value: nextProps.value });
  }

  _toggleEditing = () => {
    this.setState(state => ({ editing: !state.editing }));
  };

  _onChange = (evt) => {
    evt.preventDefault();
    this.setState({ value: evt.target.value });
  };

  _onSubmit = (e) => {
    if (this.state.value !== '') {
      this.props.onChange(this.state.value);
    } else {
      this.setState({ value: this.props.value });
    }
    this.setState({ editing: false });
  };

  render() {
    const { onClose } = this.props;
    const { editing, value } = this.state;
    return editing ? (
      <span>
        <form onSubmit={this._onSubmit} className={styles.inlineForm}>
          <input autoFocus
                 type="text"
                 value={value}
                 onBlur={this._toggleEditing}
                 onChange={this._onChange} />
        </form>
      </span>
    ) : <span onDoubleClick={this._toggleEditing}>{value}</span>;
  }
}
