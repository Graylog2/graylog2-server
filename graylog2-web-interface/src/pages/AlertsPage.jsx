import React from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import AlertsComponent from 'components/alerts/AlertsComponent';

import DocumentationLink from 'components/support/DocumentationLink';
import { PageHeader } from 'components/common';

import DocsHelper from 'util/DocsHelper';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const AlertsPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore)],
  render() {
    return (
      <div>
        <PageHeader title="Alerts overview">
          <span>
            Alerts are triggered when conditions you define are satisfied. Graylog will automatically mark alerts as
            resolved once the status of your conditions change.
          </span>

          <span>
            Read more about alerting in the <DocumentationLink page={DocsHelper.PAGES.ALERTS} text="documentation"/>.
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <AlertsComponent />
          </Col>
        </Row>
      </div>
    );
  },
});

export default AlertsPage;
