import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import naturalSort from 'javascript-natural-sort';

import { Spinner } from 'components/common';
import { AlertConditionsList } from 'components/alertconditions';

import Routes from 'routing/Routes';

import CombinedProvider from 'injection/CombinedProvider';

const { AlertConditionsStore, AlertConditionsActions } = CombinedProvider.get('AlertConditions');

const StreamAlertConditions = createReactClass({
  displayName: 'AlertConditionsComponent',
  propTypes: {
    stream: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(AlertConditionsStore)],

  componentDidMount() {
    this.loadData();
  },

  loadData() {
    AlertConditionsActions.list(this.props.stream.id);
  },

  render() {
    const isLoading = !this.state.alertConditions;
    if (isLoading) {
      return <Spinner />;
    }

    const alertConditions = this.state.alertConditions.sort((a1, a2) => {
      const t1 = a1.title || 'Untitled';
      const t2 = a2.title || 'Untitled';
      return naturalSort(t1.toLowerCase(), t2.toLowerCase());
    });

    return (
      <div>
        <div className="pull-right">
          <LinkContainer to={Routes.ALERTS.NEW_CONDITION}>
            <Button bsStyle="success">Add new condition</Button>
          </LinkContainer>
        </div>
        <h2>Conditions</h2>
        <p>These are all configured alert conditions for stream <em>{this.props.stream.title}</em>.</p>
        <AlertConditionsList alertConditions={alertConditions} streams={[this.props.stream]} />
      </div>
    );
  },
});

export default StreamAlertConditions;
