/** @jsx React.DOM */

'use strict';

var React = require('react/addons');
var BootstrapModal = require('../bootstrap/BootstrapModal');

var TrendConfigurationModal = React.createClass({
    getInitialState() {
        return {description: "", trend: false, amount: 1, unit: "days"};
    },
    _onDescriptionChange(event) {
        this.setState({description: event.target.value});
    },
    _onTrendChange(event) {
        this.setState({trend: event.target.checked});
    },
    _onAmountChange(event) {
        this.setState({amount: event.target.value});
    },
    _onUnitChange(unit) {
        this.setState({unit: unit});
    },

    render() {
        var header = <h2>Configure Dashboard Widget</h2>;
        var body = (
            <fieldset>
                <label>Description:</label>
                <input type="text" onChange={this._onDescriptionChange} value={this.state.description}/>
                <label className="checkbox">
                    <input type="checkbox" onChange={this._onTrendChange} checked={this.state.trend}/> Display trend
                </label>
            </fieldset>
        );
        return (
            <form>
                <BootstrapModal ref="modal" onCancel={this.closeModal} onConfirm={this._save} cancel="Cancel" confirm="Save">
                   {header}
                   {body}
                </BootstrapModal>
            </form>
        );
    },
    closeModal() {
        this.refs.modal.close();
    },
    openModal(resultcallback) {
        this.setState(this.getInitialState());
        this.resultCallback = resultcallback;
        this.refs.modal.open();
    },
    _save: function () {
        this.resultCallback(this.state);
        this.closeModal();
    }

});

module.exports = TrendConfigurationModal;
