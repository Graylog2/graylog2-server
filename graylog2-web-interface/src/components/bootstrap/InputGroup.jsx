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
import PropTypes from 'prop-types';
// eslint-disable-next-line no-restricted-imports
import { InputGroup as BootstrapInputGroup } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import classNames from 'classnames';

const StyledBootstrapInputAddon = ({ className, ...rest }) => <BootstrapInputGroup.Addon bsClass={className} {...rest} />;

export const StyledAddon = styled(StyledBootstrapInputAddon)(({ theme }) => css`
  color: ${theme.colors.input.color};
  background-color: ${theme.colors.input.background};
  border-color: ${theme.colors.input.border};
`);

const Addon = ({ bsClass = 'input-group-addon', className, ...addonProps }) => <StyledAddon className={classNames(bsClass, className)} {...addonProps} />;

const Button = ({ bsClass = 'input-group-btn', className, ...addonProps }) => <BootstrapInputGroup.Button bsClass={classNames(bsClass, className)} {...addonProps} />;

const InputGroup = ({ bsClass = 'input-group', className, ...restProps }) => <BootstrapInputGroup bsClass={classNames(bsClass, className)} {...restProps} />;

InputGroup.Addon = Addon;
InputGroup.Button = Button;

StyledBootstrapInputAddon.propTypes = {
  className: PropTypes.string,
};

InputGroup.propTypes = {
  bsClass: PropTypes.string,
  className: PropTypes.string,
  children: PropTypes.node.isRequired,
};

Addon.propTypes = {
  bsClass: PropTypes.string,
  className: PropTypes.string,
  children: PropTypes.node.isRequired,
};

Button.propTypes = {
  bsClass: PropTypes.string,
  className: PropTypes.string,
  children: PropTypes.node.isRequired,
};

/** @component */
export default InputGroup;
