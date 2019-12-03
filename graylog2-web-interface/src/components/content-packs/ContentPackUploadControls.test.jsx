import React from 'react';
import { mount } from 'theme/enzymeWithTheme';
import 'helpers/mocking/react-dom_mock';

import ContentPackUploadControls from 'components/content-packs/ContentPackUploadControls';

describe('<ContentPackUploadControls />', () => {
  it('should render', () => {
    const wrapper = mount(<ContentPackUploadControls />);
    expect(wrapper).toMatchSnapshot();
  });
});
