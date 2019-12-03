import React from 'react';
import { mount } from 'theme/enzymeWithTheme';
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
          name: 'string',
          value: 'hulud.com.uk',
        },
      },
      entities: [
        {
          id: '5ba38df33d274660f0b94118',
          type: {
            name: 'input',
            version: '1',
          },
          content_pack_entity_id: '5b6d973d3d274645572a4318',
          title: 'hulud.net',
        },
      ],
      comment: 'The fake input',
      created_at: '2018-07-23T11:15:03.871Z',
      created_by: 'kmerz',
    },
  ];

  it('should render without installations', () => {
    const wrapper = mount(<ContentPackInstallations />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should render with empty installations', () => {
    const wrapper = mount(<ContentPackInstallations installations={[]} />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should render with a installation', () => {
    const wrapper = mount(<ContentPackInstallations installations={installations} />);
    expect(wrapper).toMatchSnapshot();
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
