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
import * as React from 'react';
import ReactSlider from 'react-slider';
import type { ReactSliderProps } from 'react-slider';
import styled, { css } from 'styled-components';
import type { DefaultTheme } from 'styled-components';

import { Input, Tooltip } from 'components/bootstrap';
import OverlayTrigger from 'components/common/OverlayTrigger';

type Props = {
  id: string,
  label?: string,
  help?: string | React.ReactElement,
  error?: string,
  bsStyle?: 'success' | 'warning' | 'error',
  labelClassName?: string
  wrapperClassName?: string,
} & ReactSliderProps<Array<number> | number>;

const StyledSlider = styled(ReactSlider)(({ theme }: { theme: DefaultTheme }) => css`
  width: 100%;
  height: 10px;
  margin: ${theme.spacings.md} 0;
`);

const StyledThumb = styled.div(({ theme }: { theme: DefaultTheme }) => css`
  height: auto;
  min-height: 25px;
  line-height: 25px;
  width: auto;
  min-width: 25px;
  text-align: center;
  background-color: #5082bc;
  color: ${theme.colors.input.color};
  border-radius: 50%;
  cursor: grab;
  top: -5px;
`);

const Thumb = (props, state) => {
  const tooltip = <Tooltip id={`${state.valueNow}-tooltip`} show>{state.valueNow}</Tooltip>;

  return (
    <StyledThumb {...props} className={`${state.valueNow}-tooltip`}>
      <OverlayTrigger placement="top" trigger="hover" overlay={tooltip} rootClose>
        <span className="value">{state.valueNow}</span>
      </OverlayTrigger>
    </StyledThumb>
  );
};

const StyledTrack = styled.div(({ theme }: { theme: DefaultTheme }) => css`
  top: ${theme.spacings.xxs};
  bottom: 0;
  background: ${(props: any) => (props.index === 1 ? '#5082bc' : theme.colors.variant.default)};
  border-radius: 999px;
`);

const Track = (props, state) => <StyledTrack {...props} index={state.index} />;

const RangeInput = ({
  id,
  label,
  help,
  error,
  bsStyle,
  labelClassName,
  wrapperClassName,
  ...otherProps
}: Props) => {
  return (
    <Input labelClassName={labelClassName}
           id={id}
           wrapperClassName={wrapperClassName}
           help={help}
           bsStyle={bsStyle}
           error={error}
           label={label}>
      <StyledSlider renderTrack={Track}
                    renderThumb={Thumb}
                    className={error}
                    {...otherProps} />
    </Input>
  );
};

RangeInput.defaultProps = {
  label: undefined,
  help: undefined,
  error: undefined,
  bsStyle: undefined,
  labelClassName: undefined,
  wrapperClassName: undefined,
};

export default RangeInput;
