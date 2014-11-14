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
            <div>
                <label>Description:</label>
                <input onChange={this._onDescriptionChange} className="form-control" value={this.state.description} placeholder="Description"/>
                <label className="checkbox">
                    <input type="checkbox" onChange={this._onTrendChange} checked={this.state.trend}/> Display trend
                </label>
                <div className="btn-group" data-toggle="buttons-radio">
                    <input type="number" onChange={this._onAmountChange} className="form-control" value={this.state.amount} placeholder="x" disabled={!this.state.trend}/>&nbsp;
                    <button type="button" className="btn" onClick={this._onUnitChange.bind(this, "minutes")} disabled={!this.state.trend}>Minutes</button>
                    <button type="button" className="btn" onClick={this._onUnitChange.bind(this, "hours")} disabled={!this.state.trend}>Hours</button>
                    <button type="button" className="btn active" onClick={this._onUnitChange.bind(this, "days")} disabled={!this.state.trend}>Days</button>
                    <button type="button" className="btn" onClick={this._onUnitChange.bind(this, "months")} disabled={!this.state.trend}>Months</button>
                </div>
            </div>
        );
        return (
            <form role="form">
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
