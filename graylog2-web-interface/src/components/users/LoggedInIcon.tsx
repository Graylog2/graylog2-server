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
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Icon } from 'components/common';

type Props = {
  active: boolean,
  showReadableSuffix?: boolean,
};

const Wrapper = styled.div<{ active: boolean }>(({ theme, active }) => `
  color: ${active ? theme.colors.variant.success : theme.colors.variant.default};
  display: inline-block;
`);

const LoggedInIcon = ({ active, showReadableSuffix, ...rest }: Props) => (
  <>
    <Wrapper active={active}>
      <Icon {...rest} name={active ? 'check-circle' : 'times-circle'} />
    </Wrapper>
    {showReadableSuffix && (active ? ' yes' : ' no')}
  </>
);

LoggedInIcon.propTypes = {
  active: PropTypes.bool.isRequired,
  showReadableSuffix: PropTypes.bool,
};

LoggedInIcon.defaultProps = {
  showReadableSuffix: false,
};

export default LoggedInIcon;
