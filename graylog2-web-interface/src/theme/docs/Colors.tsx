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
import React, { useState, useCallback } from 'react';
import styled, { css } from 'styled-components';

import { Tooltip } from 'components/bootstrap';
import copyToClipboard from 'util/copyToClipboard';

const Name = styled.span`
  flex: 1;
  font-weight: bold;
  text-align: left;
`;

const Value = styled.span`
  text-align: right;
  opacity: 0.5;
  transition: opacity 150ms ease-in-out;
`;

const StyledTooltip = styled(Tooltip).attrs<{ opened: boolean }>((props) => ({
  className: props.opened ? 'in' : '', /* stylelint-disable-line */
}))(({ opened }) => css`
  display: ${opened ? 'block' : 'none'};
`);

const Wrapped = styled.div`
  flex: 1;
  position: relative;
`;

const Swatch = styled.button(({ color, theme }) => css`
  height: 60px;
  background-color: ${color};
  border: 1px solid #222;
  color: ${theme.utils.readableColor(color)};
  cursor: pointer;
  display: flex;
  flex-direction: column;
  position: relative;
  padding: 3px 6px;
  width: 100%;

  &:hover {
    ${Value} {
      opacity: 1;
    }
  }
`);

type ColorSwatchProps = {
  className?: string;
  color: string;
  copyText?: string;
  name?: string;
};

const ColorSwatch = ({
  className,
  color,
  name = '',
  copyText,
}: ColorSwatchProps) => {
  const [opened, setOpened] = useState(false);

  const copyCallback = useCallback(() => {
    copyToClipboard(copyText).then(() => {
      setOpened(true);

      setTimeout(() => {
        setOpened(false);
      }, 1000);
    });
  }, [copyText]);

  return (
    (
      <Wrapped className={className}>
        <StyledTooltip placement="top"
                       opened={opened}
                       positionTop={-32}
                       id={`${copyText ? copyText.replace(/\./g, '-') : name}-tooltip`}>
          Copied!
        </StyledTooltip>
        <Swatch color={color}
                onClick={copyCallback}>
          <Name>{name}</Name>
          <Value>{color}</Value>
        </Swatch>
      </Wrapped>
    )
  );
};

export default ColorSwatch;
