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
import styled from 'styled-components';
import { Collapse } from '@mantine/core';

import useDisclosure from 'util/hooks/useDisclosure';

import Icon from './Icon';

const Container = styled.div`
  margin-bottom: ${({ theme }) => theme.spacings.md};
`;

const Toggle = styled.button`
  background: none;
  border: none;
  padding: ${({ theme }) => theme.spacings.xxs} 0;
  cursor: pointer;
  font-size: inherit;
  color: inherit;
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacings.xs};
`;

type Props = {
  label: string;
  defaultOpen?: boolean;
  children: React.ReactNode;
};

const Collapsible = ({ label, children, defaultOpen = false }: Props) => {
  const [opened, { toggle }] = useDisclosure(defaultOpen);

  return (
    <Container>
      <Toggle type="button" onClick={toggle} aria-expanded={opened}>
        <Icon name={opened ? 'keyboard_arrow_down' : 'chevron_right'} />
        {label}
      </Toggle>
      <Collapse in={opened}>{children}</Collapse>
    </Container>
  );
};

export default Collapsible;
