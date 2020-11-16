// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import type { StyledComponent } from 'styled-components';
import moment from 'moment';

import { Icon } from 'components/common';

import AbsoluteRangeField from './AbsoluteRangeField';

type Props = {
  disabled: boolean,
  originalTimeRange: {
    from: string,
    to: string,
  },
};

const AbsoluteWrapper: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  display: flex;
  align-items: stretch;
  justify-content: space-around;
`;

const RangeWrapper: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  flex: 4;
  align-items: center;
  min-height: 290px;
`;

const IconWrap: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  flex: 0.75;
  display: flex;
  align-items: center;
  justify-content: center;
`;

const AbsoluteTimeRangeSelector = ({ disabled, limitDuration, originalTimeRange, currentTimerange, setDisableApply }: Props) => {
  return (
    <AbsoluteWrapper>
      <RangeWrapper>
        <AbsoluteRangeField from
                            originalTimeRange={originalTimeRange}
                            disabled={disabled}
                            limitDuration={limitDuration}
                            currentTimerange={currentTimerange}
                            setDisableApply={setDisableApply} />
      </RangeWrapper>

      <IconWrap>
        <Icon name="arrow-right" />
      </IconWrap>

      <RangeWrapper>
        <AbsoluteRangeField from={false}
                            originalTimeRange={originalTimeRange}
                            disabled={disabled}
                            limitDuration={limitDuration}
                            currentTimerange={currentTimerange}
                            setDisableApply={setDisableApply} />
      </RangeWrapper>
    </AbsoluteWrapper>
  );
};

AbsoluteTimeRangeSelector.propTypes = {
  disabled: PropTypes.bool,
  originalTimeRange: PropTypes.shape({ from: PropTypes.string, to: PropTypes.string }).isRequired,
};

AbsoluteTimeRangeSelector.defaultProps = {
  disabled: false,
};

export default AbsoluteTimeRangeSelector;
