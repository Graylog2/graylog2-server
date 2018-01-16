import React from 'react';
import renderer from 'react-test-renderer';

import WidgetFooter from 'components/widgets/WidgetFooter';

describe('<WidgetFooter />', () => {
  const date = new Date('December 17, 1995 03:24:00');
  const lockWidget = true;
  const disableReplay = true;

  it('should render a widget footer locked and enabled replay', () => {
    const wrapper = renderer.create(<WidgetFooter
      locked={lockWidget}
      onShowConfig={() => {}}
      onEditConfig={() => {}}
      onDelete={() => {}}
      replayHref={'http://example.org'}
      replayDisabled={false}
      calculatedAt={date.getTime()}
      error={{}}
      errorMessage={''} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render a widget footer locked and disabled replay', () => {
    const wrapper = renderer.create(<WidgetFooter
      locked={lockWidget}
      onShowConfig={() => {}}
      onEditConfig={() => {}}
      onDelete={() => {}}
      replayHref={'http://example.org'}
      replayDisabled={disableReplay}
      calculatedAt={date.getTime()}
      error={{}}
      errorMessage={''} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render a widget footer with unlocked', () => {
    const wrapper = renderer.create(<WidgetFooter
      locked={false}
      onShowConfig={() => {}}
      onEditConfig={() => {}}
      onDelete={() => {}}
      replayHref={'http://example.org'}
      replayDisabled={false}
      calculatedAt={date.getTime()}
      error={{}}
      errorMessage={''} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });
});
