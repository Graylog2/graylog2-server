import React from 'react';
import PropTypes from 'prop-types';

import RelativeTimeRangeSelector from 'views/components/searchbar/RelativeTimeRangeSelector';
import AbsoluteTimeRangeSelector from 'views/components/searchbar/AbsoluteTimeRangeSelector';
import KeywordTimeRangeSelector from 'views/components/searchbar/KeywordTimeRangeSelector';
import DisabledTimeRangeSelector from './DisabledTimeRangeSelector';

export default function TimeRangeOverrideInput({ rangeType, rangeParams, config, onChange }) {
  switch (rangeType) {
    case undefined:
      return <DisabledTimeRangeSelector />;
    case 'relative':
      return <RelativeTimeRangeSelector value={rangeParams} config={config} onChange={onChange} />;
    case 'absolute':
      return <AbsoluteTimeRangeSelector value={rangeParams} onChange={onChange} />;
    case 'keyword':
      return <KeywordTimeRangeSelector value={rangeParams} onChange={onChange} />;
    default:
      throw new Error(`Unsupported range type ${rangeType}`);
  }
}

TimeRangeOverrideInput.propTypes = {
  config: PropTypes.shape({
    relative_timerange_options: PropTypes.objectOf(PropTypes.string).isRequired,
  }).isRequired,
  onChange: PropTypes.func.isRequired,
  rangeParams: PropTypes.object.isRequired,
  rangeType: PropTypes.string.isRequired,
};
