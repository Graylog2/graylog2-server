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
  getInitialState() {
    return {
      typeName: this.props.typeName,
      field: this.props.field,
      title: this.props.title,
      value: (this.props.value === undefined ? this.props.field.default_value : this.props.value),
    };
  },
  componentWillReceiveProps(props) {
    this.setState(props);
  },
  handleChange() {
    const newValue = !this.state.value;
    this.setState({value: newValue});
    this.props.onChange(this.state.title, newValue);
  },
  render() {
    const field = this.state.field;
    const typeName = this.state.typeName;
    const value = this.state.value;
    // TODO: replace with bootstrap input component
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
});

export default BooleanField;
