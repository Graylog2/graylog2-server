import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Col, Row } from 'react-bootstrap';

import { DocumentTitle, PageHeader } from 'components/common';
import { AlertsHeaderToolbar } from 'components/alerts';
import { AlertNotificationsComponent } from 'components/alertnotifications';
import Routes from 'routing/Routes';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const AlertNotificationsPage = createReactClass({
  displayName: 'AlertNotificationsPage',
  mixins: [Reflux.connect(CurrentUserStore)],

  render() {
    return (
      <DocumentTitle title="Alert notifications">
        <div>
          <PageHeader title="Manage alert notifications">
            <span>
              Notifications let you be aware of changes in your alert conditions status any time. Graylog can send
              notifications directly to you or to other systems you use for that purpose.
            </span>

            <span>
              Remember to assign the notifications to use in the alert conditions page.
            </span>

            <span>
              <AlertsHeaderToolbar active={Routes.ALERTS.NOTIFICATIONS} />
            </span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <AlertNotificationsComponent />
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  },
});

export default AlertNotificationsPage;
