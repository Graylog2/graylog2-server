'use strict';

var React = require('react/addons');
var FieldHelpers = require('./FieldHelpers');

var BooleanField = React.createClass({
    getInitialState() {
        return {
            typeName: this.props.typeName,
            field: this.props.field,
            title: this.props.title,
            value: (this.props.value === undefined ? this.props.field.default_value : this.props.value)
        };
    },
    componentWillReceiveProps(props) {
        this.setState(props);
    },
    handleChange(evt) {
        var newValue = !this.state.value;
        this.setState({value: newValue});
        this.props.onChange(this.state.title, newValue);
    },
    render() {
        var field = this.state.field;
        var typeName = this.state.typeName;
        var value = this.state.value;
        return (
            <div className="form-group">
                <div className="checkbox">
                    <label>
                        <input id={typeName + "-" + field.title}
                            type="checkbox"
                            checked={value}
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
