import React from 'react';
import PropTypes from 'prop-types';

import RelativeTimeRangeSelector from 'views/components/searchbar/RelativeTimeRangeSelector';
import AbsoluteTimeRangeSelector from 'views/components/searchbar/AbsoluteTimeRangeSelector';
import KeywordTimeRangeSelector from 'views/components/searchbar/KeywordTimeRangeSelector';

export default function TimeRangeInput({ disabled, rangeType, rangeParams, config, onChange, onSubmit }) {
  switch (rangeType) {
    case 'relative':
      return <RelativeTimeRangeSelector disabled={disabled} value={rangeParams} config={config} onChange={onChange} onSubmit={onSubmit} />;
    case 'absolute':
      return <AbsoluteTimeRangeSelector disabled={disabled} value={rangeParams} onChange={onChange} onSubmit={onSubmit} />;
    case 'keyword':
      return <KeywordTimeRangeSelector disabled={disabled} value={rangeParams} onChange={onChange} onSubmit={onSubmit} />;
    default:
      throw new Error(`Unsupported range type ${rangeType}`);
  }
}

TimeRangeInput.propTypes = {
  config: PropTypes.shape({
    relative_timerange_options: PropTypes.objectOf(PropTypes.string).isRequired,
  }).isRequired,
  disabled: PropTypes.bool,
  onChange: PropTypes.func.isRequired,
  rangeParams: PropTypes.object.isRequired,
  rangeType: PropTypes.string.isRequired,
};

TimeRangeInput.defaultProps = {
  disabled: false,
};
