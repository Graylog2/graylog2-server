'use strict';

var $ = require('jquery');

var React = require('react');
var Modal = require('react-bootstrap').Modal;
var ModalTrigger = require('react-bootstrap').ModalTrigger;
var ButtonGroup = require('react-bootstrap').ButtonGroup;
var Button = require('react-bootstrap').Button;
var Input = require('react-bootstrap').Input;
var DropdownButton = require('react-bootstrap').DropdownButton;
var MenuItem = require('react-bootstrap').MenuItem;
var OverlayTrigger = require('react-bootstrap').OverlayTrigger;
var Tooltip = require('react-bootstrap').Tooltip;
var ReactZeroClipboard = require('react-zeroclipboard');

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

var resizeMutex;

var SearchSidebar = React.createClass({
    getInitialState() {
        return {
            fieldFilter: "",
            maxFieldsHeight: 1000
        };
    },

    componentDidMount() {
        this._updateHeight();
        $(window).on('resize', this._resizeCallback);
        $('#sidebar-affix').on('affixed.bs.affix', this._updateHeight);
        $('#sidebar-affix').on('affixed-top.bs.affix', this._updateHeight);
    },
    componentWillUnmount() {
        $(window).off("resize", this._resizeCallback);
        $('#sidebar-affix').off('affixed.bs.affix', this._updateHeight);
        $('#sidebar-affix').off('affixed-top.bs.affix', this._updateHeight);
    },
    componentWillReceiveProps(newProps) {
        // update max-height of fields when we toggle per page/all fields
        if (this.props.showAllFields !== newProps.showAllFields) {
            this._updateHeight();
        }
    },
    _resizeCallback() {
        // Call resizedWindow() only at end of resize event so we do not trigger all the time while resizing.
        clearTimeout(resizeMutex);
        resizeMutex = setTimeout(() => this._updateHeight(), 100);
    },
    _updateHeight() {
        var header = React.findDOMNode(this.refs.header);

        var footer = React.findDOMNode(this.refs.footer);

        var sidebar = React.findDOMNode(this.refs.sidebar);
        var sidebarTop = sidebar.getBoundingClientRect().top;
        var sidebarCss = window.getComputedStyle(React.findDOMNode(this.refs.sidebar));
        var sidebarPaddingTop = parseFloat(sidebarCss['padding-top']);
        var sidebarPaddingBottom = parseFloat(sidebarCss['padding-bottom']);

        var viewPortHeight = window.innerHeight;
        var maxHeight =
            viewPortHeight -
            header.clientHeight - footer.clientHeight -
            sidebarTop - sidebarPaddingTop - sidebarPaddingBottom -
            35; // for good measure™

        this.setState({maxFieldsHeight: maxHeight});
    },

    _updateFieldSelection(setName) {
        this.props.predefinedFieldSelection(setName);
    },
    _showAllFields(event) {
        event.preventDefault();
        if (!this.props.showAllFields) {
            this.props.togglePageFields();
        }
    },
    _showPageFields(event) {
        event.preventDefault();
        if (this.props.showAllFields) {
            this.props.togglePageFields();
        }
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

        var queryText = JSON.stringify(JSON.parse(this.props.builtQuery), null, '  ');
        var queryModal =
            <Modal title='Elasticsearch Query' onRequestHide={() => {}}>
                <div className="modal-body">
                    <pre>{queryText}</pre>
                </div>
                <div className="modal-footer">
                    <OverlayTrigger
                        placement="top"
                        ref="copyBtnTooltip"
                        overlay={<Tooltip>Query copied to clipboard.</Tooltip>}>
                        <ReactZeroClipboard
                            text={queryText}
                            onAfterCopy={() => { this.refs['copyBtnTooltip'].toggle(); window.setTimeout(() => this.refs['copyBtnTooltip'] && this.refs['copyBtnTooltip'].toggle(), 1000); } }>
                            <Button>Copy query</Button>
                        </ReactZeroClipboard>
                    </OverlayTrigger>
                </div>
            </Modal>;
        var messageFields = this.props.fields
            .filter((field) => field.name.indexOf(this.state.fieldFilter) !== -1)
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
        var searchTitle = null;
        var moreActions = [
            <MenuItem key="export" href={SearchStore.getCsvExportURL()}>Export as CSV</MenuItem>
        ];
        if (this.props.searchInStream) {
            searchTitle = <span>{this.props.searchInStream.title}</span>;
            // TODO: add stream actions to dropdown
        } else {
            searchTitle = <span>Search result</span>;
        }

        // always add the debug query link as last elem
        moreActions.push(<MenuItem divider key="div2"/>);
        moreActions.push(<ModalTrigger key="debugQuery" modal={queryModal}>
            <MenuItem>Show query</MenuItem>
        </ModalTrigger>);

        return (
            <div className="content-col" ref='sidebar'>
                <div ref='header'>
                    <h2>
                        {searchTitle}
                    </h2>

                    <p style={{marginTop: 3}}>
                        Found <strong>{numeral(this.props.result['total_result_count']).format("0,0")} messages</strong>
                        in {numeral(this.props.result['took_ms']).format("0,0")} ms, searched in&nbsp;
                        <ModalTrigger modal={indicesModal}>
                            <a href="#" onClick={event => event.preventDefault()}>
                                {this.props.result['used_indices'].length}&nbsp;{this.props.result['used_indices'].length === 1 ? "index" : "indices"}
                            </a>
                        </ModalTrigger>.
                    </p>

                    <div style={{marginTop: 10}}>
                        <AddToDashboardMenu title="Add count to dashboard"
                                            widgetType={this.props.searchInStream ? Widget.Type.STREAM_SEARCH_RESULT_COUNT : Widget.Type.SEARCH_RESULT_COUNT}
                                            permissions={this.props.permissions}/>
                        &nbsp;
                        <SavedSearchControls currentSavedSearch={this.props.currentSavedSearch}/>

                        <DropdownButton bsSize="small" title="More actions">
                            {moreActions}
                        </DropdownButton>
                    </div>

                    <hr />


                    <h3>Fields</h3>

                    <div className="input-group input-group-sm">
                        <span className="input-group-btn">
                            <button type="button" className="btn btn-default"
                                    onClick={() => this._updateFieldSelection('default')}>Default
                            </button>
                            <button type="button" className="btn btn-default"
                                    onClick={() => this._updateFieldSelection('all')}>All
                            </button>
                            <button type="button" className="btn btn-default"
                                    onClick={() => this._updateFieldSelection('none')}>None
                            </button>
                        </span>
                        <input type="text" className="form-control" placeholder="Filter fields"
                               onChange={(event) => this.setState({fieldFilter: event.target.value})}
                               value={this.state.fieldFilter}/>
                    </div>
                </div>
                <div ref='fields' style={{maxHeight: this.state.maxFieldsHeight, overflowY: 'scroll'}}>
                    <ul className="search-result-fields">
                        {messageFields}
                    </ul>
                </div>
                <div ref='footer'>
                    <p style={{marginTop: 13, marginBottom: 0}}>
                        List <span className="message-result-fields-range"> fields of&nbsp;
                        <a href="#" style={{fontWeight: this.props.showAllFields ? 'normal' : 'bold'}}
                           onClick={this._showPageFields}>current page</a> or <a href="#"
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
            </div>
        );
    }
});

module.exports = SearchSidebar;