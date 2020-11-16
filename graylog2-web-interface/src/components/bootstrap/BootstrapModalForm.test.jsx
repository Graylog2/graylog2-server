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

  it('shows modal form when triggering open', () => {
    const wrapper = mount(
      <BootstrapModalForm title="Sample Form">
        {children}
      </BootstrapModalForm>,
    );

    expect(wrapper).not.toContainReact(children);

    wrapper.instance().open();
    wrapper.update();

    expect(wrapper).toContainReact(children);
  });

  it('hides modal form when triggering close', () => {
    const wrapper = mount(
      <BootstrapModalForm title="Sample Form">
        {children}
      </BootstrapModalForm>,
    );

    expect(wrapper).not.toContainReact(children);

    wrapper.instance().open();
    wrapper.update();

    expect(wrapper).toContainReact(children);
    expect(wrapper.find('Modal').at(0)).toHaveProp('show', true);

    wrapper.instance().close();
    wrapper.update();

    expect(wrapper.find('Modal').at(0)).toHaveProp('show', false);
  });

  it('calls onOpen when triggering open', (done) => {
    const wrapper = mount(
      <BootstrapModalForm show={false} title="Sample Form" onModalOpen={done}>
        {children}
      </BootstrapModalForm>,
    );

    wrapper.instance().open();
  });

  it('calls onClose when triggering close', (done) => {
    const wrapper = mount(
      <BootstrapModalForm show title="Sample Form" onModalClose={done}>
        {children}
      </BootstrapModalForm>,
    );

    wrapper.instance().close();
  });

  it('does not show modal form when show property is false', () => {
    const wrapper = mount(
      <BootstrapModalForm show={false} title="Sample Form">
        {children}
      </BootstrapModalForm>,
    );

    expect(wrapper).not.toContainReact(children);
  });

  it('shows modal form when show property is true', () => {
    const wrapper = mount(
      <BootstrapModalForm show title="Sample Form">
        {children}
      </BootstrapModalForm>,
    );

    expect(wrapper).toContainReact(children);
  });

  it('calls onSubmit when form is submitted', (done) => {
    const onHide = jest.fn();
    const wrapper = mount(
      <BootstrapModalForm show title="Sample Form" onSubmitForm={() => done()} onHide={onHide}>
        {children}
      </BootstrapModalForm>,
    );
    const form = wrapper.find('form');

    form.simulate('submit');

    expect(onHide).not.toHaveBeenCalled();
  });

  it('calls onCancel when form is cancelled', (done) => {
    const onSubmitForm = jest.fn();
    const wrapper = mount(
      <BootstrapModalForm show title="Sample Form" onSubmitForm={onSubmitForm} onCancel={done}>
        {children}
      </BootstrapModalForm>,
    );
    const cancel = wrapper.find('button[children="Cancel"]');

    cancel.simulate('click');

    expect(onSubmitForm).not.toHaveBeenCalled();
  });
});
