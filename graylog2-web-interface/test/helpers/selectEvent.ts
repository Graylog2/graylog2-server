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

import userEvent from '@testing-library/user-event';
import selectEvent from 'react-select-event';
import { screen, within } from 'wrappedTestingLibrary';

/*
 * This file contains helper methods, which replace the `react-select-event` methods.
 * They are useful when interacting with the `react-select` select component.
 */

const clearAll = (container: HTMLElement, selectClassName: string) => {
  const clearIcons = container.querySelectorAll(`.${selectClassName} svg[aria-hidden="true"]`);
  userEvent.click(clearIcons[clearIcons.length - 1]);
};

const customCreate = (element: HTMLElement, option: string) =>
  selectEvent.create(element, option, { container: document.body });

const customSelect = (element: HTMLElement, optionOrOptions: string | Array<string> | RegExp) =>
  selectEvent.select(element, optionOrOptions, { container: document.body });

const findSelectInput = (name: string, config?: { container: HTMLElement }) => {
  const queryRoot = config?.container ? within(config.container) : screen;

  return queryRoot.findByRole('combobox', { name: new RegExp(name, 'i') });
};
const findOption = async (
  selectName: string,
  optionName: (string | RegExp) | Array<string | RegExp>,
  config?: { container: HTMLElement },
) => {
  const input = await findSelectInput(selectName, config);
  selectEvent.openMenu(input);
  const optionNames = Array.isArray(optionName) ? optionName : [optionName];

  return Promise.all(optionNames.map((name) => screen.findByRole('option', { name: new RegExp(name, 'i') })));
};

// The select name should ideally be the HTML label. If there is no label, you can use the placeholder text.
const selectOption = async (
  selectName: string,
  optionName: (string | RegExp) | Array<string | RegExp>,
  config?: { container: HTMLElement },
) => {
  const input = await findSelectInput(selectName, config);
  const optionNames = Array.isArray(optionName) ? optionName : [optionName];

  optionNames.forEach((name) => {
    userEvent.type(input, `{meta>}{a}{/meta}{backspace}${name}{enter}`);
  });
};

export default {
  clearAll,
  openMenu: selectEvent.openMenu,
  create: customCreate,
  select: customSelect,
  selectOption,
  findSelectInput,
  findOption,
};
