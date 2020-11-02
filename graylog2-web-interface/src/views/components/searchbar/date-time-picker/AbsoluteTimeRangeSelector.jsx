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
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import type { StyledComponent } from 'styled-components';

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

const AbsoluteTimeRangeSelector = ({ disabled, originalTimeRange }: Props) => {
  return (
    <AbsoluteWrapper>
      <RangeWrapper>
        <AbsoluteRangeField from
                            originalTimeRange={originalTimeRange}
                            disabled={disabled} />
      </RangeWrapper>

      <IconWrap>
        <Icon name="arrow-right" />
      </IconWrap>

      <RangeWrapper>
        <AbsoluteRangeField from={false}
                            originalTimeRange={originalTimeRange}
                            disabled={disabled} />
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
