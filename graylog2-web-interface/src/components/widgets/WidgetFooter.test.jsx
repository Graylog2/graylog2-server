import React from 'react';
import { mount } from 'wrappedEnzyme';

import WidgetFooter from 'components/widgets/WidgetFooter';

describe('<WidgetFooter />', () => {
  const date = new Date(Date.UTC(1995, 12, 17, 3, 24, 0));
  let dateNowMock;

  beforeAll(() => {
    dateNowMock = jest.spyOn(Date, 'now').mockImplementation(() => new Date(Date.UTC(2018, 1, 22, 17, 35, 0)));
  });

  afterAll(() => {
    dateNowMock.mockReset();
  });

  it('should render a widget footer locked and enabled replay', () => {
    const wrapper = mount(<WidgetFooter locked
                                        onShowConfig={() => {}}
                                        onEditConfig={() => {}}
                                        onDelete={() => {}}
                                        replayHref="http://example.org"
                                        replayDisabled={false}
                                        calculatedAt={date.toISOString()}
                                        error={{}}
                                        errorMessage="" />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should render a widget footer locked and disabled replay', () => {
    const wrapper = mount(<WidgetFooter locked
                                        onShowConfig={() => {}}
                                        onEditConfig={() => {}}
                                        onDelete={() => {}}
                                        replayHref="http://example.org"
                                        replayDisabled
                                        calculatedAt={date.toISOString()}
                                        error={{}}
                                        errorMessage="" />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should render a widget footer with unlocked', () => {
    const wrapper = mount(<WidgetFooter locked={false}
                                        onShowConfig={() => {}}
                                        onEditConfig={() => {}}
                                        onDelete={() => {}}
                                        replayHref="http://example.org"
                                        replayDisabled={false}
                                        calculatedAt={date.toISOString()}
                                        error={{}}
                                        errorMessage="" />);
    expect(wrapper).toMatchSnapshot();
  });
});
