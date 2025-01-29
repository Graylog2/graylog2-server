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
import styled, { css } from 'styled-components';

import NumberUtils from 'util/NumberUtils';
import { Icon } from 'components/common';

const InputIO = styled.span(({ theme }) => css`
  margin-left: -5px;

  .total {
    color: ${theme.colors.gray[70]};
  }

  .value {
    font-family: ${theme.fonts.family.monospace};
  }

  .persec {
    margin-left: 3px;
  }

  .channel-direction {
    position: relative;
    left: -1px;
  }

  .channel-direction-down {
    position: relative;
    top: 1px;
  }

  .channel-direction-up {
    position: relative;
    top: -1px;
  }
`);

type Props = {
  writtenBytes1Sec: number,
  writtenBytesTotal: number,
  readBytes1Sec: number,
  readBytesTotal: number,
}

const NetworkStats = ({ writtenBytes1Sec, writtenBytesTotal, readBytes1Sec, readBytesTotal }: Props) => (
  <InputIO>
    <span className="persec">
      <Icon name="arrow_drop_down" className="channel-direction channel-direction-down" />
      <span className="rx value">{NumberUtils.formatBytes(readBytes1Sec)} </span>

      <Icon name="arrow_drop_up" className="channel-direction channel-direction-up" />
      <span className="tx value">{NumberUtils.formatBytes(writtenBytes1Sec)}</span>
    </span>

    <span className="total">
      <span> (total: </span>
      <Icon name="arrow_drop_down" className="channel-direction channel-direction-down" />
      <span className="rx value">{NumberUtils.formatBytes(readBytesTotal)} </span>

      <Icon name="arrow_drop_up" className="channel-direction channel-direction-up" />
      <span className="tx value">{NumberUtils.formatBytes(writtenBytesTotal)}</span>
      <span>)</span>
    </span>
  </InputIO>
);

export default NetworkStats;
