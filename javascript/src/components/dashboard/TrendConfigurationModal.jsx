'use strict';

var React = require('react');
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
        var header = <h2 className="modal-title">Configure Dashboard Widget</h2>;
        var trendOptions = (
            <div>
                <div className="checkbox">
                    <label>
                        <input type="checkbox" onChange={this._onTrendChange} checked={this.state.trend}/> Display trend
                    </label>
                    <p className="help-block">Show trend information for this number.</p>
                </div>
                <div className="checkbox">
                    <label>
                        <input type="checkbox" onChange={this._onLowerIsBetterChange} disabled={!this.state.trend} checked={this.state.lowerIsBetter}/> Lower value is better
                    </label>
                    <p className="help-block">Use green colour when trend goes down.</p>
                </div>
            </div>
        );
        var body = (
            <fieldset>
                <div className="form-group">
                    <label>Description:</label>
                    <input type="text" className="form-control" onChange={this._onDescriptionChange} value={this.state.description} required/>
                </div>
                {this.state.supportsTrending ? trendOptions : null}
            </fieldset>
        );
        return (
            <BootstrapModal ref="modal" onCancel={this.closeModal} onConfirm={this._save} cancel="Cancel" confirm="Save">
               {header}
               {body}
            </BootstrapModal>
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
    _save() {
        this.resultCallback(this.state);
        this.closeModal();
    }

});

module.exports = TrendConfigurationModal;
