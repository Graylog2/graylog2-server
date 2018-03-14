import PropTypes from 'prop-types';
import React from 'react';

class SourceType extends React.Component {
  static propTypes = {
    name: PropTypes.string.isRequired,
    description: PropTypes.string.isRequired,
    onSelect: PropTypes.func.isRequired,
  };

  _onChange = (event) => {
    this.props.onSelect(event.target.id, event.target.value);
  };

  render() {
    return (
      <label className="radio">
        <input type="radio" name="sourceType" id={this.props.id} value={this.props.description}
               onChange={this._onChange} />
        {this.props.name}
      </label>
    );
  }
}

export default SourceType;
