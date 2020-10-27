// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';
import { HoverForHelp } from 'components/common';

type Props = {
  type: string,
};

const StyledHoverForHelp: StyledComponent<{}, ThemeInterface, HoverForHelp> = styled(HoverForHelp)`
  margin-left: 8px;
`;

const SharingDisabledPopover = ({ type }: Props) => (
  <StyledHoverForHelp title="Sharing not possible" pullRight={false}>
    Only owners of this {type} are allowed to share it.
  </StyledHoverForHelp>
);

export default SharingDisabledPopover;
