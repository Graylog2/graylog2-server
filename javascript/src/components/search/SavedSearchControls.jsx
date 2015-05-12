'use strict';

var React = require('react');
var Button = require('react-bootstrap').Button;
var Input = require('react-bootstrap').Input;

var BootstrapModal = require('../bootstrap/BootstrapModal');

var SavedSearchesStore = require('../../stores/search/SavedSearchesStore');

var SavedSearchControls = React.createClass({
    _openModal() {
        this.refs['saveSearchModal'].open();
    },
    _hide() {
        this.refs['saveSearchModal'].close();
    },
    _save() {
        var promise = SavedSearchesStore.create(this.refs.title.getValue());
        promise.done(() => this._hide());
    },
    render() {
        return (
            <div style={{display: 'inline'}}>
                <Button bsStyle='success' bsSize='small' onClick={this._openModal}>Save search criteria</Button>
                <BootstrapModal ref="saveSearchModal"
                                onCancel={this._hide}
                                onConfirm={this._save}
                                cancel="Cancel"
                                confirm="Save">
                    <h2 className="modal-title">Save search criteria</h2>
                    <Input type="text"
                           label="Title"
                           ref="title"
                           required
                           defaultValue={this.props.title}
                           help="Type a name that describes the current search."/>
                </BootstrapModal>
            </div>
        );
    }
});

module.exports = SavedSearchControls;