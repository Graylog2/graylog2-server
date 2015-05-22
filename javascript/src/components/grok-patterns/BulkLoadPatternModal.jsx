'use strict';

var React = require('react/addons');
//noinspection JSUnusedGlobalSymbols
var BootstrapModal = require('../bootstrap/BootstrapModal');
var URLUtils = require("../../util/URLUtils");

var BulkLoadPatternModal = React.createClass({
    getInitialState() {
        return {};
    },
    render() {
        var header = <h2 className="modal-title">Import Grok patterns from file</h2>;
        var body = (
            <div>
                <div className="form-group">
                    <label htmlFor="pattern-file">Pattern file</label>
                    <input id="pattern-file" name="patterns" type="file" required/>
                    <span className="help-block">A file containing Grok patterns, one per line. Name and patterns should be separated by whitespace.</span>
                </div>
                <div className="checkbox">
                    <label>
                        <input type="checkbox" name="replace"/> Replace all existing patterns&#63;
                    </label>
                </div>
            </div>
        );
        return (
            <span>
                <button className="btn btn-info" style={{marginRight: 5}} onClick={this.openModal}>Import pattern file</button>

                <BootstrapModal
                    ref="modal"
                    confirm="Upload"
                    onCancel={this._closeModal}
                    cancel="Cancel"
                    method="POST"
                    encType="multipart/form-data"
                    action={URLUtils.appPrefixed('/system/grokpatterns/import')}>
                   {header}
                   {body}
                </BootstrapModal>
            </span>
        );
    },
    _closeModal() {
        this.refs.modal.close();
    },
    openModal() {
        this.refs.modal.open();
    }
});

module.exports = BulkLoadPatternModal;
