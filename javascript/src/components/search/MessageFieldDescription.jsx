/* global jsRoutes */

'use strict';

var $ = require('jquery');

var React = require('react');
var SplitButton = require('react-bootstrap').SplitButton;
var Alert = require('react-bootstrap').Alert;
var MenuItem = require('react-bootstrap').MenuItem;

var Immutable = require('immutable');

var MessagesStore = require('../../stores/messages/MessagesStore');

var MessageFieldDescription = React.createClass({
    getInitialState() {
        return {
            messageTerms: Immutable.List()
        };
    },
    _loadTerms(field) {
        return () => {
            var promise = MessagesStore.fieldTerms(this.props.message.index, this.props.message.id, field);
            promise.done((terms) => this._onTermsLoaded(terms));
        };
    },
    _onTermsLoaded(terms) {
        this.setState({messageTerms: Immutable.fromJS(terms)});
    },
    _shouldShowTerms() {
        return this.state.messageTerms.size !== 0;
    },
    _getNewExtractorRoute(type) {
        return jsRoutes.controllers.ExtractorsController.newExtractor(
            this.props.message['source_node_id'],
            this.props.message['source_input_id'],
            type,
            this.props.fieldName,
            this.props.message.index,
            this.props.message.id
        ).url;
    },
    _addFieldToSearchBar(event) {
        event.preventDefault();
        $(document).trigger('add-search-term.graylog.search', {field: this.props.fieldName, value: this.props.fieldValue});
    },
    _getFormattedTerms() {
        var termsMarkup = [];
        this.state.messageTerms.forEach((term) => termsMarkup.push(<span key={term} className="message-terms">{term}</span>));

        return termsMarkup;
    },
    render() {
        var fieldActions = (this.props.disableFieldActions ? null : <div className="message-field-actions pull-right">
            <SplitButton pullRight={true} bsSize="xsmall" title={<i className="fa fa-search-plus"></i>} key={1} onClick={this._addFieldToSearchBar}>
                <li className="dropdown-submenu left-submenu">
                    <a href="#">Create extractor for field {this.props.fieldName}</a>
                    <ul className="dropdown-menu">
                        <li><a href={this._getNewExtractorRoute("regex")}>Regular expression</a>
                        </li>
                        <li><a href={this._getNewExtractorRoute("substring")}>Substring</a></li>
                        <li><a href={this._getNewExtractorRoute("split_and_index")}>Split &amp; Index</a></li>
                        <li><a href={this._getNewExtractorRoute("copy_input")}>Copy Input</a></li>
                        <li><a href={this._getNewExtractorRoute("grok")}>Grok pattern</a></li>
                    </ul>
                </li>
                <MenuItem onClick={this._loadTerms(this.props.fieldName)}>Show terms of {this.props.fieldName}</MenuItem>
            </SplitButton>
        </div>);

        return (
            <dd key={this.props.fieldName + "dd"}>
                {fieldActions}
                {this.props.possiblyHighlight(this.props.fieldName)}
                {this._shouldShowTerms() && <br />}
                {this._shouldShowTerms() && <Alert bsStyle='info' onDismiss={() => this.setState({messageTerms: Immutable.Map()})}>Field terms: {this._getFormattedTerms()}</Alert>}
            </dd>
        );
    }
});


module.exports = MessageFieldDescription;