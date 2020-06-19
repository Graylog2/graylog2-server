import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import Reflux from 'reflux';

import { Col, Row } from 'components/graylog';
import { DocumentTitle, PageHeader } from 'components/common';
import { AlertsHeaderToolbar } from 'components/alerts';
import { CreateAlertNotificationInput } from 'components/alertnotifications';
import Routes from 'routing/Routes';
import StoreProvider from 'injection/StoreProvider';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const NewAlertNotificationPage = createReactClass({
  displayName: 'NewAlertNotificationPage',
  propTypes: {
    location: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(CurrentUserStore)],

  render() {
    const streamId = this.props.location.query.stream_id;

    return (
      <DocumentTitle title="New alert notification">
        <div>
          <PageHeader title="New alert notification">
            <span>
              Create a new notification that you can use to not miss any of your alerts.
            </span>

            <span>
              Remember to assign the notifications to use in the alert conditions page.
            </span>

            <span>
              <AlertsHeaderToolbar active={Routes.LEGACY_ALERTS.NOTIFICATIONS} />
            </span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <CreateAlertNotificationInput initialSelectedStream={streamId} />
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  },
});

export default NewAlertNotificationPage;
