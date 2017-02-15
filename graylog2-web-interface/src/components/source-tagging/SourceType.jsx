import React from 'react';

const SourceType = React.createClass({
  propTypes: {
    name: React.PropTypes.string.isRequired,
    description: React.PropTypes.string.isRequired,
    onSelect: React.PropTypes.func.isRequired,
  },
  _onChange(event) {
    this.props.onSelect(event.target.id, event.target.value);
  },
  render() {
    return (
      <label className="radio">
        <input type="radio" name="sourceType" id={this.props.id} value={this.props.description}
               onChange={this._onChange} />
        {this.props.name}
      </label>
    );
  },
});

export default SourceType;
