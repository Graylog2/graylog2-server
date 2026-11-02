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

import { useQuery } from '@tanstack/react-query';

import { SystemCodecsTypes } from '@graylog/server-api';

import i18n from 'i18n';
import { defaultOnError } from 'util/conditional/onError';

import type { CodecTypes } from './Types';

const useCodecTypes = () => {
  const { data, isInitialLoading } = useQuery<CodecTypes>({
    queryKey: ['system', 'codecs', 'types'],
    queryFn: () =>
      defaultOnError(
        SystemCodecsTypes.getAll(),
        i18n.t('notifications.codec_types_failed', { ns: 'messageloaders' }),
        i18n.t('notifications.codec_types_failed_title', { ns: 'messageloaders' }),
      ),
  });

  return { data, isInitialLoading };
};

export default useCodecTypes;
