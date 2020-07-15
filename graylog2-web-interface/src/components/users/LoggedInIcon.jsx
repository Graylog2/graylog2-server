// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import { type ThemeInterface } from 'theme';
import { Icon } from 'components/common';

const StyledIcon: StyledComponent<{active?: boolean}, ThemeInterface, Icon> = styled(Icon)(({ theme, active }) => `
  color: ${active ? theme.colors.variant.success : theme.colors.variant.danger};
`);

const LoggedInIcon = ({ active }: { active: boolean }) => <StyledIcon name="circle" active={active} />;

export default LoggedInIcon;
