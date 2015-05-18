'use strict';

var React = require('react/addons');
var InputsStore = require('../../stores/inputs/InputsStore');
var Input = require('react-bootstrap').Input;
var Spinner = require('../common/Spinner');

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
            var inputs = this.props.inputs.sort(this._sortByTitle).map(this._formatInput);
            return (
                <div className="col-md-12">
                    <div className="col-md-6">
                        <Input type='select' valueLink={this.linkState('selectedInput')} placeholder={this.PLACEHOLDER}>
                            <option value={this.PLACEHOLDER}>--- Select an Input ---</option>
                            {inputs}
                        </Input>
                    </div>
                    <div className="colo-md-6">
                        <a className="btn btn-success" disabled={this.state.selectedInput === this.PLACEHOLDER} onClick={this._onClick}>{this.props.title}</a>
                    </div>
                </div>
            );
        } else {
            return <Spinner />
        }
    }
});

module.exports = InputDropdown;
