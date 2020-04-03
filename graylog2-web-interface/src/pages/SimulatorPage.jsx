import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import { Button, Col, Row } from 'components/graylog';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import ProcessorSimulator from 'components/simulator/ProcessorSimulator';

import StoreProvider from 'injection/StoreProvider';

import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';

const StreamsStore = StoreProvider.getStore('Streams');

// Events do not work on Pipelines yet, hide Events and System Events Streams.
const HIDDEN_STREAMS = [
  '000000000000000000000002',
  '000000000000000000000003',
];

class SimulatorPage extends React.Component {
  state = {
    streams: undefined,
  };

  componentDidMount() {
    StreamsStore.listStreams().then((streams) => {
      const filteredStreams = streams.filter((s) => !HIDDEN_STREAMS.includes(s.id));
      this.setState({ streams: filteredStreams });
    });
  }

  _isLoading = () => {
    const { streams } = this.state;
    return !streams;
  };

  render() {
    const { streams } = this.state;

    const content = this._isLoading() ? <Spinner /> : <ProcessorSimulator streams={streams} />;

    return (
      <DocumentTitle title="Simulate processing">
        <div>
          <PageHeader title="Simulate processing">
            <span>
              Processing messages can be complex. Use this page to simulate the result of processing an incoming
              message using your current set of pipelines and rules.
            </span>
            <span>
              Read more about Graylog pipelines in the <DocumentationLink page={DocsHelper.PAGES.PIPELINES} text="documentation" />.
            </span>

            <span>
              <LinkContainer to={Routes.SYSTEM.PIPELINES.OVERVIEW}>
                <Button bsStyle="info">Manage pipelines</Button>
              </LinkContainer>
              &nbsp;
              <LinkContainer to={Routes.SYSTEM.PIPELINES.RULES}>
                <Button bsStyle="info">Manage rules</Button>
              </LinkContainer>
              &nbsp;
              <LinkContainer to={Routes.SYSTEM.PIPELINES.SIMULATOR}>
                <Button bsStyle="info" className="active">Simulator</Button>
              </LinkContainer>
            </span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              {content}
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  }
}

export default SimulatorPage;
