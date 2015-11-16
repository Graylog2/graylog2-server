import $ from 'jquery';
import React from 'react';
import ReactDOM from 'react-dom';
import { Button, ButtonGroup, DropdownButton, Input, MenuItem, Modal } from 'react-bootstrap';
import numeral from 'numeral';

import Widget from 'components/widgets/Widget';
import SearchStore from 'stores/search/SearchStore';
import { SavedSearchControls, ShowQueryModal } from 'components/search';
import AddToDashboardMenu from 'components/dashboard/AddToDashboardMenu';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';

const MessageField = React.createClass({
  propTypes: {
    field: React.PropTypes.object,
    onFieldSelectedForGraph: React.PropTypes.func.isRequired,
    onFieldSelectedForQuickValues: React.PropTypes.func.isRequired,
    onFieldSelectedForStats: React.PropTypes.func.isRequired,
    onToggled: React.PropTypes.func,
    selected: React.PropTypes.bool,
  },
  getInitialState() {
    return {
      showActions: false,
    };
  },
  _toggleShowActions() {
    this.setState({showActions: !this.state.showActions});
  },
  render() {
    let toggleClassName = 'fa fa-fw open-analyze-field ';
    toggleClassName += this.state.showActions ? 'open-analyze-field-active fa-caret-down' : 'fa-caret-right';

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
            <ButtonGroup bsSize="xsmall">
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
  },
});

let resizeMutex;

const SearchSidebar = React.createClass({
  propTypes: {
    builtQuery: React.PropTypes.any,
    currentSavedSearch: React.PropTypes.string,
    fields: React.PropTypes.array,
    onFieldSelectedForGraph: React.PropTypes.func,
    onFieldSelectedForQuickValues: React.PropTypes.func,
    onFieldSelectedForStats: React.PropTypes.func,
    onFieldToggled: React.PropTypes.func,
    permissions: React.PropTypes.array,
    predefinedFieldSelection: React.PropTypes.func,
    result: React.PropTypes.object,
    searchInStream: React.PropTypes.object,
    selectedFields: React.PropTypes.object,
    shouldHighlight: React.PropTypes.bool,
    showAllFields: React.PropTypes.bool,
    showHighlightToggle: React.PropTypes.bool,
    togglePageFields: React.PropTypes.func,
    toggleShouldHighlight: React.PropTypes.func,
  },
  getInitialState() {
    return {
      fieldFilter: '',
      maxFieldsHeight: 1000,
    };
  },

  componentDidMount() {
    this._updateHeight();
    $(window).on('resize', this._resizeCallback);
    const $sidebarAffix = $('#sidebar-affix');
    $sidebarAffix.on('affixed.bs.affix', () => {
      $(window).off('scroll', this._updateHeight);
      this._updateHeight();
    });
    $sidebarAffix.on('affixed-top.bs.affix', () => {
      $(window).on('scroll', this._updateHeight);
      this._updateHeight();
    });
  },
  componentWillReceiveProps(newProps) {
    // update max-height of fields when we toggle per page/all fields
    if (this.props.showAllFields !== newProps.showAllFields) {
      this._updateHeight();
    }
  },
  componentWillUnmount() {
    $(window).off('resize', this._resizeCallback);
    const $sidebarAffix = $('#sidebar-affix');
    $sidebarAffix.off('affixed.bs.affix');
    $sidebarAffix.off('affixed-top.bs.affix');
  },
  _resizeCallback() {
    // Call resizedWindow() only at end of resize event so we do not trigger all the time while resizing.
    clearTimeout(resizeMutex);
    resizeMutex = setTimeout(() => this._updateHeight(), 100);
  },
  _updateHeight() {
    const header = ReactDOM.findDOMNode(this.refs.header);

    const footer = ReactDOM.findDOMNode(this.refs.footer);

    const sidebar = ReactDOM.findDOMNode(this.refs.sidebar);
    const sidebarTop = sidebar.getBoundingClientRect().top;
    const sidebarCss = window.getComputedStyle(ReactDOM.findDOMNode(this.refs.sidebar));
    const sidebarPaddingTop = parseFloat(sidebarCss.getPropertyValue('padding-top'));
    const sidebarPaddingBottom = parseFloat(sidebarCss.getPropertyValue('padding-bottom'));

    const viewPortHeight = window.innerHeight;
    const maxHeight =
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
  _showIndicesModal(event) {
    event.preventDefault();
    this.refs.indicesModal.open();
  },
  render() {
    const indicesModal = (
      <BootstrapModalWrapper ref="indicesModal">
        <Modal.Header closeButton>
          <Modal.Title>Used indices</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p>Graylog is intelligently selecting the indices it needs to search upon based on the time frame
            you selected.
            This list of indices is mainly useful for debugging purposes.</p>
          <h4>Indices used for this search:</h4>

          <ul className="index-list">
            {this.props.result.used_indices.map((index) => <li key={index.index_name}> {index.index_name}</li>)}
          </ul>
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={() => this.refs.indicesModal.close()}>Close</Button>
        </Modal.Footer>
      </BootstrapModalWrapper>
    );

    const messageFields = this.props.fields
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
    let searchTitle = null;
    const moreActions = [
      <MenuItem key="export" href={SearchStore.getCsvExportURL()}>Export as CSV</MenuItem>,
    ];
    if (this.props.searchInStream) {
      searchTitle = <span>{this.props.searchInStream.title}</span>;
      // TODO: add stream actions to dropdown
    } else {
      searchTitle = <span>Search result</span>;
    }

    // always add the debug query link as last elem
    moreActions.push(<MenuItem divider key="div2"/>);
    moreActions.push(<MenuItem key="showQuery" onSelect={() => this.refs.showQueryModal.open()}>Show query</MenuItem>);

    return (
      <div className="content-col" ref="sidebar">
        <div ref="header">
          <h2>
            {searchTitle}
          </h2>

          <p style={{marginTop: 3}}>
            Found <strong>{numeral(this.props.result.total_results).format('0,0')} messages</strong>&nbsp;
            in {numeral(this.props.result.time).format('0,0')} ms, searched in&nbsp;
            <a href="#" onClick={this._showIndicesModal}>
              {this.props.result.used_indices.length}&nbsp;{this.props.result.used_indices.length === 1 ? 'index' : 'indices'}
            </a>.
            {indicesModal}
          </p>

          <div className="actions">
            <AddToDashboardMenu title="Add count to dashboard"
                                widgetType={this.props.searchInStream ? Widget.Type.STREAM_SEARCH_RESULT_COUNT : Widget.Type.SEARCH_RESULT_COUNT}
                                permissions={this.props.permissions}/>

            <SavedSearchControls currentSavedSearch={this.props.currentSavedSearch}/>

            <div style={{display: 'inline-block'}}>
              <DropdownButton bsSize="small" title="More actions" id="search-more-actions-dropdown">
                {moreActions}
              </DropdownButton>
              <ShowQueryModal key="debugQuery" ref="showQueryModal" builtQuery={this.props.builtQuery} />
            </div>
          </div>

          <hr />


          <h3>Fields</h3>

          <div className="input-group input-group-sm" style={{marginTop: 5, marginBottom: 5}}>
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
        <div ref="fields" style={{maxHeight: this.state.maxFieldsHeight, overflowY: 'scroll'}}>
          <ul className="search-result-fields">
            {messageFields}
          </ul>
        </div>
        <div ref="footer">
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
  },
});

export default SearchSidebar;
