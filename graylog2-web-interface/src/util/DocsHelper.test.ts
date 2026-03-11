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
import asMock from 'helpers/mocking/AsMock';
import AppConfig from 'util/AppConfig';

jest.mock('util/AppConfig');

describe('DocsHelper', () => {
  const loadDocsHelper = async () => {
    let docsHelper;

    await jest.isolateModulesAsync(async () => {
      ({ default: docsHelper } = await import('./DocsHelper'));
    });

    return docsHelper;
  };

  it('prefixes page URLs with default base URL', async () => {
    const DocsHelper = await loadDocsHelper();

    expect(DocsHelper.toString(DocsHelper.PAGES.ALERTS)).toEqual(
      'https://go2docs.graylog.org/current/interacting_with_your_log_data/alerts.html',
    );
    expect(DocsHelper.toString(DocsHelper.PAGES.LICENSE)).toEqual(
      'https://go2docs.graylog.org/current/setting_up_graylog/operations_license_management.html',
    );
    expect(DocsHelper.toString(DocsHelper.PAGES.AUTHENTICATORS)).toEqual(
      'https://go2docs.graylog.org/current/setting_up_graylog/user_authentication.htm',
    );
  });

  it('applies custom url prefix', async () => {
    asMock(AppConfig.branding).mockReturnValue({
      help_url: 'https://www.example.com/docs',
    });
    const DocsHelper = await loadDocsHelper();

    expect(DocsHelper.toString(DocsHelper.PAGES.ALERTS)).toEqual(
      'https://www.example.com/docs/interacting_with_your_log_data/alerts.html',
    );
    expect(DocsHelper.toString(DocsHelper.PAGES.LICENSE)).toEqual(
      'https://www.example.com/docs/setting_up_graylog/operations_license_management.html',
    );
    expect(DocsHelper.toString(DocsHelper.PAGES.AUTHENTICATORS)).toEqual(
      'https://www.example.com/docs/setting_up_graylog/user_authentication.htm',
    );
  });

  it('applies overrides passed through branding', async () => {
    asMock(AppConfig.branding).mockReturnValue({
      help_pages: {
        ALERTS: 'http://www.example.com/alerts',
        LICENSE: 'foo',
      },
    });
    const DocsHelper = await loadDocsHelper();

    expect(DocsHelper.toString(DocsHelper.PAGES.ALERTS)).toEqual('http://www.example.com/alerts');
    expect(DocsHelper.toString(DocsHelper.PAGES.LICENSE)).toEqual('https://go2docs.graylog.org/current/foo');
    expect(DocsHelper.toString(DocsHelper.PAGES.AUTHENTICATORS)).toEqual(
      'https://go2docs.graylog.org/current/setting_up_graylog/user_authentication.htm',
    );
  });
});
