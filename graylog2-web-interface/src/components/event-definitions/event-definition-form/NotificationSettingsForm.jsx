import React from 'react';
import PropTypes from 'prop-types';
import { FormGroup, HelpBlock } from 'react-bootstrap';
import lodash from 'lodash';
import moment from 'moment';

import { TimeUnitInput } from 'components/common';
import { extractDurationAndUnit } from 'components/common/TimeUnitInput';

import commonStyles from '../common/commonStyles.css';

const TIME_UNITS = ['HOURS', 'MINUTES', 'SECONDS'];

class NotificationSettingsForm extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    onSettingsChange: PropTypes.func.isRequired,
  };

  state = {
    lastEnabledGracePeriod: undefined,
  };

  handleGracePeriodChange = (nextValue, nextUnit, enabled) => {
    const { eventDefinition, onSettingsChange } = this.props;
    const durationInMs = enabled ? moment.duration(nextValue, nextUnit).asMilliseconds() : 0;
    const nextNotificationSettings = lodash.cloneDeep(eventDefinition.notification_settings);
    nextNotificationSettings.grace_period_ms = durationInMs;
    onSettingsChange('notification_settings', nextNotificationSettings);

    // Update last enabled grace period in state to display, to display the previous value when input is disabled.
    const stateUpdate = {};
    if (enabled) {
      stateUpdate.lastEnabledGracePeriod = undefined;
    } else {
      stateUpdate.lastEnabledGracePeriod = {
        duration: nextValue,
        unit: nextUnit,
      };
    }
    this.setState(stateUpdate);
  };

  render() {
    const { eventDefinition } = this.props;
    const { lastEnabledGracePeriod } = this.state;

    if (eventDefinition.notifications.length === 0) {
      return null;
    }

    // Display old grace period to avoid input resetting to null when input is disabled.
    const gracePeriod = (lastEnabledGracePeriod === undefined
      ? extractDurationAndUnit(eventDefinition.notification_settings.grace_period_ms, TIME_UNITS)
      : lastEnabledGracePeriod);

    return (
      <React.Fragment>
        <h3 className={commonStyles.title}>Notification Settings</h3>
        <fieldset>
          <FormGroup controlId="grace-period">
            <TimeUnitInput label="Grace Period"
                           update={this.handleGracePeriodChange}
                           defaultEnabled={gracePeriod.duration !== 0}
                           value={gracePeriod.duration}
                           unit={gracePeriod.unit}
                           units={TIME_UNITS} />
            <HelpBlock>
              Time to wait after a Notification is sent, to send another Notification.
              Events with keys have different grace periods for each key.
            </HelpBlock>
          </FormGroup>
        </fieldset>
      </React.Fragment>
    );
  }
}

export default NotificationSettingsForm;
