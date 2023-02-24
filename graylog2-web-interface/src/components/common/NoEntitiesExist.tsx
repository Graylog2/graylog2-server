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

import { Alert } from 'components/bootstrap';
import Icon from 'components/common/Icon';

const StyledIcon = styled(Icon)`
  margin-right: 5px;
`;

type Props = {
  children: React.ReactNode,
  className?: string,
};

/**
 * Component used to display a simple alert message for an empty entity in Graylog. For more complex messages that
 * explain what that entity is and a link to create a new one please use <EmptyEntity>
*/
const NoEntitiesExist = ({ children, className }: Props) => (
  <Alert className={`${className ?? ''} no-bm`}><StyledIcon name="info-circle" />{children}</Alert>
);

NoEntitiesExist.propTypes = {
  children: PropTypes.oneOfType([PropTypes.node]),
  className: PropTypes.string,
};

NoEntitiesExist.defaultProps = {
  children: 'No entities exist.',
  className: undefined,
};

export default NoEntitiesExist;
