import React from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const StreamsStore = StoreProvider.getStore('Streams');

import OutputsComponent from 'components/outputs/OutputsComponent';
import SupportLink from 'components/support/SupportLink';
import { DocumentTitle, Spinner } from 'components/common';
import Routes from 'routing/Routes';

const StreamOutputsPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore)],
  getInitialState() {
    return { stream: undefined };
  },
  componentDidMount() {
    StreamsStore.get(this.props.params.streamId, (stream) => {
      this.setState({ stream: stream });
    });
  },
  render() {
    if (!this.state.stream) {
      return <Spinner />;
    }
    return (
      <DocumentTitle title={`Outputs for Stream ${this.state.stream.title}`}>
        <div>
          <Row className="content content-head">
            <Col md={10}>
              <h1>
                Outputs for Stream &raquo;{this.state.stream.title}&laquo;
              </h1>

              <p className="description">
                Graylog nodes can forward messages of streams via outputs. Launch or terminate as many outputs as you want here.
                You can also reuse outputs that are already running for other streams.

                A global view of all configured outputs is available <a href="@routes.OutputsController.index()">here</a>.
                You can find output plugins on <a href="https://marketplace.graylog.org/" target="_blank">the Graylog Marketplace</a>.
              </p>

              <SupportLink>
                <i>Removing</i> an output removes it from this stream but it will still be in the list of available outputs.
                Deleting an output <i>globally</i> will remove it from this and all other streams and terminate it.
                You can see all defined outputs in details at the {' '} <LinkContainer to={Routes.SYSTEM.OUTPUTS}><a>global output list</a></LinkContainer>.
              </SupportLink>
            </Col>
          </Row>
          <OutputsComponent streamId={this.state.stream.id} permissions={this.state.currentUser.permissions} />
        </div>
      </DocumentTitle>
    );
  },
});

export default StreamOutputsPage;
