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

const StyledBootstrapInputAddon = ({ className, ...rest }) => {
  return <BootstrapInputGroup.Addon bsClass={className} {...rest} />;
};

export const StyledAddon = styled(StyledBootstrapInputAddon)(({ theme }) => css`
  color: ${theme.colors.input.color};
  background-color: ${theme.colors.input.background};
  border-color: ${theme.colors.input.border};
`);

const Addon = ({ bsClass, className, ...addonProps }) => {
  return <StyledAddon className={classNames(bsClass, className)} {...addonProps} />;
};

const Button = ({ bsClass, className, ...addonProps }) => {
  return <BootstrapInputGroup.Button bsClass={classNames(bsClass, className)} {...addonProps} />;
};

const InputGroup = ({ bsClass, className, ...restProps }) => {
  return <BootstrapInputGroup bsClass={classNames(bsClass, className)} {...restProps} />;
};

InputGroup.Addon = Addon;
InputGroup.Button = Button;

StyledBootstrapInputAddon.propTypes = {
  className: PropTypes.string,
};

StyledBootstrapInputAddon.defaultProps = {
  className: undefined,
};

InputGroup.propTypes = {
  bsClass: PropTypes.string,
  className: PropTypes.string,
  children: PropTypes.node.isRequired,
};

InputGroup.defaultProps = {
  bsClass: 'input-group',
  className: undefined,
};

Addon.propTypes = {
  bsClass: PropTypes.string,
  className: PropTypes.string,
  children: PropTypes.node.isRequired,
};

Addon.defaultProps = {
  bsClass: 'input-group-addon',
  className: undefined,
};

Button.propTypes = {
  bsClass: PropTypes.string,
  className: PropTypes.string,
  children: PropTypes.node.isRequired,
};

Button.defaultProps = {
  bsClass: 'input-group-btn',
  className: undefined,
};

/** @component */
export default InputGroup;
