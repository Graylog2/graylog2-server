import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import numeral from 'numeral';
import moment from 'moment';

import { Alert, Col, Row, Button } from 'components/graylog';
import { Icon, Spinner } from 'components/common';
import StoreProvider from 'injection/StoreProvider';
import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';

import { SmallSupportLink, DocumentationLink } from 'components/support';

const IndexerFailuresStore = StoreProvider.getStore('IndexerFailures');

class IndexerFailuresComponent extends React.Component {
  state = {};

  componentDidMount() {
    const since = moment().subtract(24, 'hours');

    IndexerFailuresStore.count(since).then((response) => {
      this.setState({ total: response.count });
    });
  }

  _formatFailuresSummary = () => {
    return (
      <Alert bsStyle={this.state.total === 0 ? 'success' : 'danger'}>
        <Icon name={this._iconForFailureCount(this.state.total)} /> {this._formatTextForFailureCount(this.state.total)}

        <LinkContainer to={Routes.SYSTEM.INDICES.FAILURES}>
          <Button bsStyle="info" bsSize="xs" className="pull-right">
            Show errors
          </Button>
        </LinkContainer>
      </Alert>
    );
  };

  _formatTextForFailureCount = (count) => {
    if (count === 0) {
      return 'No failed indexing attempts in the last 24 hours.';
    }
    return <strong>There were {numeral(count).format('0,0')} failed indexing attempts in the last 24 hours.</strong>;
  };

  _iconForFailureCount = (count) => {
    if (count === 0) {
      return 'check-circle';
    }
    return 'ambulance';
  };

  render() {
    let content;
    if (this.state.total === undefined) {
      content = <Spinner />;
    } else {
      content = this._formatFailuresSummary();
    }

    return (
      <Row className="content">
        <Col md={12}>
          <h2>Indexer failures</h2>

          <SmallSupportLink>
            Every message that was not successfully indexed will be logged as an indexer failure. You can learn more about this feature in the{' '}
            <DocumentationLink page={DocsHelper.PAGES.INDEXER_FAILURES} text="Graylog documentation" />.
          </SmallSupportLink>

          {content}
        </Col>
      </Row>
    );
  }
}

export default IndexerFailuresComponent;
