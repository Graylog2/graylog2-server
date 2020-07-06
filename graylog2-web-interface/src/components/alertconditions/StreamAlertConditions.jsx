import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import { LinkContainer } from 'react-router-bootstrap';
import naturalSort from 'javascript-natural-sort';

import { Button } from 'components/graylog';
import { AlertConditionsList } from 'components/alertconditions';
import Routes from 'routing/Routes';

const StreamAlertConditions = createReactClass({
  displayName: 'StreamAlertConditions',
  propTypes: {
    stream: PropTypes.object.isRequired,
    alertConditions: PropTypes.array.isRequired,
    availableConditions: PropTypes.object.isRequired,
    onConditionUpdate: PropTypes.func,
    onConditionDelete: PropTypes.func,
  },

  getDefaultProps() {
    return {
      onConditionUpdate: () => {},
      onConditionDelete: () => {},
    };
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
          <LinkContainer to={Routes.new_alert_condition_for_stream(this.props.stream.id)}>
            <Button bsStyle="success">Add new condition</Button>
          </LinkContainer>
        </div>
        <h2>Conditions</h2>
        <p className="description">Alert Conditions define when an Alert should be triggered for this Stream.</p>
        <AlertConditionsList alertConditions={alertConditions}
                             availableConditions={this.props.availableConditions}
                             streams={[this.props.stream]}
                             onConditionUpdate={this.props.onConditionUpdate}
                             onConditionDelete={this.props.onConditionDelete}
                             isStreamView />
      </div>
    );
  },
});

export default StreamAlertConditions;
