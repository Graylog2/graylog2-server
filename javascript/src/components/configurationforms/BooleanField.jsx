'use strict';

var React = require('react/addons');
var FieldHelpers = require('./FieldHelpers');

var BooleanField = React.createClass({
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
    handleChange(evt) {
        this.props.onChange(this.state.title, evt.target.value);
    },
    render() {
        var field = this.state.field;
        var typeName = this.state.typeName;
        return (
            <div className="form-group">
                <div className="checkbox">
                    <label>
                        <input id={typeName + "-" + field.title}
                            type="checkbox"
                            checked={field.default_value}
                            name={"configuration[" + field.title + "]"}
                            onChange={this.handleChange} />

                            {field.human_name}

                            {FieldHelpers.optionalMarker(field)}
                    </label>
                </div>
                <p className="help-block">{field.description}</p>
            </div>
        );
    }
});

module.exports = BooleanField;
