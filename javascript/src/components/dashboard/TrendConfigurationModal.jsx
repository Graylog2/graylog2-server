/** @jsx React.DOM */

'use strict';

var React = require('react/addons');
var BootstrapModal = require('../bootstrap/BootstrapModal');

var TrendConfigurationModal = React.createClass({
    getInitialState() {
        return {description: "", trend: false, lowerIsBetter: false};
    },
    _onDescriptionChange(event) {
        this.setState({description: event.target.value});
    },
    _onTrendChange(event) {
        this.setState({trend: event.target.checked});
    },
    _onLowerIsBetterChange(event) {
        this.setState({lowerIsBetter: event.target.checked});
    },
    render() {
        var header = <h2>Configure Dashboard Widget</h2>;
        var trendOptions = (
            <div>
                <label className="checkbox">
                    <input type="checkbox" onChange={this._onTrendChange} checked={this.state.trend}/> Display trend
                </label>
                <label className="checkbox">
                    <input type="checkbox" onChange={this._onLowerIsBetterChange} disabled={!this.state.trend} checked={this.state.lowerIsBetter}/> Lower value is better
                    <span className="help-inline"> (use green colour when trend goes down)</span>
                </label>
            </div>
        );
        var body = (
            <fieldset>
                <label>Description:</label>
                <input type="text" onChange={this._onDescriptionChange} value={this.state.description}/>
                {this.state.supportsTrending ? trendOptions : null}
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
    openModal(resultcallback, supportsTrending, description) {
        var initialState = this.getInitialState();
        this.resultCallback = resultcallback;
        initialState.supportsTrending = supportsTrending;
        if (description) {
            initialState.description = description;
        }
        this.setState(initialState);
        this.refs.modal.open();
    },
    _save: function () {
        this.resultCallback(this.state);
        this.closeModal();
    }

});

module.exports = TrendConfigurationModal;
