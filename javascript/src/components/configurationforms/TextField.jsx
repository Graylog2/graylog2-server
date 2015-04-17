'use strict';

var React = require('react/addons');
var FieldHelpers = require('./FieldHelpers');

var TextField = React.createClass({
    _fieldValue(field) {
        return field.default_value;
    },
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
    render() {
        var field = this.state.field;
        var title = this.state.title;
        var typeName = this.state.typeName;

        var inputField;
        var value = this._fieldValue(field);
        var isRequired = !field.is_optional;
        var fieldType = (!FieldHelpers.hasAttribute(field.attributes, "textarea") && FieldHelpers.hasAttribute(field.attributes, "is_password") ? "password" : "text");

        if (FieldHelpers.hasAttribute(field.attributes, "textarea")) {
            inputField = (
                    <textarea id={typeName + "-" + title} className="textarea-xlarge form-control" name={"configuration["+title+"]"} required={isRequired}>
                        {value}
                    </textarea>
            );
        } else {
            inputField = (
                <input id={typeName + "-" + title} type={fieldType} className="input-xlarge form-control" name={"configuration["+title+"]"} required={isRequired} defaultValue={this._fieldValue(field)}/>
            );
        }

        return (
            <div className="form-group">
                <label htmlFor={typeName + "-" + title + ")"}>
                    {field.human_name}
                    {FieldHelpers.optionalMarker(field)}
                </label>
                {inputField}
                <p className="help-block">{field.description}</p>
            </div>
        );
    }
});

module.exports = TextField;
