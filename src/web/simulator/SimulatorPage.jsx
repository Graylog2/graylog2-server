import React from 'react';
import Reflux from 'reflux';
import { Button, Row, Col } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { PageHeader, Spinner } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import ProcessorSimulator from './ProcessorSimulator';

import PipelinesActions from 'pipelines/PipelinesActions';
import PipelinesStore from 'pipelines/PipelinesStore';
import PipelineConnectionsActions from 'pipeline-connections/PipelineConnectionsActions';
import PipelineConnectionsStore from 'pipeline-connections/PipelineConnectionsStore';

import StoreProvider from 'injection/StoreProvider';
const StreamsStore = StoreProvider.getStore('Streams');

import DocsHelper from 'util/DocsHelper';

const SimulatorPage = React.createClass({
  propTypes: {
    params: React.PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(PipelinesStore), Reflux.connect(PipelineConnectionsStore)],

  getInitialState() {
    return {
      stream: this.props.params.streamId === 'default' ? this._getDefaultStream() : undefined,
    };
  },

  componentDidMount() {
    PipelinesActions.list();
    PipelineConnectionsActions.list();

    if (!this.state.stream) {
      StreamsStore.get(this.props.params.streamId, stream => this.setState({ stream: stream }));
    }
  },

  _getDefaultStream() {
    return {
      id: 'default',
      title: 'Default',
      description: 'Stream used by default for messages not matching another stream.',
    };
  },

  _isLoading() {
    return !this.state.pipelines || !this.state.stream || !this.state.connections;
  },

  render() {
    let content;
    if (this._isLoading()) {
      content = <Spinner/>;
    } else {
      content = <ProcessorSimulator stream={this.state.stream} />;
    }

    let title;
    if (this.state.stream) {
      title = <span>Simulate processing in stream <em>{this.state.stream.title}</em></span>;
    } else {
      title = 'Simulate processing';
    }

    return (
      <div>
        <PageHeader title={title} experimental>
          <span>
            Processing messages can be complex. Use this page to simulate the result of processing an incoming{' '}
            message using your current set of pipelines and rules for this stream.
          </span>
          <span>
            Read more about Graylog pipelines in the <DocumentationLink page={DocsHelper.PAGES.PIPELINES} text="documentation" />.
          </span>

          <span>
            <LinkContainer to={'/system/pipelines'}>
              <Button bsStyle="info">Manage connections</Button>
            </LinkContainer>
            &nbsp;
            <LinkContainer to={'/system/pipelines/overview'}>
              <Button bsStyle="info">Manage pipelines</Button>
            </LinkContainer>
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            {content}
          </Col>
        </Row>
      </div>
    );
  },
});

export default SimulatorPage;
