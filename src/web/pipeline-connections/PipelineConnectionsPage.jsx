import React from 'react';
import Reflux from 'reflux';
import { Button, Row, Col } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import DocumentationLink from 'components/support/DocumentationLink';
import { PageHeader, Spinner } from 'components/common';
import PipelineConnections from './PipelineConnections';

import PipelinesActions from 'pipelines/PipelinesActions';
import PipelinesStore from 'pipelines/PipelinesStore';
import PipelineConnectionsActions from 'pipeline-connections/PipelineConnectionsActions';
import PipelineConnectionsStore from 'pipeline-connections/PipelineConnectionsStore';

import StoreProvider from 'injection/StoreProvider';
const StreamsStore = StoreProvider.getStore('Streams');

import DocsHelper from 'util/DocsHelper';

const PipelineConnectionsPage = React.createClass({
  mixins: [Reflux.connect(PipelinesStore), Reflux.connect(PipelineConnectionsStore)],

  getInitialState() {
    return {
      streams: undefined,
    };
  },

  componentDidMount() {
    PipelinesActions.list();
    PipelineConnectionsActions.list();

    StreamsStore.listStreams().then((streams) => {
      streams.push({
        id: 'default',
        title: 'Default',
        description: 'Stream used by default for messages not matching another stream.',
      });
      this.setState({ streams });
    });
  },

  _updateConnections(connections, callback) {
    PipelineConnectionsActions.update(connections);
    callback();
  },

  _isLoading() {
    return !this.state.pipelines || !this.state.streams || !this.state.connections;
  },

  render() {
    let content;
    if (this._isLoading()) {
      content = <Spinner />;
    } else {
      content = (
        <PipelineConnections pipelines={this.state.pipelines} streams={this.state.streams}
                             connections={this.state.connections} onConnectionsChange={this._updateConnections} />
      );
    }
    return (
      <span>
        <PageHeader title="Pipeline connections" experimental>
          <span>
            Pipelines let you process messages sent to Graylog. Here you can select which streams will be used{' '}
            as input for the different pipelines you configure.
          </span>
          <span>
            Read more about Graylog pipelines in the <DocumentationLink page={DocsHelper.PAGES.PIPELINES} text="documentation" />.
          </span>

          <span>
            <LinkContainer to={'/system/pipelines/overview'}>
              <Button bsStyle="info">Manage pipelines</Button>
            </LinkContainer>
            &nbsp;
            <LinkContainer to={'/system/pipelines/rules'}>
              <Button bsStyle="info">Manage rules</Button>
            </LinkContainer>
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            {content}
          </Col>
        </Row>
      </span>);
  },
});

export default PipelineConnectionsPage;
