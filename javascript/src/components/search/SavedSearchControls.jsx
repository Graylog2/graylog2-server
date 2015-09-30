'use strict';

var React = require('react');
var Button = require('react-bootstrap').Button;
var DropdownButton = require('react-bootstrap').DropdownButton;
var MenuItem = require('react-bootstrap').MenuItem;
var Input = require('react-bootstrap').Input;

var BootstrapModalForm = require('../bootstrap/BootstrapModalForm');

var SavedSearchesStore = require('../../stores/search/SavedSearchesStore');

var SavedSearchControls = React.createClass({
    getInitialState() {
        return {
            title: "",
            error: false
        };
    },
    componentDidMount() {
        if (this._isSearchSaved()) {
            SavedSearchesStore.addOnSavedSearchesChangedListener(this._updateTitle);
        }
    },
    _isSearchSaved() {
        return this.props.currentSavedSearch !== undefined;
    },
    _updateTitle(newSavedSearches) {
        var currentSavedSearch = SavedSearchesStore.getSavedSearch(this.props.currentSavedSearch);
        if (currentSavedSearch !== undefined) {
            this.setState({title: currentSavedSearch.title});
        }
    },
    _openModal() {
        this.refs['saveSearchModal'].open();
    },
    _hide() {
        this.refs['saveSearchModal'].close();
    },
    _save() {
        if (this.state.error) {
            return;
        }

        var promise;
        if (this._isSearchSaved()) {
            promise = SavedSearchesStore.update(this.props.currentSavedSearch, this.refs.title.getValue());
        } else {
            promise = SavedSearchesStore.create(this.refs.title.getValue());
        }
        promise.done(() => this._hide());
    },
    _deleteSavedSearch(e) {
        e.preventDefault();
        if (window.confirm('Do you really want to delete this saved search?')) {
            SavedSearchesStore.delete(this.props.currentSavedSearch);
        }
    },
    _titleChanged(e) {
        this.setState({error: !SavedSearchesStore.isValidTitle(this.props.currentSavedSearch, this.refs.title.getValue())});
    },
    _getNewSavedSearchButtons() {
        return <Button bsStyle='success' bsSize='small' onClick={this._openModal}>Save search criteria</Button>;
    },
    _getEditSavedSearchControls() {
        return (
            <DropdownButton bsSize='small' title='Saved search' id="saved-search-actions-dropdown">
                <MenuItem onSelect={this._openModal}>Update search criteria</MenuItem>
                <MenuItem divider/>
                <MenuItem onSelect={this._deleteSavedSearch}>Delete saved search</MenuItem>
            </DropdownButton>
        );
    },
    render() {
        return (
            <div style={{display: 'inline-block'}}>
                {this._isSearchSaved() ? this._getEditSavedSearchControls() : this._getNewSavedSearchButtons()}
                <BootstrapModalForm ref="saveSearchModal"
                                    title={this._isSearchSaved() ? 'Update saved search' : 'Save search criteria'}
                                    onSubmitForm={this._save}
                                    submitButtonText="Save">
                    <Input type="text"
                           label="Title"
                           ref="title"
                           required
                           defaultValue={this.state.title}
                           onChange={this._titleChanged}
                           bsStyle={this.state.error ? 'error' : null}
                           help={this.state.error ? 'Title was already taken.' : 'Type a name describing the current search.'}
                           autoFocus />
                </BootstrapModalForm>
            </div>
        );
    }
});

module.exports = SavedSearchControls;