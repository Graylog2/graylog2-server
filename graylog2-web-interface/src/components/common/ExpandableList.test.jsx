import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'wrappedEnzyme';
import 'helpers/mocking/react-dom_mock';

import ExpandableList from 'components/common/ExpandableList';
import ExpandableListItem from 'components/common/ExpandableListItem';

describe('<ExpandableList />', () => {
  it('should render with no children', () => {
    const wrapper = renderer.create(<ExpandableList />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with a Item', () => {
    const wrapper = renderer.create(<ExpandableList>
      <ExpandableListItem header="Wheel of time">
        <span>Edmonds Field</span>
      </ExpandableListItem>
    </ExpandableList>);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with a nested ExpandableList', () => {
    const wrapper = renderer.create(<ExpandableList>
      <ExpandableListItem expandable expanded header="Wheel of time">
        <ExpandableList>
          <ExpandableListItem expandable expanded={false} header="Edmonds Field" />
        </ExpandableList>
      </ExpandableListItem>
    </ExpandableList>);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });


  it('should expand a expandable list item', () => {
    const wrapper = mount(<ExpandableList>
      <ExpandableListItem expandable header="Wheel of time" readOnly>
        <ExpandableList>
          <ExpandableListItem header="Edmonds Field" readOnly />
        </ExpandableList>
      </ExpandableListItem>
    </ExpandableList>);
    expect(wrapper.find('span.header').length).toBe(1);
    wrapper.find('div.fa-stack').simulate('click');
    expect(wrapper.find('span.header').length).toBe(2);
  });

  it('should select a selectable list item', () => {
    const checkFn = jest.fn();
    const wrapper = mount(<ExpandableList>
      <ExpandableListItem expanded header="Wheel of time" readOnly>
        <ExpandableList>
          <ExpandableListItem expanded selectable header="Edmonds Field" onChange={checkFn} />
        </ExpandableList>
      </ExpandableListItem>
    </ExpandableList>);
    wrapper.find('input[type="checkbox"]').at(1).simulate('change', { target: { checked: true } });
    expect(checkFn.mock.calls.length).toBe(1);
  });
});
