import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import { Button, Row, Col } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import DocumentationLink from 'components/support/DocumentationLink';
import { PageHeader, Spinner } from 'components/common';
import PipelineConnections from './PipelineConnections';

import Routes from 'routing/Routes';

import PipelinesActions from 'pipelines/PipelinesActions';
import PipelinesStore from 'pipelines/PipelinesStore';
import PipelineConnectionsActions from 'pipeline-connections/PipelineConnectionsActions';
import PipelineConnectionsStore from 'pipeline-connections/PipelineConnectionsStore';

const PipelineConnectionsPage = React.createClass({
  contextTypes: {
    storeProvider: PropTypes.object,
  },

  mixins: [Reflux.connect(PipelinesStore), Reflux.connect(PipelineConnectionsStore)],

  getInitialState() {
    return {
      streams: undefined,
    };
  },

  componentDidMount() {
    PipelinesActions.list();
    PipelineConnectionsActions.list();

    const store = this.context.storeProvider.getStore('Streams');
    store.listStreams().then((streams) => this.setState({streams: streams}));
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
                             connections={this.state.connections} onConnectionsChange={this._updateConnections}/>
      );
    }
    return (
      <span>
        <PageHeader title="Pipeline connections" titleSize={8} buttonSize={4}>
          <span>
            Pipelines let you process messages sent to Graylog. Here you can select which streams will be used{' '}
            as input for the different pipelines you configure.
          </span>
          <span>
            Read more about Graylog pipelines in the <DocumentationLink page={"TODO"} text="documentation"/>.
          </span>

          <span>
            <LinkContainer to={Routes.STREAMS}>
              <Button bsStyle="info">Manage streams</Button>
            </LinkContainer>
            &nbsp;
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
