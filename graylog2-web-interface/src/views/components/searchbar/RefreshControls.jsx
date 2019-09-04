// @flow strict
import React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';

// $FlowFixMe: imports from core need to be fixed in flow
import connect from 'stores/connect';

// $FlowFixMe: imports from core need to be fixed in flow
import { MenuItem, ButtonGroup, DropdownButton, Button, Icon } from 'components/graylog';
// $FlowFixMe: imports from core need to be fixed in flow
import Pluralize from 'components/common/Pluralize';
import { RefreshActions, RefreshStore } from 'views/stores/RefreshStore';

import styles from './RefreshControls.css';

type RefreshConfig = {
  interval: number,
  enabled: boolean,
};

type Props = {
  refreshConfig: RefreshConfig,
};

type State = {
  /* eslint-disable-next-line no-undef */
  intervalId: ?IntervalID,
};

class RefreshControls extends React.Component<Props, State> {
  static propTypes = {
    refreshConfig: PropTypes.object.isRequired,
  };

  static INTERVAL_OPTIONS : Array<[string, number]> = [
    ['1 Second', 1000],
    ['2 Seconds', 2000],
    ['5 Seconds', 5000],
    ['10 Seconds', 10000],
    ['30 Seconds', 30000],
    ['1 Minute', 60000],
    ['5 Minutes', 300000],
  ];

  constructor(props: Props) {
    super(props);

    this.state = {
      intervalId: undefined,
    };
  }

  componentWillUnmount(): void {
    RefreshActions.disable();
  }

  _toggleEnable = (): void => {
    if (this.props.refreshConfig.enabled) {
      RefreshActions.disable();
    } else {
      RefreshActions.enable();
    }
  };

  _onChange = (interval: number): void => {
    RefreshActions.setInterval(interval);
  };

  render() {
    const intervalOptions = RefreshControls.INTERVAL_OPTIONS.map(([label, interval]: [string, number]) => {
      return <MenuItem key={`RefreshControls-${label}`} onClick={() => this._onChange(interval)}>{label}</MenuItem>;
    });
    const intervalDuration = moment.duration(this.props.refreshConfig.interval);
    const naturalInterval = intervalDuration.asSeconds() < 60
      ? <span>{intervalDuration.asSeconds()} <Pluralize singular="second" plural="seconds" value={intervalDuration.asSeconds()} /></span>
      : <span>{intervalDuration.asMinutes()} <Pluralize singular="minute" plural="minutes" value={intervalDuration.asMinutes()} /></span>;
    const buttonLabel = <span>Update every {naturalInterval}</span>;
    return (
      <div className={`${styles.position} pull-right`}>
        <ButtonGroup>
          <Button onClick={this._toggleEnable}>
            {this.props.refreshConfig.enabled ? <Icon className="fa fa-pause" /> : <Icon className="fa fa-play" />}
          </Button>

          <DropdownButton title={this.props.refreshConfig.enabled ? buttonLabel : 'Not updating'} id="refresh-options-dropdown">
            {intervalOptions}
          </DropdownButton>
        </ButtonGroup>
      </div>
    );
  }
}

export default connect(RefreshControls, { refreshConfig: RefreshStore });
