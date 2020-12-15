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
import 'helpers/mocking/react-dom_mock';

import ContentPack from 'logic/content-packs/ContentPack';
import ContentPackSelection from 'components/content-packs/ContentPackSelection';
import Entity from 'logic/content-packs/Entity';

jest.mock('uuid/v4', () => jest.fn(() => 'dead-beef'));

describe('<ContentPackSelection />', () => {
  it('should render with empty content pack', () => {
    const contentPack = new ContentPack.builder().build();
    const wrapper = mount(<ContentPackSelection contentPack={contentPack} />);

    expect(wrapper).toExist();
  });

  it('should render with filled content pack', () => {
    const contentPack = ContentPack.builder()
      .name('name')
      .summary('summary')
      .description('description')
      .vendor('vendor')
      .url('http://example.com')
      .build();

    const entity = Entity.builder()
      .v('1')
      .type({ name: 'spaceship', version: '1' })
      .id('beef123')
      .data({
        title: { value: 'breq', type: 'string' },
      })
      .build();
    const entities = {
      spaceship: [entity],
    };

    const wrapper = mount(
      <ContentPackSelection contentPack={contentPack}
                            edit
                            entities={entities}
                            selectedEntities={{}} />,
    );

    expect(wrapper).toExist();
  });

  it('should update the state when filling out the form', () => {
    let resultedState;
    const changeFn = jest.fn((state) => {
      resultedState = state;
    });
    const contentPack = ContentPack.builder().build();

    const wrapper = mount(<ContentPackSelection contentPack={contentPack} onStateChange={changeFn} />);

    wrapper.find('input#name').simulate('change', { target: { name: 'name', value: 'name' } });
    wrapper.find('input#summary').simulate('change', { target: { name: 'summary', value: 'summary' } });
    wrapper.find('textarea#description').simulate('change', { target: { name: 'description', value: 'descr' } });
    wrapper.find('input#vendor').simulate('change', { target: { name: 'vendor', value: 'vendor' } });
    wrapper.find('input#url').simulate('change', { target: { name: 'url', value: 'http://url' } });

    expect(changeFn.mock.calls.length).toBe(5);
    expect(resultedState.contentPack.name).toEqual('name');
    expect(resultedState.contentPack.summary).toEqual('summary');
    expect(resultedState.contentPack.description).toEqual('descr');
    expect(resultedState.contentPack.vendor).toEqual('vendor');
    expect(resultedState.contentPack.url).toEqual('http://url');
  });

  it('should add a entity if content selection was checked', () => {
    const contentPack = {};
    const entities = {
      spaceship: [{
        title: 'breq',
        type: {
          name: 'spaceship',
          version: '1',
        },
        id: 'beef123',
      }],
    };

    const changeFn = jest.fn((newState) => {
      expect(newState.selectedEntities).toEqual(entities);
    });

    const wrapper = mount(
      <ContentPackSelection contentPack={contentPack}
                            selectedEntities={{}}
                            onStateChange={changeFn}
                            entities={entities} />,
    );

    wrapper.find('input[type="checkbox"]').at(0).simulate('change', { target: { checked: true } });

    expect(changeFn.mock.calls.length).toBe(1);
  });

  describe('with several entities', () => {
    const breq = {
      title: 'breq',
      type: {
        name: 'spaceship',
        version: '1',
      },
      id: 'beef123',
    };
    const falcon = {
      title: 'falcon',
      type: {
        name: 'spaceship',
        version: '1',
      },
      id: 'beef124',
    };
    const entities = { spaceship: [breq, falcon] };

    it('should remove a entity if content selection was unchecked', () => {
      const contentPack = {};
      const selectedEntities = { spaceship: [breq, falcon] };

      const changeFn = jest.fn((newState) => {
        expect(newState.selectedEntities).toEqual({ spaceship: [falcon] });
      });

      const wrapper = mount(
        <ContentPackSelection contentPack={contentPack}
                              selectedEntities={selectedEntities}
                              onStateChange={changeFn}
                              entities={entities} />,
      );

      wrapper.find('div.fa-stack').simulate('click');
      wrapper.find('input[type="checkbox"]').at(1).simulate('change', { target: { checked: false } });

      expect(changeFn.mock.calls.length).toBe(1);
    });

    it('should filter expandableList of content selection', () => {
      const contentPack = {};
      const wrapper = mount(
        <ContentPackSelection contentPack={contentPack}
                              entities={entities} />,
      );

      /*
       * Search for falcon
       *
      /* Open menu to show all checkboxes */
      wrapper.find('div.fa-stack').simulate('click');

      expect(wrapper.find('input[type="checkbox"]').length).toEqual(3);

      wrapper.find('input#common-search-form-query-input').simulate('change', { target: { value: 'falcon' } });
      wrapper.find('form').at(1).simulate('submit');

      expect(wrapper.find('input[type="checkbox"]').length).toEqual(2);

      /*
       * reset the search
       */
      wrapper.find("button[children='Reset']").simulate('click');
      /* Open menu to show all checkboxes */
      wrapper.find('div.fa-stack').simulate('click');

      expect(wrapper.find('input[type="checkbox"]').length).toEqual(3);
    });

    it('should validate that all fields are filled out', () => {
      const wrapper = mount(<ContentPackSelection contentPack={{}} entities={entities} />);

      wrapper.instance()._validate();
      wrapper.update();

      expect(wrapper.find({ error: 'Must be filled out.' }).find('InputDescription').length).toEqual(3);

      const wrapper2 = mount(<ContentPackSelection contentPack={{ name: 'name' }} entities={entities} />);

      wrapper2.instance()._validate();
      wrapper2.update();

      expect(wrapper2.find({ error: 'Must be filled out.' }).find('InputDescription').length).toEqual(2);

      const wrapper1 = mount(<ContentPackSelection contentPack={{ name: 'name', summary: 'summary' }}
                                                   entities={entities} />);

      wrapper1.instance()._validate();
      wrapper1.update();

      expect(wrapper1.find({ error: 'Must be filled out.' }).find('InputDescription').length).toEqual(1);

      const wrapper0 = mount(<ContentPackSelection contentPack={{ name: 'name', summary: 'summary', vendor: 'vendor' }}
                                                   entities={entities} />);

      wrapper0.instance()._validate();
      wrapper0.update();

      expect(wrapper0.find({ error: 'Must be filled out.' }).find('InputDescription').length).toEqual(0);
    });

    it('should validate that URLs only have http or https protocols', () => {
      const contentPack = { name: 'name', summary: 'summary', vendor: 'vendor' };
      const protocols = [
        // eslint-disable-next-line no-script-url
        { protocol: 'javascript:', errors: 1 },
        { protocol: 'ftp:', errors: 1 },
        { protocol: 'http:', errors: 0 },
        { protocol: 'https:', errors: 0 },
      ];

      protocols.forEach(({ protocol, errors }) => {
        contentPack.url = `${protocol}//example.org`;
        const invalidWrapper = mount(<ContentPackSelection contentPack={contentPack} entities={entities} />);

        invalidWrapper.instance()._validate();
        invalidWrapper.update();

        expect(invalidWrapper.find({ error: 'Must use a URL starting with http or https.' }).find('InputDescription')).toHaveLength(errors);
      });
    });
  });
});
