'use strict';

var $ = require('jquery'); // excluded and shimed

var React = require('react/addons');
var FieldHelpers = require('./FieldHelpers');

var DropdownField = React.createClass({
    getInitialState() {
        return {
            typeName: this.props.typeName,
            field: this.props.field,
            title: this.props.title,
            value: this.props.value
        };
    },
    componentWillReceiveProps(props) {
        this.setState(props);
    },
    _formatOption(value, key) {
        return (
            <option key={this.state.typeName + "-" + this.state.field + "-" + key}value={key} id={key}>{value}</option>
        );
    },
    handleChange(evt) {
        this.props.onChange(this.state.title, evt.target.value);
        this.setState({value: evt.target.value});
    },
    render() {
        var field = this.state.field;
        var options = $.map(field.additional_info.values, this._formatOption);
        var typeName = this.state.typeName;
        return (
            <div className="form-group">
                <label htmlFor={typeName + "-" + field.title}>
                    {field.human_name}

                    {FieldHelpers.optionalMarker(field)}
                </label>

                <select id={field.title} defaultValue={field.default_value} value={this.state.value}
                        className="input-xlarge form-control" onChange={this.handleChange}>
                    {options}
                </select>
                <p className="help-block">{field.description}</p>
            </div>
        );
    }
});

module.exports = DropdownField;
