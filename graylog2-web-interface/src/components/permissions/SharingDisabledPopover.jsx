// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';
import HoverForHelp from 'components/common/HoverForHelp';

type Props = {
  type: string,
  description?: string,
};

const StyledHoverForHelp: StyledComponent<{}, ThemeInterface, typeof HoverForHelp> = styled(HoverForHelp)`
  margin-left: 8px;
`;

const SharingDisabledPopover = ({ type, description }: Props) => (
  <StyledHoverForHelp title="Sharing not possible" pullRight={false}>
    {description || `Only owners of this ${type} are allowed to share it.`}
  </StyledHoverForHelp>
);

SharingDisabledPopover.defaultProps = {
  description: undefined,
};

export default SharingDisabledPopover;
