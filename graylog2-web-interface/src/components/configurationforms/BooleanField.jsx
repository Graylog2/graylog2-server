import PropTypes from 'prop-types';
import React from 'react';
import FieldHelpers from './FieldHelpers';

import FormsUtils from 'util/FormsUtils';

class BooleanField extends React.Component {
  static propTypes = {
    autoFocus: PropTypes.bool,
    field: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    title: PropTypes.string.isRequired,
    typeName: PropTypes.string.isRequired,
    value: PropTypes.any,
  };

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
  }

  handleChange = (event) => {
    const newValue = FormsUtils.getValueFromInput(event.target);
    this.props.onChange(this.props.title, newValue);
  };
}

export default BooleanField;
