import asMock from 'helpers/mocking/AsMock';
import AppConfig from 'util/AppConfig';

jest.mock('util/AppConfig');

describe('DocsHelper', () => {
  it('prefixes page URLs with default base URL', () => {
    jest.isolateModules(() => {
      // eslint-disable-next-line global-require
      const DocsHelper = require('./DocsHelper').default;

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
  });

  it('applies custom url prefix', () => {
    jest.isolateModules(() => {
      asMock(AppConfig.branding).mockReturnValue({
        help_url: 'https://www.example.com/docs',
      });
      // eslint-disable-next-line global-require
      const DocsHelper = require('./DocsHelper').default;

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
  });

  it('applies overrides passed through branding', () => {
    jest.isolateModules(() => {
      asMock(AppConfig.branding).mockReturnValue({
        help_pages: {
          ALERTS: 'http://www.example.com/alerts',
          LICENSE: 'foo',
        },
      });

      // eslint-disable-next-line global-require
      const DocsHelper = require('./DocsHelper').default;

      expect(DocsHelper.toString(DocsHelper.PAGES.ALERTS)).toEqual('http://www.example.com/alerts');
      expect(DocsHelper.toString(DocsHelper.PAGES.LICENSE)).toEqual('https://go2docs.graylog.org/current/foo');
      expect(DocsHelper.toString(DocsHelper.PAGES.AUTHENTICATORS)).toEqual(
        'https://go2docs.graylog.org/current/setting_up_graylog/user_authentication.htm',
      );
    });
  });
});
