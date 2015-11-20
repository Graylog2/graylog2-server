import $ from 'jquery';

import React from 'react';
import FieldHelpers from 'components/configurationforms/FieldHelpers';

const DropdownField = React.createClass({
  propTypes: {
    autoFocus: React.PropTypes.bool.isRequired,
    field: React.PropTypes.object.isRequired,
    onChange: React.PropTypes.func.isRequired,
    title: React.PropTypes.string.isRequired,
    typeName: React.PropTypes.string.isRequired,
    value: React.PropTypes.any.isRequired,
  },
  getInitialState() {
    return {
      typeName: this.props.typeName,
      field: this.props.field,
      title: this.props.title,
      value: this.props.value,
    };
  },
  componentWillReceiveProps(props) {
    this.setState(props);
  },
  _formatOption(value, key) {
    return (
      <option key={this.state.typeName + '-' + this.state.field + '-' + key}value={key} id={key}>{value}</option>
    );
  },
  handleChange(evt) {
    this.props.onChange(this.state.title, evt.target.value);
    this.setState({value: evt.target.value});
  },
  render() {
    const field = this.state.field;
    const options = $.map(field.additional_info.values, this._formatOption);
    const typeName = this.state.typeName;
    return (
      <div className="form-group">
        <label htmlFor={typeName + '-' + field.title}>
          {field.human_name}

          {FieldHelpers.optionalMarker(field)}
        </label>

        <select id={field.title} defaultValue={field.default_value} value={this.state.value}
                className="input-xlarge form-control" onChange={this.handleChange}
                autoFocus={this.props.autoFocus} >
          {options}
        </select>
        <p className="help-block">{field.description}</p>
      </div>
    );
  },
});

export default DropdownField;
