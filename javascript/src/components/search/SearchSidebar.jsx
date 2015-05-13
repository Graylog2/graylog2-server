'use strict';

var React = require('react');
var Modal = require('react-bootstrap').Modal;
var ModalTrigger = require('react-bootstrap').ModalTrigger;
var ButtonGroup = require('react-bootstrap').ButtonGroup;
var Button = require('react-bootstrap').Button;
var Input = require('react-bootstrap').Input;

var Widget = require('../widgets/Widget');
var SearchStore = require('../../stores/search/SearchStore');
var SavedSearchControls = require('./SavedSearchControls');
var AddToDashboardMenu = require('../dashboard/AddToDashboardMenu');

var numeral = require('numeral');

var MessageField = React.createClass({
    getInitialState() {
        return {
            showActions: false
        };
    },
    _toggleShowActions() {
        this.setState({showActions: !this.state.showActions});
    },
    render() {
        var toggleClassName = "fa fa-fw open-analyze-field ";
        toggleClassName += this.state.showActions ? "open-analyze-field-active fa-caret-down" : "fa-caret-right";

        return (
            <li>
                <div className="pull-left">
                    <i className={toggleClassName}
                       onClick={this._toggleShowActions}></i>
                </div>
                <div style={{marginLeft: 25}}>
                    <Input type="checkbox"
                           label={this.props.field.name}
                           checked={this.props.selected}
                           onChange={() => this.props.onToggled(this.props.field.name)}/>

                    {this.state.showActions &&
                    <div className="analyze-field">
                        <ButtonGroup bsSize='xsmall'>
                            <Button onClick={() => this.props.onFieldSelectedForStats(this.props.field.name)}>
                                Statistics
                            </Button>
                            <Button onClick={() => this.props.onFieldSelectedForQuickValues(this.props.field.name)}>
                                Quick values
                            </Button>
                            <Button onClick={() => this.props.onFieldSelectedForGraph(this.props.field.name)}>
                                Generate chart
                            </Button>
                        </ButtonGroup>
                    </div>}
                </div>
            </li>
        );
    }
});

var SearchSidebar = React.createClass({
    _updateFieldSelection(event, setName) {
        this.props.predefinedFieldSelection(setName);
        event.preventDefault();
    },
    _showAllFields(event) {
        if (!this.props.showAllFields) {
            this.props.togglePageFields();
        }
        event.preventDefault();
    },
    _showPageFields(event) {
        if (this.props.showAllFields) {
            this.props.togglePageFields();
        }
        event.preventDefault();
    },
    render() {
        var indicesModal =
            <Modal title='Used Indices' onRequestHide={() => {}}>
                <div className="modal-body">
                    <p>Graylog is intelligently selecting the indices it needs to search upon based on the time frame
                        you selected.
                        This list of indices is mainly useful for debugging purposes.</p>
                    <h4>Indices used for this search:</h4>

                    <ul className="index-list">
                        {this.props.result['used_indices'].map((index) => <li key={index.index}> {index.index}</li>)}
                    </ul>
                </div>
            </Modal>;

        var messageFields = this.props.fields
            .sort((a, b) => a.name.localeCompare(b.name))
            .map((field) => {
                return (
                    <MessageField key={field.name}
                                  field={field}
                                  onToggled={this.props.onFieldToggled}
                                  onFieldSelectedForGraph={this.props.onFieldSelectedForGraph}
                                  onFieldSelectedForQuickValues={this.props.onFieldSelectedForQuickValues}
                                  onFieldSelectedForStats={this.props.onFieldSelectedForStats}
                                  selected={this.props.selectedFields.contains(field.name)}/>
                );
            });

        return (
            <div className="content-col">
                <h3>Found {numeral(this.props.result['total_result_count']).format("0,0")} messages</h3>

                <p style={{marginTop: 3}}>
                    Search took {numeral(this.props.result['took_ms']).format("0,0")} ms, searched in <ModalTrigger
                    modal={indicesModal}><a href="#"
                                            onClick={event => event.preventDefault()}>{this.props.result['used_indices'].length}&nbsp;{this.props.result['used_indices'].length === 1 ? "index" : "indices"}</a></ModalTrigger>.
                </p>

                <div style={{marginTop: 10}}>
                    <AddToDashboardMenu title="Add count to dashboard"
                                        widgetType={this.props.searchInStreamId ? Widget.Type.STREAM_SEARCH_RESULT_COUNT : Widget.Type.SEARCH_RESULT_COUNT}
                                        dashboards={this.props.dashboards}/>
                    &nbsp;
                    <SavedSearchControls currentSavedSearch={this.props.currentSavedSearch}/>
                    <a href={SearchStore.getCsvExportURL()} className="btn btn-default btn-sm">Export as CSV</a>
                </div>

                <hr />

                <h1 style={{display: 'inline-block'}}>Fields</h1>
                <a href="#" className="fields-set-chooser"
                   onClick={(event) => this._updateFieldSelection(event, 'default')}>Default</a>
                |
                <a href="#" className="fields-set-chooser"
                   onClick={(event) => this._updateFieldSelection(event, 'all')}>All</a>
                |
                <a href="#" className="fields-set-chooser"
                   onClick={(event) => this._updateFieldSelection(event, 'none')}>None</a>

                <ul className="search-result-fields">
                    {messageFields}
                </ul>

                <p style={{marginTop: 13, marginBottom: 0}}>
                    List <span className="message-result-fields-range">
                <a href="#" style={{fontWeight: this.props.showAllFields ? 'normal' : 'bold'}}
                   onClick={this._showPageFields}>fields of current page</a> or <a href="#"
                                                                                   style={{fontWeight: this.props.showAllFields ? 'bold' : 'normal'}}
                                                                                   onClick={this._showAllFields}>all
                    fields</a>.
                </span>
                    <br/>
                    { this.props.showHighlightToggle &&
                    <Input type="checkbox" bsSize="small" checked={this.props.shouldHighlight}
                           onChange={this.props.toggleShouldHighlight} label="Highlight results"
                           groupClassName="result-highlight-control"/>
                    }
                </p>
            </div>
        );
    }
});

module.exports = SearchSidebar;