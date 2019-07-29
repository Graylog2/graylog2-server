import React from 'react';
import PropTypes from 'prop-types';
import { Button, ButtonGroup, ControlLabel, FormControl, FormGroup } from 'react-bootstrap';
import moment from 'moment';

import { SearchForm, TimeUnitInput } from 'components/common';
import { extractDurationAndUnit } from 'components/common/TimeUnitInput';
import FormsUtils from 'util/FormsUtils';

import styles from './EventsSearchBar.css';

const TIME_UNITS = ['DAYS', 'HOURS', 'MINUTES', 'SECONDS'];

class EventsSearchBar extends React.Component {
  static propTypes = {
    parameters: PropTypes.object.isRequired,
    pageSize: PropTypes.number.isRequired,
    pageSizes: PropTypes.array.isRequired,
    onQueryChange: PropTypes.func.isRequired,
    onAlertFilterChange: PropTypes.func.isRequired,
    onTimeRangeChange: PropTypes.func.isRequired,
    onPageSizeChange: PropTypes.func.isRequired,
  };

  updateSearchTimeRange = (nextValue, nextUnit) => {
    const { onTimeRangeChange } = this.props;
    const durationInSeconds = moment.duration(nextValue, nextUnit).asSeconds();
    onTimeRangeChange('relative', durationInSeconds);
  };

  handlePageSizeChange = (event) => {
    const { onPageSizeChange } = this.props;
    onPageSizeChange(FormsUtils.getValueFromInput(event.target));
  };

  render() {
    const { parameters, pageSize, pageSizes, onQueryChange, onAlertFilterChange } = this.props;

    const filterAlerts = parameters.filter.alerts;
    const timerangeDuration = extractDurationAndUnit(parameters.timerange.range * 1000, TIME_UNITS);

    return (
      <div className={styles.eventsSearchBar}>
        <div>
          <div className={styles.searchForm}>
            <SearchForm query={parameters.query}
                        onSearch={onQueryChange}
                        searchButtonLabel={<i className="fa fa-search" />}
                        loadingLabel=""
                        placeholder="Find Events"
                        queryWidth="100%"
                        topMargin={0}
                        useLoadingState>
              <Button><i className="fa fa-refresh" /></Button>
            </SearchForm>
          </div>

          <TimeUnitInput id="event-timerange-selector"
                         update={this.updateSearchTimeRange}
                         units={TIME_UNITS}
                         unit={timerangeDuration.unit}
                         value={timerangeDuration.duration}
                         pullRight
                         required />
        </div>
        <div>
          <ButtonGroup>
            <Button active={filterAlerts === 'only'} onClick={onAlertFilterChange('only')}>Alerts</Button>
            <Button active={filterAlerts === 'exclude'} onClick={onAlertFilterChange('exclude')}>Events</Button>
            <Button active={filterAlerts === 'include'} onClick={onAlertFilterChange('include')}>Both</Button>
          </ButtonGroup>

          <FormGroup className="form-inline">
            <ControlLabel>Show</ControlLabel>
            <FormControl componentClass="select" bsSize="small" value={pageSize} onChange={this.handlePageSizeChange}>
              {pageSizes.map(size => <option key={`option-${size}`} value={size}>{size}</option>)}
            </FormControl>
          </FormGroup>
        </div>
      </div>
    );
  }
}

export default EventsSearchBar;
