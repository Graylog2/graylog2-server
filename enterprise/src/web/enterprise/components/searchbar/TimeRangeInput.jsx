import React from 'react';
import PropTypes from 'prop-types';

import RelativeTimeRangeSelector from 'enterprise/components/searchbar/RelativeTimeRangeSelector';
import AbsoluteTimeRangeSelector from 'enterprise/components/searchbar/AbsoluteTimeRangeSelector';
import KeywordTimeRangeSelector from 'enterprise/components/searchbar/KeywordTimeRangeSelector';

export default function TimeRangeInput({ rangeType, rangeParams, config, onChange }) {
  switch (rangeType) {
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

TimeRangeInput.propTypes = {
  config: PropTypes.shape({
    relative_timerange_options: PropTypes.objectOf(PropTypes.string).isRequired,
  }).isRequired,
  onChange: PropTypes.func.isRequired,
  rangeParams: PropTypes.object.isRequired,
  rangeType: PropTypes.string.isRequired,
};
