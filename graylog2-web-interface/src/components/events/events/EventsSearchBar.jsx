/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import max from 'lodash/max';

import { ButtonGroup, Button } from 'components/bootstrap';
import { SearchForm, TimeUnitInput, Icon } from 'components/common';
import { extractDurationAndUnit } from 'components/common/TimeUnitInput';

import styles from './EventsSearchBar.css';

const TIME_UNITS = ['DAYS', 'HOURS', 'MINUTES', 'SECONDS'];

class EventsSearchBar extends React.Component {
  static propTypes = {
    parameters: PropTypes.object.isRequired,
    onQueryChange: PropTypes.func.isRequired,
    onAlertFilterChange: PropTypes.func.isRequired,
    onTimeRangeChange: PropTypes.func.isRequired,
    onSearchReload: PropTypes.func.isRequired,
  };

  constructor(props) {
    super(props);

    const timerangeDuration = extractDurationAndUnit(props.parameters.timerange.range * 1000, TIME_UNITS);

    this.state = {
      isReloadingResults: false,
      timeRangeDuration: timerangeDuration.duration,
      timeRangeUnit: timerangeDuration.unit,
    };
  }

  updateSearchTimeRange = (nextValue, nextUnit) => {
    const { onTimeRangeChange } = this.props;
    const durationInSeconds = moment.duration(max([nextValue, 1]), nextUnit).asSeconds();

    onTimeRangeChange('relative', durationInSeconds);
    this.setState({ timeRangeDuration: nextValue, timeRangeUnit: nextUnit });
  };

  resetLoadingState = () => {
    this.setState({ isReloadingResults: false });
  };

  handleSearchReload = () => {
    this.setState({ isReloadingResults: true });
    const { onSearchReload } = this.props;

    onSearchReload(this.resetLoadingState);
  };

  render() {
    const { parameters, onQueryChange, onAlertFilterChange } = this.props;
    const { isReloadingResults, timeRangeUnit, timeRangeDuration } = this.state;

    const filterAlerts = parameters.filter.alerts;

    return (
      <div className={styles.eventsSearchBar}>
        <div>
          <div className={styles.searchForm}>
            <SearchForm query={parameters.query}
                        onSearch={onQueryChange}
                        placeholder="Find Events"
                        topMargin={0}
                        useLoadingState>
              <Button onClick={this.handleSearchReload} disabled={isReloadingResults}>
                <Icon name="sync" spin={isReloadingResults} />
              </Button>
            </SearchForm>
          </div>

          <TimeUnitInput id="event-timerange-selector"
                         update={this.updateSearchTimeRange}
                         units={TIME_UNITS}
                         unit={timeRangeUnit}
                         value={timeRangeDuration}
                         clearable
                         pullRight
                         required />
        </div>
        <div>
          <ButtonGroup>
            <Button active={filterAlerts === 'only'} onClick={onAlertFilterChange('only')}>Alerts</Button>
            <Button active={filterAlerts === 'exclude'} onClick={onAlertFilterChange('exclude')}>Events</Button>
            <Button active={filterAlerts === 'include'} onClick={onAlertFilterChange('include')}>Both</Button>
          </ButtonGroup>
        </div>
      </div>
    );
  }
}

export default EventsSearchBar;
