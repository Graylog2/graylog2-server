'use strict';

var $ = require('jquery'); // excluded and shimed

var React = require('react/addons');
var FieldHelpers = require('./FieldHelpers');

var DropdownField = React.createClass({
    getInitialState() {
        return {
            typeName: this.props.typeName,
            field: this.props.field,
            title: this.props.title
        };
    },
    componentWillReceiveProps(props) {
        this.setState(props);
    },
    _formatOption(value, key) {
        return (
            <option value={key} id={key}>{value}</option>
        );
    },
    render() {
        var field = this.state.field;
        var options = $.map(field.values, this._formatOption);
        return (
            <div className="form-group">
                <label htmlFor={typeName + "-" + field.title}>
                    {field.human_name}

                    {FieldHelpers.optionalMarker(field)}
                </label>

                <select id={typeName + "-" + field.title} defaultValue={field.default_value} className="input-xlarge form-control">
                    {options}
                </select>
                <p className="help-block">{field.description}</p>
            </div>
        );
    }
});

module.exports = DropdownField;
