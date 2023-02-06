import * as React from 'react';
import ReactSlider from 'react-slider';
import type { ReactSliderProps } from 'react-slider';
import styled, { css } from 'styled-components';
import type { DefaultTheme } from 'styled-components';

import { Input } from 'components/bootstrap';

type Props = {
  label?: string,
  help?: string | React.ReactElement,
  error?: string,
  bsStyle?: 'success' | 'warning' | 'error',
  labelClassName?: string
  wrapperClassName?: string,
} & ReactSliderProps<Array<number>>;

const StyledSlider = styled(ReactSlider)(({ theme }: { theme: DefaultTheme }) => css`
    width: 100%;
    height: 10px;
    margin: ${theme.spacings.md} 0;
`);

const StyledThumb = styled.div(({ theme }: { theme: DefaultTheme }) => css`
    height: 25px;
    line-height: 25px;
    width: 25px;
    text-align: center;
    background-color: #5082bc;
    color: ${theme.colors.variant.textDefault};
    border-radius: 50%;
    cursor: grab;
    top: -5px;
`);

const Thumb = (props, state) => <StyledThumb {...props}>{state.valueNow}</StyledThumb>;

const StyledTrack = styled.div(({ theme }: { theme: DefaultTheme }) => css`
    top: ${theme.spacings.xxs};
    bottom: 0;
    background: ${(props: any) => (props.index === 1 ? '#5082bc' : theme.colors.variant.default)};
    border-radius: 999px;
  `);

const Track = (props, state) => <StyledTrack {...props} index={state.index} />;

const RangeInput = ({
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
