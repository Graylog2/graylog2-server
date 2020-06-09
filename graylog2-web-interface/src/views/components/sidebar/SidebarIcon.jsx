// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';
import { Icon } from 'components/common';

type Props = {
  title: string,
  name: string,
};

const IconWrapper: StyledComponent<{}, {}, HTMLDivElement> = styled.div`
  width: 25px;
  text-align: center;
  font-size: 20px;
  cursor: pointer;
`;

const SidebarIcon = ({ name, title }: Props) => (
  <IconWrapper title={title}>
    <Icon name={name} />
  </IconWrapper>
);


export default SidebarIcon;
