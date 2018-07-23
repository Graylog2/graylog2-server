import React from 'react';
import renderer from 'react-test-renderer';
import { mount } from 'enzyme';
import 'helpers/mocking/react-dom_mock';

import ContentPackInstallations from 'components/content-packs/ContentPackInstallations';

describe('<ContentPackInstallations />', () => {
  const installations = [
    {
      _id: '5b55b8b73d274645e49f7eec',
      content_pack_id: '82d59d98-7440-ab74-9857-36b2f1b7ab8a',
      content_pack_revision: 1,
      parameters: {
        SOURCE: {
          type: 'string',
          value: 'hulud.com.uk',
        },
      },
      entities: [
        {
          id: '5b55b8b73d274645e49f7eea',
          type: 'input'
        },
      ],
      comment: 'The fake input',
      created_at: '2018-07-23T11:15:03.871Z',
      created_by: 'kmerz',
    },
  ];

  it('should render without installations', () => {
    const wrapper = renderer.create(<ContentPackInstallations />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with empty installations', () => {
    const wrapper = renderer.create(<ContentPackInstallations installations={[]} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should render with a installation', () => {
    const wrapper = renderer.create(<ContentPackInstallations installations={installations} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('should uninstall a installation', () => {
    const uninstallFn = jest.fn((contentPackId, installId) => {
      expect(contentPackId).toEqual(installations[0].content_pack_id);
      expect(installId).toEqual(installations[0]._id);
    });
    const wrapper = mount(<ContentPackInstallations installations={installations} onUninstall={uninstallFn} />);
    wrapper.find('button[children="Uninstall"]').simulate('click');
    expect(uninstallFn.mock.calls.length).toBe(1);
  });
});
