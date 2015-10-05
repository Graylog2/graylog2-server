'use strict';

var React = require('react');
var Input = require('react-bootstrap').Input;

var BootstrapModalForm = require('../bootstrap/BootstrapModalForm');
var URLUtils = require("../../util/URLUtils");

var BulkLoadPatternModal = React.createClass({
    render() {
        return (
            <span>
                <button className="btn btn-info" style={{marginRight: 5}} onClick={() => this.refs.modal.open()}>Import pattern file</button>

                <BootstrapModalForm ref="modal"
                                    title="Import Grok patterns from file"
                                    submitButtonText="Upload"
                                    formProps={{method: 'POST', encType: 'multipart/form-data', action: URLUtils.appPrefixed('/system/grokpatterns/import')}}>
                    <Input type="file"
                           id="pattern-file"
                           name="patterns"
                           label="Pattern file"
                           help="A file containing Grok patterns, one per line. Name and patterns should be separated by whitespace."
                           required />
                    <Input type="checkbox"
                           id="replace-patterns"
                           name="replace"
                           label="Replace all existing patterns?" />
                </BootstrapModalForm>
            </span>
        );
    },
});

module.exports = BulkLoadPatternModal;
