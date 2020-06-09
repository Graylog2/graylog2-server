// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';
import { DropdownButton } from 'components/graylog';
import { Icon } from 'components/common';

const StyledDropdownButton: StyledComponent<{}, void, DropdownButton> = styled(DropdownButton)`
  padding: 6px 7px;
  margin-right: 5px;
`;

type Props = {
  onSelect: (newType: string) => void,
  children: React.Node,
  disabled?: boolean,
};

const TimeRangeDropdownButton = ({ onSelect, children, disabled, ...rest }: Props) => (
  <StyledDropdownButton {...rest}
                        bsStyle="info"
                        disabled={disabled}
                        id="timerange-type"
                        title={<Icon name="clock" />}
                        onSelect={onSelect}>
    {children}
  </StyledDropdownButton>
);

TimeRangeDropdownButton.defaultProps = {
  disabled: false,
};

export default TimeRangeDropdownButton;
