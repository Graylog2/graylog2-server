import React from 'react';
import FieldHelpers from './FieldHelpers';

const BooleanField = React.createClass({
  propTypes: {
    autoFocus: React.PropTypes.bool,
    field: React.PropTypes.object.isRequired,
    onChange: React.PropTypes.func.isRequired,
    title: React.PropTypes.string.isRequired,
    typeName: React.PropTypes.string.isRequired,
    value: React.PropTypes.any,
  },
  render() {
    const field = this.props.field;
    const typeName = this.props.typeName;
    const title = this.props.title;
    const value = this._getEffectiveValue();
    return (
      <div className="form-group">
        <div className="checkbox">
          <label>
            <input id={typeName + '-' + field.title}
                   type="checkbox"
                   checked={value}
                   name={'configuration[' + field.title + ']'}
                   onChange={this.handleChange}
                   autoFocus={this.props.autoFocus} />

            {field.human_name}

            {FieldHelpers.optionalMarker(field)}
          </label>
        </div>
        <p className="help-block">{field.description}</p>
      </div>
    );
  },
  _getEffectiveValue() {
    return (this.props.value === undefined ? this.props.field.default_value : this.props.value);
  },
  handleChange() {
    const newValue = !this._getEffectiveValue();
    this.setState({value: newValue});
    this.props.onChange(this.props.title, newValue);
  },
});

export default BooleanField;
