import PropTypes from 'prop-types';
import React from 'react';

import { MultiSelect } from 'components/common';
import { FieldHelpers } from 'components/configurationforms';

class ListField extends React.Component {
  static propTypes = {
    autoFocus: PropTypes.bool.isRequired,
    field: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    title: PropTypes.string.isRequired,
    typeName: PropTypes.string.isRequired,
    value: PropTypes.oneOfType([PropTypes.array, PropTypes.string]),
    addPlaceholder: PropTypes.bool,
    disabled: PropTypes.bool,
  };

  static defaultProps = {
    addPlaceholder: false,
  };

  state = {
    typeName: this.props.typeName,
    field: this.props.field,
    title: this.props.title,
    value: this.props.value,
  };

  componentWillReceiveProps(props) {
    this.setState(props);
  }

  _formatOption = (key, value) => {
    return { value: value, label: key };
  };

  _handleChange = (nextValue) => {
    const values = (nextValue === '' ? [] : nextValue.split(','));
    this.props.onChange(this.state.title, values);
    this.setState({ value: values });
  };

  render() {
    const { field } = this.state;
    const { typeName } = this.state;
    const { value } = this.state;
    const isRequired = !field.is_optional;
    const allowCreate = field.attributes.includes('allow_create');
    const options = (field.additional_info && field.additional_info.values ? field.additional_info.values : {});
    const formattedOptions = Object.keys(options).map((key) => this._formatOption(key, options[key]));

    return (
      <div className="form-group">
        <label htmlFor={`${typeName}-${field.title}`}>
          {field.human_name}

          {FieldHelpers.optionalMarker(field)}
        </label>

        <MultiSelect id={field.title}
                     required={isRequired}
                     autoFocus={this.props.autoFocus}
                     options={formattedOptions}
                     value={value ? (Array.isArray(value) ? value.join(',') : value) : undefined}
                     placeholder={`${allowCreate ? 'Add' : 'Select'} ${field.human_name}`}
                     onChange={this._handleChange}
                     disabled={this.props.disabled}
                     allowCreate={allowCreate} />

        <p className="help-block">{field.description}</p>
      </div>
    );
  }
}

export default ListField;
