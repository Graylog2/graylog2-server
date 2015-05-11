'use strict';

var React = require('react/addons');
var InputsStore = require('../../stores/inputs/InputsStore');
var Input = require('react-bootstrap').Input;

var InputDropdown = React.createClass({
    mixins: [React.addons.LinkedStateMixin],
    PLACEHOLDER: "placeholder",
    getInitialState() {
        return {
            inputs: [],
            selectedInput: this.PLACEHOLDER
        };
    },
    loadData() {
        InputsStore.list((inputs) => {
            this.setState({inputs: inputs});
        });
    },
    componentDidMount() {
        if (!this.props.inputs) {
            this.loadData();
        } else {
            this.setState({inputs: this.props.inputs});
        }
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
        var inputs = this.state.inputs.sort(this._sortByTitle).map(this._formatInput);
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
    }
});

module.exports = InputDropdown;
