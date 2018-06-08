import React from 'react';
import renderer from 'react-test-renderer';
import 'helpers/mocking/react-dom_mock';

import ContentPackUploadControls from 'components/content-packs/ContentPackUploadControls';

describe('<ContentPackUploadControls />', () => {
  it('should render', () => {
    const wrapper = renderer.create(<ContentPackUploadControls />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });
});
