// @flow strict
import { useMemo } from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

const usePluginEntities = <T>(entityKey: string): Array<T> => useMemo(() => PluginStore.exports(entityKey), [entityKey]);

export default usePluginEntities;
