import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import { Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import naturalSort from 'javascript-natural-sort';

import { AlertConditionsList } from 'components/alertconditions';

import Routes from 'routing/Routes';

const StreamAlertConditions = createReactClass({
  displayName: 'StreamAlertConditions',
  propTypes: {
    stream: PropTypes.object.isRequired,
    alertConditions: PropTypes.array.isRequired,
  },

  render() {
    const alertConditions = this.props.alertConditions.sort((a1, a2) => {
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
        <p>These are all configured alert conditions for the stream <em>{this.props.stream.title}</em>.</p>
        <AlertConditionsList alertConditions={alertConditions}
                             streams={[this.props.stream]}
                             onConditionUpdate={this.loadData}
                             onConditionDelete={this.loadData}
                             isStreamView />
      </div>
    );
  },
});

export default StreamAlertConditions;
