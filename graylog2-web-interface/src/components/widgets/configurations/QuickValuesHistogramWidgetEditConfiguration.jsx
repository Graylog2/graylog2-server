import PropTypes from 'prop-types';
import React from 'react';
import { FormGroup, ControlLabel } from 'react-bootstrap';
import { Select } from 'components/common';
import SearchUtils from 'util/SearchUtils';

import { QueryConfiguration, QuickValuesConfiguration } from 'components/widgets/configurations';

class QuickValuesHistogramWidgetEditConfiguration extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  _onIntervalChange = (value) => {
    this.props.onChange('interval', value);
  };

  render() {
    const intervalOptions = SearchUtils.histogramIntervals().map((i) => {
      return { value: i, label: i };
    });

    return (
      <fieldset>
        <QueryConfiguration {...this.props} />
        <QuickValuesConfiguration isHistogram {...this.props} />
        <FormGroup>
          <ControlLabel>Interval</ControlLabel>
          <Select options={intervalOptions}
                  value={this.props.config.interval}
                  onChange={this._onIntervalChange} />
        </FormGroup>
      </fieldset>
    );
  }
}

export default QuickValuesHistogramWidgetEditConfiguration;
