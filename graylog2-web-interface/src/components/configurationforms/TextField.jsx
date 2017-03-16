import React from 'react';
import FieldHelpers from 'components/configurationforms/FieldHelpers';

const TextField = React.createClass({
  propTypes: {
    autoFocus: React.PropTypes.bool,
    field: React.PropTypes.object.isRequired,
    onChange: React.PropTypes.func.isRequired,
    title: React.PropTypes.string.isRequired,
    typeName: React.PropTypes.string.isRequired,
    value: React.PropTypes.any,
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
  handleChange(evt) {
    this.props.onChange(this.state.title, evt.target.value);
    this.setState({ value: evt.target.value });
  },
  render() {
    const field = this.state.field;
    const title = this.state.title;
    const typeName = this.state.typeName;

    let inputField;
    const isRequired = !field.is_optional;
    const fieldType = (!FieldHelpers.hasAttribute(field.attributes, 'textarea') && FieldHelpers.hasAttribute(field.attributes, 'is_password') ? 'password' : 'text');

    if (FieldHelpers.hasAttribute(field.attributes, 'textarea')) {
      inputField = (
        <textarea id={title} className="form-control" rows={10}
                  name={`configuration[${title}]`} required={isRequired} value={this.state.value}
                  onChange={this.handleChange} autoFocus={this.props.autoFocus} />
      );
    } else {
      inputField = (
        <input id={title} type={fieldType} className="form-control" name={`configuration[${title}]`} value={this.state.value}
               onChange={this.handleChange} required={isRequired} autoFocus={this.props.autoFocus} />
      );
    }

    // TODO: replace with bootstrap input component
    return (
      <div className="form-group">
        <label htmlFor={`${typeName}-${title})`}>
          {field.human_name}
          {FieldHelpers.optionalMarker(field)}
        </label>
        {inputField}
        <p className="help-block">{field.description}</p>
      </div>
    );
  },
});

export default TextField;
