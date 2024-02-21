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

import OriginalButtonGroup from './ButtonGroup';

const Container = styled.div`
  display: inline-block;
`;

const StyledButtonToolbar = styled(OriginalButtonGroup)`
  gap: 0.25em;
`;

const ButtonToolbar = ({ className, ...rest }: React.ComponentProps<typeof StyledButtonToolbar> & { className?: string }) => (
  <Container role="toolbar" className={className}>
    <StyledButtonToolbar {...rest} />
  </Container>
);

ButtonToolbar.defaultProps = {
  className: undefined,
};

/** @component */
export default ButtonToolbar;
