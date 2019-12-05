// @flow strict
import { PluginStore } from 'graylog-web-plugin/plugin';
import asMock from 'helpers/mocking/AsMock';

import { widgetDefinition } from './Widgets';

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: {
    exports: jest.fn(),
  },
}));

describe('Widgets', () => {
  describe('widgetDefinition', () => {
    it('returns widget definition if present', () => {
      asMock(PluginStore.exports).mockReturnValue([{
        type: 'some-other-type',
        value: 23,
      }, {
        type: 'some-type',
        value: 42,
      }]);

      expect(widgetDefinition('some-type')).toEqual({ type: 'some-type', value: 42 });
    });
    it('returns default definition if widget type is not present', () => {
      asMock(PluginStore.exports).mockReturnValue([{
        type: 'some-other-type',
        value: 23,
      }, {
        type: 'default',
        value: 42,
      }]);

      expect(widgetDefinition('some-type')).toEqual({ type: 'default', value: 42 });
    });
    it('throws error if widget type and default type are not present', () => {
      asMock(PluginStore.exports).mockReturnValue([{
        type: 'some-other-type',
        value: 23,
      }]);

      expect(() => widgetDefinition('some-type'))
        .toThrowError('Neither a widget of type "some-type" nor a default widget are registered!');
    });
  });
});
