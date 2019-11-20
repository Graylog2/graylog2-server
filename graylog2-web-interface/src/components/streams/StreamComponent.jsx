import PropTypes from 'prop-types';
import React from 'react';

import { Alert, Col, Row } from 'components/graylog';
import { Icon, IfPermitted, PaginatedList, SearchForm } from 'components/common';

import StoreProvider from 'injection/StoreProvider';
import Spinner from 'components/common/Spinner';
import StreamList from './StreamList';
import CreateStreamButton from './CreateStreamButton';

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
    const { state } = this;
    const { page, perPage, query } = state.pagination;
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
    const { state } = this;
    return !(state.streams && state.streamRuleTypes);
  };

  _onPageChange = (newPage, newPerPage) => {
    const { pagination } = this.state;
    const newPagination = Object.assign(pagination, {
      page: newPage,
      perPage: newPerPage,
    });
    this.setState({ pagination: newPagination }, this.loadData);
  };

  _onSearch = (query, resetLoadingCallback) => {
    const { pagination } = this.state;
    const newPagination = Object.assign(pagination, { query: query });
    this.setState({ pagination: newPagination }, () => this.loadData(resetLoadingCallback));
  };

  _onReset = () => {
    const { pagination } = this.state;
    const newPagination = Object.assign(pagination, { query: '' });
    this.setState({ pagination: newPagination }, this.loadData);
  };

  render() {
    const { streams, pagination, streamRuleTypes } = this.state;
    const { currentUser, onStreamSave, indexSets } = this.props;

    if (this._isLoading()) {
      return (
        <div style={{ marginLeft: 10 }}>
          <Spinner />
        </div>
      );
    }

    if (streams.length === 0) {
      const createStreamButton = (
        <IfPermitted permissions="streams:create">
          <CreateStreamButton bsSize="small"
                              bsStyle="link"
                              className="btn-text"
                              buttonText="Create one now"
                              indexSets={indexSets}
                              onSave={onStreamSave} />
        </IfPermitted>
      );

      return (
        <Alert bsStyle="warning">
          <Icon name="info-circle" />&nbsp;No streams configured. {createStreamButton}
        </Alert>
      );
    }

    const streamsList = (
      <StreamList streams={streams}
                  streamRuleTypes={streamRuleTypes}
                  permissions={currentUser.permissions}
                  user={currentUser}
                  onStreamSave={onStreamSave}
                  indexSets={indexSets} />
    );

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
                           totalItems={pagination.total}>
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
