import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import naturalSort from 'javascript-natural-sort';

import { LinkContainer } from 'components/graylog/router';
import { Button } from 'components/graylog';
import { Spinner } from 'components/common';
import { AlertConditionsList } from 'components/alertconditions';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';

const { StreamsStore } = CombinedProvider.get('Streams');
const { AlertConditionsStore, AlertConditionsActions } = CombinedProvider.get('AlertConditions');

const AlertConditionsComponent = createReactClass({
  displayName: 'AlertConditionsComponent',
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
    AlertConditionsActions.available();
  },

  _isLoading() {
    const { streams, allAlertConditions, availableConditions } = this.state;

    return !streams || !allAlertConditions || !availableConditions;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { streams, allAlertConditions, availableConditions } = this.state;

    const alertConditions = allAlertConditions.sort((a1, a2) => {
      const t1 = a1.title || 'Untitled';
      const t2 = a2.title || 'Untitled';

      return naturalSort(t1.toLowerCase(), t2.toLowerCase());
    });

    return (
      <div>
        <div className="pull-right">
          <LinkContainer to={Routes.LEGACY_ALERTS.NEW_CONDITION}>
            <Button bsStyle="success">Add new condition</Button>
          </LinkContainer>
        </div>
        <h2>Conditions</h2>
        <p>These are all configured alert conditions.</p>
        <AlertConditionsList alertConditions={alertConditions}
                             availableConditions={availableConditions}
                             streams={streams}
                             onConditionUpdate={this._loadData}
                             onConditionDelete={this._loadData} />
      </div>
    );
  },
});

export default AlertConditionsComponent;
