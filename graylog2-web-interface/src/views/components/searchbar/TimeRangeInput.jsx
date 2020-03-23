// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import type { SearchesConfig } from 'components/search/SearchConfig';
import RelativeTimeRangeSelector from 'views/components/searchbar/RelativeTimeRangeSelector';
import AbsoluteTimeRangeSelector from 'views/components/searchbar/AbsoluteTimeRangeSelector';
import KeywordTimeRangeSelector from 'views/components/searchbar/KeywordTimeRangeSelector';

type Props = {
  disabled: boolean,
  type: string,
  config: SearchesConfig,
};

export default function TimeRangeInput({ disabled, type, config }: Props) {
  switch (type) {
    case 'relative':
      return (
        <RelativeTimeRangeSelector disabled={disabled}
                                   config={config} />
      );
    case 'absolute':
      return (
        <AbsoluteTimeRangeSelector disabled={disabled} />
      );
    case 'keyword':
      return (
        <KeywordTimeRangeSelector disabled={disabled} />
      );
    default:
      throw new Error(`Unsupported time range type ${type}`);
  }
}

TimeRangeInput.propTypes = {
  config: PropTypes.shape({
    relative_timerange_options: PropTypes.objectOf(PropTypes.string).isRequired,
  }).isRequired,
  disabled: PropTypes.bool,
  onChange: PropTypes.func.isRequired,
  timerange: PropTypes.object.isRequired,
};

TimeRangeInput.defaultProps = {
  disabled: false,
};
