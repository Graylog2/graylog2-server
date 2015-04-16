'use strict';

var $ = require('jquery'); // excluded and shimed

var React = require('react/addons');
var FieldHelpers = require('./FieldHelpers');

var BooleanField = React.createClass({
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
        var typeName = this.state.typeName;
        return (
            <div className="form-group">
                <div className="checkbox">
                    <label>
                        <input id={typeName + "-" + field.title}
                            type="checkbox"
                            checked={field.default_value}
                            name={"configuration[" + field.title + "]"} />

                            {field.human_name}

                            {FieldHelpers.optionalMarker(field)}
                    </label>
                </div>
                <p class="help-block">{field.description}</p>
            </div>
        );
    }
});

module.exports = BooleanField;
