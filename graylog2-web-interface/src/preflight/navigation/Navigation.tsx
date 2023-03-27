import React from 'react';
import type { TextProps } from '@mantine/core';
import type { DefaultTheme } from 'styled-components';
import styled, { css } from 'styled-components';

import HelpMenu from 'preflight/navigation/HelpMenu';
import { Group, Header, Text } from 'preflight/common';

import NavigationBrand from './NavigationBrand';
import ThemeModeToggle from './ThemeModeToggle';

type StyledMantineTextProps = TextProps & {
  theme: DefaultTheme,
  children: React.ReactNode,
};
const GraylogHeader = styled(Header)(({ theme }) => css`
  background-color: ${theme.colors.global.contentBackground};
  border: 1px solid ${theme.colors.variant.lighter.default};
  padding: ${theme.spacings.md};
`);
const NavigationContainer = styled.div`
  display: flex;
  justify-content : space-between;
  align-items: center;
`;
const StyledText = styled(Text)<React.PropsWithChildren<StyledMantineTextProps>>(({ theme }: StyledMantineTextProps) => css`
  color: ${theme.mode === 'teint' ? theme.colors.brand.concrete : 'white'};
`);
const Navigation = () => (
  <GraylogHeader height={80}>
    <NavigationContainer>
      <Group position="left" spacing="xs">
        <NavigationBrand />
        <StyledText weight={700} size="lg" mr={1}>Graylog Initial Setup</StyledText>
      </Group>
      <Group position="right">
        <HelpMenu />
        <ThemeModeToggle />
      </Group>
    </NavigationContainer>
  </GraylogHeader>
);

export default Navigation;
