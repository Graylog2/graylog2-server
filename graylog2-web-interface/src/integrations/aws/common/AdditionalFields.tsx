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
import React from 'react';
import styled from 'styled-components';

import { Collapsible } from 'components/common';

const Container = styled.div`
  margin: 0 0 35px;
`;

const Content = styled.div`
  padding: 0 100px 0 25px;
`;

type AdditionalFieldsProps = {
  children: any;
  title: string;
  visible?: boolean;
  className?: string;
};

const AdditionalFields = ({ children, className = undefined, title, visible = false }: AdditionalFieldsProps) => (
  <Container className={className}>
    <Collapsible label={title} defaultOpen={visible}>
      <Content>{children}</Content>
    </Collapsible>
  </Container>
);

export default AdditionalFields;
