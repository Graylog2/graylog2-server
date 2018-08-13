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


  it('should filter expandableList of content selection', () => {
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
    const wrapper = mount(
      <ContentPackSelection contentPack={contentPack}
                            entities={entities} />);

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

    const wrapper = mount(<ContentPackSelection contentPack={{ }} entities={entities} />);
    wrapper.instance()._validate();
    wrapper.update();
    expect(wrapper.find('span[children="Must be filled out."]').length).toEqual(3);

    const wrapper2 = mount(<ContentPackSelection contentPack={{ name: 'name' }} entities={entities} />);
    wrapper2.instance()._validate();
    wrapper2.update();
    expect(wrapper2.find('span[children="Must be filled out."]').length).toEqual(2);

    const wrapper1 = mount(<ContentPackSelection contentPack={{ name: 'name', summary: 'summary' }} entities={entities} />);
    wrapper1.instance()._validate();
    wrapper1.update();
    expect(wrapper1.find('span[children="Must be filled out."]').length).toEqual(1);

    const wrapper0 = mount(<ContentPackSelection contentPack={{ name: 'name', summary: 'summary', vendor: 'vendor' }} entities={entities} />);
    wrapper0.instance()._validate();
    wrapper0.update();
    expect(wrapper0.find('span[children="Must be filled out."]').length).toEqual(0);
  });
});
