// @flow strict
import React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import styled from 'styled-components';

// $FlowFixMe: imports from core need to be fixed in flow
import connect from 'stores/connect';

// $FlowFixMe: imports from core need to be fixed in flow
import { MenuItem, ButtonGroup, DropdownButton, Button } from 'components/graylog';
// $FlowFixMe: imports from core need to be fixed in flow
import { Icon, Pluralize } from 'components/common';
// $FlowFixMe: imports from core need to be fixed in flow
import { RefreshActions, RefreshStore } from 'views/stores/RefreshStore';

const ControlsContainer = styled.div`
  max-width: 100%;
`;

const FlexibleButtonGroup = styled(ButtonGroup)`
  display: flex;
  > .btn-group {
    .btn:first-child {
      max-width: 100%;
    }
  }
`;

const ButtonLabel = styled.div`
  display: inline-block;
  text-overflow: ellipsis;
  overflow: hidden;
  max-width: calc(100% - 9px);
  vertical-align: inherit;
`;


type RefreshConfig = {
  interval: number,
  enabled: boolean,
};

type Props = {
  refreshConfig: RefreshConfig,
};

class RefreshControls extends React.Component<Props> {
  static propTypes = {
    refreshConfig: PropTypes.object.isRequired,
  };

  componentWillUnmount(): void {
    RefreshActions.disable();
  }

  _toggleEnable = (): void => {
    const { refreshConfig } = this.props;
    if (refreshConfig.enabled) {
      RefreshActions.disable();
    } else {
      RefreshActions.enable();
    }
  };

  _onChange = (interval: number): void => {
    RefreshActions.setInterval(interval);
  };

  _buttonLabel = (refreshConfigEnabled, naturalInterval) => {
    let buttonText = 'Not updating';
    if (refreshConfigEnabled) {
      buttonText = <React.Fragment>Update every {naturalInterval}</React.Fragment>;
    }
    return <ButtonLabel>{buttonText}</ButtonLabel>;
  }

  static INTERVAL_OPTIONS: Array<[string, number]> = [
    ['1 Second', 1000],
    ['2 Seconds', 2000],
    ['5 Seconds', 5000],
    ['10 Seconds', 10000],
    ['30 Seconds', 30000],
    ['1 Minute', 60000],
    ['5 Minutes', 300000],
  ];

  render() {
    const { refreshConfig } = this.props;
    const intervalOptions = RefreshControls.INTERVAL_OPTIONS.map(([label, interval]: [string, number]) => {
      return <MenuItem key={`RefreshControls-${label}`} onClick={() => this._onChange(interval)}>{label}</MenuItem>;
    });
    const intervalDuration = moment.duration(refreshConfig.interval);
    const naturalInterval = intervalDuration.asSeconds() < 60
      ? <span>{intervalDuration.asSeconds()} <Pluralize singular="second" plural="seconds" value={intervalDuration.asSeconds()} /></span>
      : <span>{intervalDuration.asMinutes()} <Pluralize singular="minute" plural="minutes" value={intervalDuration.asMinutes()} /></span>;
    const buttonLabel = this._buttonLabel(refreshConfig.enabled, naturalInterval);
    return (
      <ControlsContainer className="pull-right">
        <FlexibleButtonGroup>
          <Button onClick={this._toggleEnable}>
            {refreshConfig.enabled ? <Icon name="pause" /> : <Icon name="play" />}
          </Button>

          <DropdownButton title={buttonLabel} id="refresh-options-dropdown">
            {intervalOptions}
          </DropdownButton>
        </FlexibleButtonGroup>
      </ControlsContainer>
    );
  }
}

export default connect(RefreshControls, { refreshConfig: RefreshStore });
