'use strict';

var React = require('react/addons');
var FieldHelpers = require('./FieldHelpers');

var NumberField = React.createClass({
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
    mapValidationAttribute(attribute) {
        switch(attribute) {
            case "ONLY_NEGATIVE": return "negative_number";
            case "ONLY_POSITIVE": return "positive_number";
            case "IS_PORT_NUMBER": return "port_number";
            default: return attribute.toLowerCase();
        }
    },
    validationSpec(field) {
        return field.attributes.map(this.mapValidationAttribute).join(" ");
    },
    render() {
        var typeName = this.state.typeName;
        var field = this.state.field;
        var isRequired = !field.is_optional;
        var validationSpecs = this.validationSpec(field);

        return (
            <div className="form-group">
                <label htmlFor={typeName + "-" + field.title}>
                    {field.human_name}
                    {FieldHelpers.optionalMarker(field)}
                </label>
                <input id={typeName + "-" + field.title} type="number" required={isRequired} min={Number.MIN_VALUE.toFixed()} max={Number.MAX_VALUE.toFixed()}
                    className="input-xlarge validatable form-control" data-validate={validationSpecs} />
                <p className="help-block">{field.description}</p>
            </div>
        );
    }
});

module.exports = NumberField;
