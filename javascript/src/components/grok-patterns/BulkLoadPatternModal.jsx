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
        var header = <h2>Import Grok patterns from file</h2>;
        var body = (
            <div>
                <span className="help-block">A file containing Grok patterns, one per line. Name and patterns should be separated by whitespace.</span>
                <input type="file" name="patterns" required/>
            </div>
        );
        return (
            <span>
                <button className="btn btn-small btn-success" style={{marginRight: 5}} onClick={this.openModal}><i className="icon icon-file"></i> Import pattern file</button>

                <BootstrapModal 
                    ref="modal" 
                    onConfirm={this.uploadPatterns} 
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
    },
    uploadPatterns(event) {
        event.target.submit();
        this._closeModal();
    }

});

module.exports = BulkLoadPatternModal;
