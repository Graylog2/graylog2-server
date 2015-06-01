'use strict';

var React = require('react/addons');
var Input = require('react-bootstrap').Input;
var Spinner = require('../common/Spinner');
var Col = require('react-bootstrap').Col;
var Immutable = require('immutable');

var InputDropdown = React.createClass({
    mixins: [React.addons.LinkedStateMixin],
    PLACEHOLDER: "placeholder",
    getInitialState() {
        return {
            selectedInput: this.PLACEHOLDER
        };
    },
    _formatInput(input) {
        return <option key={input.id} value={input.id}>{input.title} ({input.type})</option>;
    },
    _sortByTitle(input1, input2) {
        return input1.title.localeCompare(input2.title);
    },
    _onClick() {
        this.props.onClick(this.state.selectedInput);
    },
    render() {
        if (this.props.inputs) {
            var inputs = Immutable.List(this.props.inputs.sort(this._sortByTitle).map(this._formatInput));
            return (
                <div>
                        <Input type='select' style={{float: "left", width: "400px", marginRight: "10px"}} valueLink={this.linkState('selectedInput')} placeholder={this.PLACEHOLDER}>
                            <option value={this.PLACEHOLDER}>Select an input</option>
                            {inputs}
                        </Input>

                        <a className="btn btn-success" disabled={this.state.selectedInput === this.PLACEHOLDER} onClick={this._onClick}>{this.props.title}</a>
                </div>
            );
        } else {
            return <Spinner />;
        }
    }
});

module.exports = InputDropdown;
