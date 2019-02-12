import PropTypes from 'prop-types';
import React from 'react';

import { Row, Col, Alert } from 'components/graylog';
import { IfPermitted, TypeAheadDataFilter, Icon, PaginatedList, SearchForm } from 'components/common';

import StoreProvider from 'injection/StoreProvider';
import Spinner from 'components/common/Spinner';
import StreamList from './StreamList';

const StreamsStore = StoreProvider.getStore('Streams');
const StreamRulesStore = StoreProvider.getStore('StreamRules');

class StreamComponent extends React.Component {
  static propTypes = {
    currentUser: PropTypes.object.isRequired,
    onStreamSave: PropTypes.func.isRequired,
    indexSets: PropTypes.array.isRequired,
  };

  state = {
    pagination: {
      page: 1,
      perPage: 10,
      count: 0,
      total: 0,
      query: '',
    },
  };

  componentDidMount() {
    this.loadData();
    StreamRulesStore.types().then((types) => {
      this.setState({ streamRuleTypes: types });
    });
    StreamsStore.onChange(this.loadData);
    StreamRulesStore.onChange(this.loadData);
  }

  componentWillUnmount() {
    StreamsStore.unregister(this.loadData);
    StreamRulesStore.unregister(this.loadData);
  }

  loadData = (callback) => {
    const { page, perPage, query } = this.state.pagination;
    StreamsStore.searchPaginated(page, perPage, query)
      .then(({ streams, pagination }) => {
        this.setState({
          streams: streams,
          pagination: pagination,
        });
      })
      .then(() => {
        if (callback) {
          callback();
        }
      });
  };

  _isLoading = () => {
    return !(this.state.streams && this.state.streamRuleTypes);
  };

  _onPageChange = (newPage, newPerPage) => {
    const pagination = this.state.pagination;
    const newPagination = Object.assign(pagination, {
      page: newPage,
      perPage: newPerPage,
    });
    this.setState({ pagination, newPagination }, this.loadData);
  };

  _onSearch = (query, resetLoadingCallback) => {
    const pagination = this.state.pagination;
    const newPagination = Object.assign(pagination, { query: query });
    this.setState({ pagination, newPagination }, () => this.loadData(resetLoadingCallback));
  };

  _onReset = () => {
    const pagination = this.state.pagination;
    const newPagination = Object.assign(pagination, { query: '' });
    this.setState({ pagination, newPagination }, this.loadData);
  };

  render() {
    if (this._isLoading()) {
      return (
        <div style={{ marginLeft: 10 }}>
          <Spinner />
        </div>
      );
    }

    if (this.state.streams.length === 0) {
      const createStreamButton = (
        <IfPermitted permissions="streams:create">
          <CreateStreamButton bsSize="small"
                              bsStyle="link"
                              className="btn-text"
                              buttonText="Create one now"
                              indexSets={this.props.indexSets}
                              onSave={this.props.onStreamSave} />
        </IfPermitted>
      );

      return (
        <Alert bsStyle="warning">
          <Icon name="info-circle" />&nbsp;No streams configured. {createStreamButton}
        </Alert>
      );
    }

    const streamsList = this.state.filteredStreams ? (
      <StreamList streams={this.state.filteredStreams}
                  streamRuleTypes={this.state.streamRuleTypes}
                  permissions={this.props.currentUser.permissions}
                  user={this.props.currentUser}
                  onStreamSave={this.props.onStreamSave}
                  indexSets={this.props.indexSets} />
    ) : <Spinner />;

    return (
      <div>
        <Row className="row-sm">
          <Col md={8}>
            <SearchForm onSearch={this._onSearch} onReset={this._onReset} useLoadingState />
          </Col>
        </Row>
        <Row>
          <Col md={12}>
            <PaginatedList onChange={this._onPageChange}
                           totalItems={this.state.pagination.total}>
              <br />
              <br />
              <div>{streamsList}</div>
            </PaginatedList>
          </Col>
        </Row>
      </div>
    );
  }
}

export default StreamComponent;
