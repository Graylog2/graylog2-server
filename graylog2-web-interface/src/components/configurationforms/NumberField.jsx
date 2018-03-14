import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import FieldHelpers from './FieldHelpers';

import FormsUtils from 'util/FormsUtils';

const NumberField = createReactClass({
  displayName: 'NumberField',

  propTypes: {
    autoFocus: PropTypes.bool,
    field: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    title: PropTypes.string.isRequired,
    typeName: PropTypes.string.isRequired,
    value: PropTypes.any,
  },

  MAX_SAFE_INTEGER: (Number.MAX_SAFE_INTEGER !== undefined ? Number.MAX_SAFE_INTEGER : Math.pow(2, 53) - 1),
  MIN_SAFE_INTEGER: (Number.MIN_SAFE_INTEGER !== undefined ? Number.MIN_SAFE_INTEGER : -1 * (Math.pow(2, 53) - 1)),

  _getDefaultValidationSpecs() {
    return { min: this.MIN_SAFE_INTEGER, max: this.MAX_SAFE_INTEGER };
  },

  mapValidationAttribute(attribute) {
    switch (attribute.toLocaleUpperCase()) {
      case 'ONLY_NEGATIVE': return { min: this.MIN_SAFE_INTEGER, max: -1 };
      case 'ONLY_POSITIVE': return { min: 0, max: this.MAX_SAFE_INTEGER };
      case 'IS_PORT_NUMBER': return { min: 0, max: 65535 };
      default: return this._getDefaultValidationSpecs();
    }
  },

  validationSpec(field) {
    const validationAttributes = field.attributes.map(this.mapValidationAttribute);
    if (validationAttributes.length > 0) {
      // The server may return more than one validation attribute, but it doesn't make sense to use more
      // than one validation for a number field, so we return the first one
      return validationAttributes[0];
    }

    return this._getDefaultValidationSpecs();
  },

  handleChange(evt) {
    const numericValue = FormsUtils.getValueFromInput(evt.target);
    this.props.onChange(this.props.title, numericValue);
  },

  render() {
    const typeName = this.props.typeName;
    const field = this.props.field;
    const isRequired = !field.is_optional;
    const validationSpecs = this.validationSpec(field);

    // TODO: replace with bootstrap input component
    return (
      <div className="form-group">
        <label htmlFor={`${typeName}-${field.title}`}>
          {field.human_name}
          {FieldHelpers.optionalMarker(field)}
        </label>

        <input id={field.title} type="number" required={isRequired} onChange={this.handleChange}
               value={this.props.value} className="input-xlarge validatable form-control"
          {...validationSpecs} autoFocus={this.props.autoFocus} />

        <p className="help-block">{field.description}</p>
      </div>
    );
  },
});

export default NumberField;
