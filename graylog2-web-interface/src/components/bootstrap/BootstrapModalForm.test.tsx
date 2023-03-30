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
import { mount } from 'wrappedEnzyme';

import BootstrapModalForm from './BootstrapModalForm';

describe('BootstrapModalForm', () => {
  const children = <span>42</span>;

  const renderModalForm = (
    show: boolean = false,
    onSubmitForm: () => void = () => {},
    onCancel: () => void = () => {},
  ) => (
    <BootstrapModalForm title="Sample Form"
                        show={show}
                        onSubmitForm={onSubmitForm}
                        onCancel={onCancel}>
      {children}
    </BootstrapModalForm>
  );

  it('does not show modal form when show property is false', () => {
    const wrapper = mount(renderModalForm(false));

    expect(wrapper).not.toContainReact(children);
  });

  it('shows modal form when show property is true', () => {
    const wrapper = mount(renderModalForm(true));

    expect(wrapper).toContainReact(children);
  });

  it('calls onSubmit when form is submitted', () => {
    const onCancel = jest.fn();
    const onSubmit = jest.fn();
    const wrapper = mount(
      renderModalForm(true, onSubmit, onCancel),
    );
    const form = wrapper.find('form');

    form.simulate('submit');

    expect(onCancel).not.toHaveBeenCalled();
  });

  it('calls onCancel when form is cancelled', () => {
    const onCancel = jest.fn();
    const onSubmit = jest.fn();
    const wrapper = mount(
      renderModalForm(true, onSubmit, onCancel),
    );
    const cancel = wrapper.find('button[children="Cancel"]');

    cancel.simulate('click');

    expect(onSubmit).not.toHaveBeenCalled();
  });
});
