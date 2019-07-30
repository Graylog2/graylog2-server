import React from 'react';
import PropTypes from 'prop-types';
import { ControlLabel, FormControl, FormGroup, HelpBlock, InputGroup } from 'react-bootstrap';
import lodash from 'lodash';
import moment from 'moment';

import { TimeUnitInput } from 'components/common';
import { extractDurationAndUnit } from 'components/common/TimeUnitInput';
import FormsUtils from 'util/FormsUtils';

import commonStyles from '../common/commonStyles.css';

const TIME_UNITS = ['HOURS', 'MINUTES', 'SECONDS'];

class NotificationSettingsForm extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    onSettingsChange: PropTypes.func.isRequired,
  };

  constructor(props) {
    super(props);

    const { backlog_size: backlogSize } = props.eventDefinition.notification_settings;
    const defaultBacklogSize = props.eventDefinition.notification_settings.default_backlog_size;
    const effectiveBacklogSize = lodash.defaultTo(backlogSize, defaultBacklogSize);

    this.state = {
      lastEnabledGracePeriod: undefined,
      isBacklogSizeEnabled: (backlogSize === null ? false : (effectiveBacklogSize > 0)),
      backlogSize: effectiveBacklogSize,
    };
  }

  propagateChanges = (key, value) => {
    const { eventDefinition, onSettingsChange } = this.props;
    const nextNotificationSettings = lodash.cloneDeep(eventDefinition.notification_settings);
    nextNotificationSettings[key] = value;
    onSettingsChange('notification_settings', nextNotificationSettings);
  };

  handleGracePeriodChange = (nextValue, nextUnit, enabled) => {
    const durationInMs = enabled ? moment.duration(nextValue, nextUnit).asMilliseconds() : 0;
    this.propagateChanges('grace_period_ms', durationInMs);

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

  handleChange = (event) => {
    const { name } = event.target;
    const value = FormsUtils.getValueFromInput(event.target);
    this.setState({ [lodash.camelCase(name)]: value });
    this.propagateChanges(name, value);
  };

  toggleBacklogSize = () => {
    const { isBacklogSizeEnabled, backlogSize } = this.state;
    this.setState({ isBacklogSizeEnabled: !isBacklogSizeEnabled });
    this.propagateChanges('backlog_size', (isBacklogSizeEnabled ? 0 : backlogSize));
  };

  render() {
    const { eventDefinition } = this.props;
    const { lastEnabledGracePeriod, isBacklogSizeEnabled, backlogSize } = this.state;

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

          <FormGroup>
            <ControlLabel>Message Backlog</ControlLabel>
            <InputGroup>
              <InputGroup.Addon>
                <input id="toggle_backlog_size"
                       type="checkbox"
                       checked={isBacklogSizeEnabled}
                       onChange={this.toggleBacklogSize} />
              </InputGroup.Addon>
              <FormControl type="number"
                           id="backlog_size"
                           name="backlog_size"
                           onChange={this.handleChange}
                           value={backlogSize}
                           disabled={!isBacklogSizeEnabled} />
            </InputGroup>
            <HelpBlock>Number of messages to be included in Notifications.</HelpBlock>
          </FormGroup>
        </fieldset>
      </React.Fragment>
    );
  }
}

export default NotificationSettingsForm;
