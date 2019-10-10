import PropTypes from 'prop-types';
import React from 'react';
import Immutable from 'immutable';

import { Button, ButtonGroup } from 'components/graylog';
import { Icon } from 'components/common';
import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';
import { MessageTableEntry, MessageTablePaginator } from 'components/search';

const StreamsStore = StoreProvider.getStore('Streams');
const SearchStore = StoreProvider.getStore('Search');
const RefreshActions = ActionsProvider.getActions('Refresh');

class ResultTable extends React.Component {
  static propTypes = {
    disableSurroundingSearch: PropTypes.bool,
    highlight: PropTypes.bool.isRequired,
    inputs: PropTypes.object.isRequired,
    messages: PropTypes.array.isRequired,
    nodes: PropTypes.object.isRequired,
    onPageChange: PropTypes.func,
    page: PropTypes.number.isRequired,
    pageSize: PropTypes.number,
    resultCount: PropTypes.number.isRequired,
    selectedFields: PropTypes.object.isRequired,
    sortField: PropTypes.string.isRequired,
    sortOrder: PropTypes.string.isRequired,
    streams: PropTypes.object.isRequired,
    searchConfig: PropTypes.object.isRequired,
  };

  static defaultProps = {
    disableSurroundingSearch: false,
    onPageChange: (page) => { SearchStore.page = page; },
  };

  state = {
    expandedMessages: Immutable.Set(),
    allStreamsLoaded: false,
    allStreams: Immutable.List(),
    expandAllRenderAsync: false,
  };

  componentDidMount() {
    // only load the streams per page
    if (this.state.allStreamsLoaded) {
      return;
    }
    const promise = StreamsStore.listStreams();
    promise.done(streams => this._onStreamsLoaded(streams));
  }

  componentDidUpdate() {
    if (this.state.expandAllRenderAsync) {
      // This may take some time, so we ensure we display a loading indicator in the page
      // while all messages are being expanded
      setTimeout(() => this.setState({ expandAllRenderAsync: false }), this.EXPAND_ALL_RENDER_ASYNC_DELAY);
    }
  }

  EXPAND_ALL_RENDER_ASYNC_DELAY = 10;

  _onStreamsLoaded = (streams) => {
    this.setState({ allStreamsLoaded: true, allStreams: Immutable.List(streams).sortBy(stream => stream.title) });
  };

  _toggleMessageDetail = (id) => {
    let newSet;
    if (this.state.expandedMessages.contains(id)) {
      newSet = this.state.expandedMessages.delete(id);
    } else {
      newSet = this.state.expandedMessages.add(id);
      RefreshActions.disable();
    }
    this.setState({ expandedMessages: newSet });
  };

  _fieldColumns = () => {
    return this.props.selectedFields.delete('message');
  };

  _columnStyle = (fieldName) => {
    if (fieldName.toLowerCase() === 'source' && this._fieldColumns().size > 1) {
      return { width: 180 };
    }
    return {};
  };

  expandAll = () => {
    // If more than 30% of the messages are being expanded, show a loading indicator
    const expandedChangeRatio = (this.props.messages.length - this.state.expandedMessages.size) / 100;
    const renderLoadingIndicator = expandedChangeRatio > 0.3;

    const newSet = Immutable.Set(this.props.messages.map(message => `${message.index}-${message.id}`));
    this.setState({ expandedMessages: newSet, expandAllRenderAsync: renderLoadingIndicator });
  };

  collapseAll = () => {
    this.setState({ expandedMessages: Immutable.Set() });
  };

  _handleSort = (e, field, order) => {
    e.preventDefault();
    SearchStore.sort(field, order);
  };

  _sortIcons = (fieldName) => {
    let sortLinks = null;
    // the classes look funny, but using the default asc/desc icons looks odd.
    // .sort-order-item does the margins
    // .sort-order-desc does the top: offset for the flipped icon
    // .sort-order-active indicates the currently active sort order
    const classesAsc = 'sort-order-item';
    const classesDesc = 'sort-order-desc sort-order-item';
    // if the given field name is the one we sorted on
    if (this.props.sortField.toLowerCase().localeCompare(fieldName.toLowerCase()) === 0) {
      if (this.props.sortOrder.toLowerCase().localeCompare('desc') === 0) {
        sortLinks = (
          <span>
            <Icon name="sort-amount-asc" className={`${classesDesc} sort-order-active`} flip="vertical" />
            <a href="#" onClick={e => this._handleSort(e, fieldName, 'asc')}>
              <Icon name="sort-amount-asc" className={classesAsc} />
            </a>
          </span>
        );
      } else {
        sortLinks = (
          <span>
            <Icon name="sort-amount-asc" className={`${classesAsc} sort-order-active`} />
            <a href="#" onClick={e => this._handleSort(e, fieldName, 'desc')}>
              <Icon name="sort-amount-asc" className={classesDesc} flip="vertical" />
            </a>
          </span>
        );
      }
    } else {
      // the given fieldname is not being sorted on
      sortLinks = (
        <span className="sort-order">
          <a href="#" onClick={e => this._handleSort(e, fieldName, 'asc')}>
            <Icon name="sort-amount-asc" className={classesAsc} />
          </a>
          <a href="#" onClick={e => this._handleSort(e, fieldName, 'desc')}>
            <Icon name="sort-amount-asc" className={classesDesc} flip="vertical" />
          </a>
        </span>
      );
    }
    return <span>{sortLinks}</span>;
  };

  render() {
    const selectedColumns = this._fieldColumns();
    return (
      <div className="content-col">
        <h1 className="pull-left">Messages</h1>

        <ButtonGroup bsSize="small" className="pull-right">
          <Button title="Expand all messages" onClick={this.expandAll}><Icon name="expand" /></Button>
          <Button title="Collapse all messages"
                  onClick={this.collapseAll}
                  disabled={this.state.expandedMessages.size === 0}><Icon name="compress" />
          </Button>
        </ButtonGroup>

        <MessageTablePaginator currentPage={Number(this.props.page)}
                               onPageChange={this.props.onPageChange}
                               pageSize={this.props.pageSize}
                               position="top"
                               resultCount={this.props.resultCount} />

        <div className="search-results-table">
          <div className="table-responsive">
            <div className="messages-container">
              <table className="table table-condensed messages">
                <thead>
                  <tr>
                    <th style={{ width: 180 }}>Timestamp {this._sortIcons('timestamp')}</th>
                    {selectedColumns.toSeq().map((selectedFieldName) => {
                      return (
                        <th key={selectedFieldName}
                            style={this._columnStyle(selectedFieldName)}>
                          {selectedFieldName} {this._sortIcons(selectedFieldName)}
                        </th>
                      );
                    })}
                  </tr>
                </thead>
                {this.props.messages.map((message) => {
                  return (
                    <MessageTableEntry key={`${message.index}-${message.id}`}
                                       disableSurroundingSearch={this.props.disableSurroundingSearch}
                                       message={message}
                                       showMessageRow={this.props.selectedFields.contains('message')}
                                       selectedFields={selectedColumns}
                                       expanded={this.state.expandedMessages.contains(`${message.index}-${message.id}`)}
                                       toggleDetail={this._toggleMessageDetail}
                                       inputs={this.props.inputs}
                                       streams={this.props.streams}
                                       allStreams={this.state.allStreams}
                                       allStreamsLoaded={this.state.allStreamsLoaded}
                                       nodes={this.props.nodes}
                                       highlight={this.props.highlight}
                                       highlightMessage={SearchStore.highlightMessage}
                                       expandAllRenderAsync={this.state.expandAllRenderAsync}
                                       searchConfig={this.props.searchConfig} />
                  );
                })}
              </table>
            </div>
          </div>
        </div>

        <MessageTablePaginator currentPage={Number(this.props.page)}
                               onPageChange={this.props.onPageChange}
                               pageSize={this.props.pageSize}
                               position="bottom"
                               resultCount={this.props.resultCount}>
          <ButtonGroup bsSize="small" className="pull-right" style={{ position: 'absolute', marginTop: 20, right: 10 }}>
            <Button title="Expand all messages" onClick={this.expandAll}><Icon name="expand" /></Button>
            <Button title="Collapse all messages"
                    onClick={this.collapseAll}
                    disabled={this.state.expandedMessages.size === 0}><Icon name="compress" />
            </Button>
          </ButtonGroup>
        </MessageTablePaginator>
      </div>
    );
  }
}

export default ResultTable;
