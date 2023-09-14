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
import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import { defaultUser } from 'defaultMockValues';

import ContentStreamSection from 'components/content-stream/ContentStreamSection';
import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';
import useContentStreamSettings from 'components/content-stream/hook/useContentStreamSettings';

const mockContentStreamSettings = {
  contentStreamSettings: {
    contentStreamTopics: [],
    releasesSectionEnabled: true,
    contentStreamEnabled: true,
  },
  contenStreamTags: {
    currentTag: 'open-feed',
    isLoadingTags: false,
    refetchContentStreamTag: () => Promise.resolve(),
    contentStreamTagError: undefined,
  },
  isLoadingContentStreamSettings: false,
  refetchContentStream: () => Promise.resolve(),
  onSaveContentStreamSetting: () => Promise.resolve(),
};

jest.mock('components/content-stream/hook/useContentStreamSettings', () => jest.fn(() => mockContentStreamSettings));

jest.mock('util/AppConfig', () => ({
  gl2DevMode: jest.fn(() => false),
  gl2AppPathPrefix: jest.fn(() => ''),
  isCloud: jest.fn(() => true),
  gl2ServerUrl: jest.fn(() => 'https://example.org/api/'),
  contentStream: jest.fn(() => ({ refresh_interval: 'PT168H', rss_url: 'https://example.org/rss' })),
}));

jest.mock('components/content-stream/ContentStreamNews', () => () => <div>ContentStreamNews</div>);

jest.mock('components/content-stream/ContentStreamReleasesSection', () => () => <div>ContentStreamReleasesSection</div>);

jest.mock('hooks/useCurrentUser');

describe('<ContentStreamSection>', () => {
  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
  });

  it('Render ContentStream', async () => {
    render(<ContentStreamSection />);
    const newsSection = await screen.findByText('ContentStreamNews');
    const releaseSection = await screen.findByText('ContentStreamNews');

    expect(newsSection).toBeInTheDocument();
    expect(releaseSection).toBeInTheDocument();
  });

  it('Hide ContentStream', async () => {
    asMock(useContentStreamSettings).mockReturnValue({
      ...mockContentStreamSettings,
      contentStreamSettings: {
        releasesSectionEnabled: false,
        contentStreamEnabled: false,
        contentStreamTopics: [],
      },
    });

    render(<ContentStreamSection />);

    expect(screen.queryByText('ContentStreamNews')).not.toBeInTheDocument();
    expect(screen.queryByText('ContentStreamNews')).not.toBeInTheDocument();
  });
});
