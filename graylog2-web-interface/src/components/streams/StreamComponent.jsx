import PropTypes from 'prop-types';
import React from 'react';
import { Row, Col, Alert } from 'react-bootstrap';

import { IfPermitted, TypeAheadDataFilter } from 'components/common';

import StoreProvider from 'injection/StoreProvider';
const StreamsStore = StoreProvider.getStore('Streams');
const StreamRulesStore = StoreProvider.getStore('StreamRules');

import StreamList from './StreamList';
import Spinner from 'components/common/Spinner';

import CreateStreamButton from './CreateStreamButton';

class StreamComponent extends React.Component {
  static propTypes = {
    currentUser: PropTypes.object.isRequired,
    onStreamSave: PropTypes.func.isRequired,
    indexSets: PropTypes.array.isRequired,
  };

  state = {};

  componentDidMount() {
    this.loadData();
    StreamRulesStore.types().then((types) => {
      this.setState({ streamRuleTypes: types });
    });
    StreamsStore.onChange(this.loadData);
    StreamRulesStore.onChange(this.loadData);
  }

  componentDidUpdate() {
    if (this.state.filteredStreams === null) {
      this._filterStreams();
    }
  }

  componentWillUnmount() {
    StreamsStore.unregister(this.loadData);
    StreamRulesStore.unregister(this.loadData);
  }

  loadData = () => {
    StreamsStore.load((streams) => {
      this.setState({
        streams: streams,
        filteredStreams: null,
      });
    });
  };

  _filterStreams = () => {
    if (this.streamFilter) {
      this.streamFilter.filterData();
    }
  };

  _updateFilteredStreams = (filteredStreams) => {
    this.setState({ filteredStreams: filteredStreams });
  };

  _isLoading = () => {
    return !(this.state.streams && this.state.streamRuleTypes);
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
          <CreateStreamButton bsSize="small" bsStyle="link" className="btn-text"
                              buttonText="Create one now"
                              indexSets={this.props.indexSets}
                              onSave={this.props.onStreamSave} />
        </IfPermitted>
      );

      return (
        <Alert bsStyle="warning">
          <i className="fa fa-info-circle" />&nbsp;No streams configured. {createStreamButton}
        </Alert>
      );
    }

    const streamsList = this.state.filteredStreams ? (<StreamList streams={this.state.filteredStreams} streamRuleTypes={this.state.streamRuleTypes}
                                                                  permissions={this.props.currentUser.permissions} user={this.props.currentUser}
                                                                  onStreamSave={this.props.onStreamSave} indexSets={this.props.indexSets} />) : <Spinner />;

    return (
      <div>
        <Row className="row-sm">
          <Col md={8}>
            <TypeAheadDataFilter id="stream-data-filter"
                                 ref={(streamFilter) => { this.streamFilter = streamFilter; }}
                                 label="Filter streams"
                                 data={this.state.streams}
                                 displayKey={'title'}
                                 filterSuggestions={[]}
                                 searchInKeys={['title', 'description']}
                                 onDataFiltered={this._updateFilteredStreams} />
          </Col>
        </Row>
        <Row>
          <Col md={12}>
            {streamsList}
          </Col>
        </Row>
      </div>
    );
  }
}

export default StreamComponent;
