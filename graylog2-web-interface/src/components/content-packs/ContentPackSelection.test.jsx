import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';
import 'helpers/mocking/react-dom_mock';

import ContentPackSelection from 'components/content-packs/ContentPackSelection';

describe('<ContentPackSelection />', () => {
  it('should render with empty content pack', () => {
    const contentPack = {};
    const wrapper = renderer.create(<ContentPackSelection contentPack={contentPack} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with filled content pack', () => {
    const contentPack = {
      name: 'name',
      summary: 'summmary',
      description: 'descr',
      vendor: 'vendor',
      url: 'http://example.com',
    };

    const entities = {
      spaceship: [{
        title: 'breq',
        type: 'spaceship',
        id: 'beef123',
      }],
    };

    const wrapper = renderer.create(
      <ContentPackSelection contentPack={contentPack}
                            entities={entities}
                            selectedEntities={{}}
      />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should update the state when filling out the form', () => {
    let called = 0;
    const changeFn = jest.fn((state) => {
      const contentPack = {
        name: 'name',
        summary: 'summary',
        description: 'descr',
        vendor: 'vendor',
        url: 'url',
      };
      called += 1;
      if (called === Object.keys(contentPack).length) {
        expect(state.contentPack).toEqual(contentPack);
      }
    });
    const contentPack = {};
    const wrapper = mount(<ContentPackSelection contentPack={contentPack} onStateChange={changeFn} />);
    wrapper.find('input#name').simulate('change', { target: { name: 'name', value: 'name' } });
    wrapper.find('input#summary').simulate('change', { target: { name: 'summary', value: 'summary' } });
    wrapper.find('textarea#description').simulate('change', { target: { name: 'description', value: 'descr' } });
    wrapper.find('input#vendor').simulate('change', { target: { name: 'vendor', value: 'vendor' } });
    wrapper.find('input#url').simulate('change', { target: { name: 'url', value: 'url' } });
    expect(changeFn.mock.calls.length).toBe(5);
  });

  it('should add a entity if content selection was checked', () => {
    const contentPack = {};
    const entities = {
      spaceship: [{
        title: 'breq',
        type: 'spaceship',
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
                            entities={entities}
      />);
    wrapper.find('input[type="checkbox"]').at(0).simulate('change', { target: { checked: true } });
    expect(changeFn.mock.calls.length).toBe(1);
  });

  it('should remove a entity if content selection was unchecked', () => {
    const contentPack = {};
    const breq = {
      title: 'breq',
      type: 'spaceship',
      id: 'beef123',
    };
    const falcon = {
      title: 'falcon',
      type: 'spaceship',
      id: 'beef124',
    };
    const entities = { spaceship: [breq, falcon] };
    const selectedEntities = { spaceship: [breq, falcon] };

    const changeFn = jest.fn((newState) => {
      expect(newState.selectedEntities).toEqual({ spaceship: [falcon] });
    });

    const wrapper = mount(
      <ContentPackSelection contentPack={contentPack}
                            selectedEntities={selectedEntities}
                            onStateChange={changeFn}
                            entities={entities}
      />);
    wrapper.find('div.fa-stack').simulate('click');
    wrapper.find('input[type="checkbox"]').at(1).simulate('change', { target: { checked: false } });
    expect(changeFn.mock.calls.length).toBe(1);
  });
});
