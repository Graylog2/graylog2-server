import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Col, Row } from 'react-bootstrap';

import DocumentationLink from 'components/support/DocumentationLink';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { AlertsHeaderToolbar } from 'components/alerts';
import { EditAlertConditionForm } from 'components/alertconditions';
import { StreamAlertNotifications } from 'components/alertnotifications';

import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';

import CombinedProvider from 'injection/CombinedProvider';
const { CurrentUserStore } = CombinedProvider.get('CurrentUser');
const { StreamsStore } = CombinedProvider.get('Streams');
const { AlertConditionsStore, AlertConditionsActions } = CombinedProvider.get('AlertConditions');

const EditAlertConditionPage = createReactClass({
  displayName: 'EditAlertConditionPage',

  propTypes: {
    params: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(CurrentUserStore), Reflux.connect(AlertConditionsStore)],

  getInitialState() {
    return {
      stream: undefined,
    };
  },

  componentDidMount() {
    StreamsStore.get(this.props.params.streamId, (stream) => {
      this.setState({ stream: stream });
    });

    AlertConditionsActions.get(this.props.params.streamId, this.props.params.conditionId);
  },

  _isLoading() {
    return !this.state.stream || !this.state.alertCondition;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const condition = this.state.alertCondition;
    const stream = this.state.stream;

    return (
      <DocumentTitle title={`Condition ${condition.title || 'Untitled'}`}>
        <div>
          <PageHeader title={<span>Condition <em>{condition.title || 'Untitled'}</em></span>}>
            <span>
              Define an alert condition and configure the way Graylog will notify you when that condition is satisfied.
            </span>

            <span>
              Are the default conditions not flexible enough? You can write your own! Read more about alerting in
              the{' '}
              <DocumentationLink page={DocsHelper.PAGES.ALERTS} text="documentation" />.
            </span>

            <span>
              <AlertsHeaderToolbar active={Routes.ALERTS.CONDITIONS} />
            </span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <EditAlertConditionForm alertCondition={condition} stream={stream} />
            </Col>
          </Row>

          <Row className="content">
            <Col md={12}>
              <StreamAlertNotifications stream={stream} />
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  },
});

export default EditAlertConditionPage;
