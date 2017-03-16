import React from 'react';
import FieldHelpers from './FieldHelpers';

import FormsUtils from 'util/FormsUtils';

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
    return (
      <div className="form-group">
        <div className="checkbox">
          <label>
            <input id={`${typeName}-${title}`}
                   type="checkbox"
                   checked={this.props.value}
                   name={`configuration[${title}]`}
                   onChange={this.handleChange} />

            {field.human_name}

            {FieldHelpers.optionalMarker(field)}
          </label>
        </div>
        <p className="help-block">{field.description}</p>
      </div>
    );
  },
  handleChange(event) {
    const newValue = FormsUtils.getValueFromInput(event.target);
    this.props.onChange(this.props.title, newValue);
  },
});

export default BooleanField;
