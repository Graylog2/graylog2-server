'use strict';

var React = require('react');
var Button = require('react-bootstrap').Button;
var DropdownButton = require('react-bootstrap').DropdownButton;
var MenuItem = require('react-bootstrap').MenuItem;
var Input = require('react-bootstrap').Input;

var BootstrapModal = require('../bootstrap/BootstrapModal');

var SavedSearchesStore = require('../../stores/search/SavedSearchesStore');

var SavedSearchControls = React.createClass({
    getInitialState() {
        return {
            title: ""
        };
    },
    _isSearchSaved() {
        return this.props.currentSavedSearch !== undefined;
    },
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
    _getNewSavedSearchButtons() {
        return <Button bsStyle='success' bsSize='small' onClick={this._openModal}>Save search criteria</Button>;
    },
    _getEditSavedSearchControls() {
        return (
            <DropdownButton bsSize='small' title='Saved search'>
                <MenuItem onClick={this._openModal}>Update search criteria</MenuItem>
                <MenuItem divider/>
                <MenuItem onClick={this._deleteSavedSearch}>Delete saved search</MenuItem>
            </DropdownButton>
        );
    },
    render() {
        return (
            <div style={{display: 'inline'}}>
                {this._isSearchSaved() ? this._getEditSavedSearchControls() : this._getNewSavedSearchButtons()}
                <BootstrapModal ref="saveSearchModal"
                                onCancel={this._hide}
                                onConfirm={this._save}
                                cancel="Cancel"
                                confirm="Save">
                    <h2 className="modal-title">{this._isSearchSaved() ? 'Update search criteria' : 'Save search criteria'}</h2>
                    <Input type="text"
                           label="Title"
                           ref="title"
                           required
                           defaultValue={this.state.title}
                           help="Type a name that describes the current search."/>
                </BootstrapModal>
            </div>
        );
    }
});

module.exports = SavedSearchControls;