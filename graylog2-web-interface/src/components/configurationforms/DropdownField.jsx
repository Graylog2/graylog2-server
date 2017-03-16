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
    value: React.PropTypes.any,
    addPlaceholder: React.PropTypes.bool,
    disabled: React.PropTypes.bool,
  },
  getDefaultProps() {
    return {
      addPlaceholder: false,
    };
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
  _formatOption(value, key, disabled) {
    return (
      <option key={`${this.state.typeName}-${this.state.title}-${key}`} value={key} id={key} disabled={disabled}>{value}</option>
    );
  },
  handleChange(evt) {
    this.props.onChange(this.state.title, evt.target.value);
    this.setState({ value: evt.target.value });
  },
  render() {
    const field = this.state.field;
    const options = $.map(field.additional_info.values, this._formatOption);
    if (this.props.addPlaceholder) {
      options.unshift(this._formatOption(`Select ${field.human_name || this.state.title}`, '', true));
    }
    const typeName = this.state.typeName;
    return (
      <div className="form-group">
        <label htmlFor={`${typeName}-${field.title}`}>
          {field.human_name}

          {FieldHelpers.optionalMarker(field)}
        </label>

        <select id={field.title} value={this.state.value}
                className="input-xlarge form-control" onChange={this.handleChange}
                autoFocus={this.props.autoFocus} disabled={this.props.disabled} required={!field.is_optional}>
          {options}
        </select>
        <p className="help-block">{field.description}</p>
      </div>
    );
  },
});

export default DropdownField;
