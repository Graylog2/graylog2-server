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
// eslint-disable-next-line no-restricted-imports
import { InputGroup as BootstrapInputGroup } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import classNames from 'classnames';

type StyledBootstrapInputAddonProps = {
  className?: string;
};

const StyledBootstrapInputAddon = ({
  className,
  ...rest
}: StyledBootstrapInputAddonProps) => <BootstrapInputGroup.Addon bsClass={className} {...rest} />;

export const StyledAddon = styled(StyledBootstrapInputAddon)(({ theme }) => css`
  color: ${theme.colors.input.color};
  background-color: ${theme.colors.input.background};
  border-color: ${theme.colors.input.border};
`);

type AddonProps = {
  bsClass?: string;
  className?: string;
  children: React.ReactNode;
};

const Addon = ({
  bsClass = 'input-group-addon',
  className,
  ...addonProps
}: AddonProps) => <StyledAddon className={classNames(bsClass, className)} {...addonProps} />;

type ButtonProps = {
  bsClass?: string;
  className?: string;
  children: React.ReactNode;
};

const Button = ({
  bsClass = 'input-group-btn',
  className,
  ...addonProps
}: ButtonProps) => <BootstrapInputGroup.Button bsClass={classNames(bsClass, className)} {...addonProps} />;

type InputGroupProps = React.ComponentProps<typeof BootstrapInputGroup> & {
  bsClass?: string;
  className?: string;
  children: React.ReactNode;
};

const InputGroup = ({
  bsClass = 'input-group',
  className,
  ...restProps
}: InputGroupProps) => <BootstrapInputGroup bsClass={classNames(bsClass, className)} {...restProps} />;

InputGroup.Addon = Addon;
InputGroup.Button = Button;

/** @component */
export default InputGroup;
