import React from 'react';
import PropTypes from 'prop-types';

class QueryTitle extends React.Component {
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
    const valueField = editing ? (
      <form onSubmit={this._onSubmit}>
        <input autoFocus
               type="text"
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
        {!editing && <i className="fa fa-times"
                        role="button"
                        tabIndex={0}
                        onClick={(e) => {
                          e.preventDefault();
                          onClose();
                        }} />}
      </span>
    );
  }
}

export default QueryTitle;
    