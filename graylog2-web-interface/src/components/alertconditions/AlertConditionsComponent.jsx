import React from 'react';
import Reflux from 'reflux';
import { Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import naturalSort from 'javascript-natural-sort';

import { Spinner } from 'components/common';
import { AlertConditionsList } from 'components/alertconditions';

import Routes from 'routing/Routes';

import CombinedProvider from 'injection/CombinedProvider';
const { StreamsStore } = CombinedProvider.get('Streams');
const { AlertConditionsStore, AlertConditionsActions } = CombinedProvider.get('AlertConditions');

const AlertConditionsComponent = React.createClass({
  mixins: [Reflux.connect(AlertConditionsStore)],

  getInitialState() {
    return {
      streams: undefined,
    };
  },

  componentDidMount() {
    this._loadData();
  },

  _loadData() {
    StreamsStore.listStreams().then((streams) => {
      this.setState({ streams: streams });
    });

    AlertConditionsActions.listAll();
  },

  _isLoading() {
    return !this.state.streams || !this.state.allAlertConditions;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const alertConditions = this.state.allAlertConditions.sort((a1, a2) => {
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
        <p>These are all configured alert conditions.</p>
        <AlertConditionsList alertConditions={alertConditions} streams={this.state.streams} />
      </div>
    );
  },
});

export default AlertConditionsComponent;
