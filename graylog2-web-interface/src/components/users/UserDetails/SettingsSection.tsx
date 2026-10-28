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
import { useState, useEffect } from 'react';
import { Trans, useTranslation } from 'react-i18next';
import upperFirst from 'lodash/upperFirst';
import type { TFunction } from 'i18next';

import Routes from 'routing/Routes';
import { Link, IfPermitted, ReadOnlyFormGroup } from 'components/common';
import type User from 'logic/users/User';
import SectionComponent from 'components/common/Section/SectionComponent';
import useStream from 'components/streams/hooks/useStream';
import useIsGlobalTimeoutEnabled from 'hooks/useIsGlobalTimeoutEnabled';
import { getView } from 'views/api/views';

type Props = {
  user: User;
};

const _sessionTimeout = (
  sessionTimeout: { value: number; unitString: string },
  isGlobalTimeoutEnabled: boolean,
  t: TFunction,
) => {
  if (sessionTimeout) {
    const globalTimeoutLink = (
      <IfPermitted permissions={['clusterconfigentry:read']}>
        <Trans
          t={t}
          i18nKey="user_details.settings.globally_set"
          components={{ configLink: <Link to={Routes.SYSTEM.CONFIGURATIONS} /> }}
        />
      </IfPermitted>
    );

    return (
      <>
        {sessionTimeout.value} {sessionTimeout.unitString} {isGlobalTimeoutEnabled && globalTimeoutLink}
      </>
    );
  }

  return t('user_details.settings.no_timeout');
};

const StartpageValue = ({ type, id }: { type: string | null | undefined; id: string | null | undefined }) => {
  const { t, i18n } = useTranslation('users');
  const [viewTitle, setViewTitle] = useState<string | undefined>();
  const isStream = type === 'stream';
  const { data: stream } = useStream(id ?? '', { enabled: isStream && !!id });

  useEffect(() => {
    if (!type || !id || isStream) {
      return;
    }

    getView(id).then(({ title }) => setViewTitle(title));
  }, [id, type, isStream]);

  if (!type || !id) {
    return <span>{t('user_details.settings.no_startpage')}</span>;
  }

  const title = isStream ? stream?.title : viewTitle;
  const route = isStream ? Routes.stream_search(id) : Routes.dashboard_show(id);

  return (
    <Link to={route}>
      <b>{i18n.exists(`startpage.types.${type}`, { ns: 'users' }) ? t(`startpage.types.${type}`) : upperFirst(type)}</b>
      : {title}
    </Link>
  );
};

const SettingsSection = ({ user: { timezone, serviceAccount, sessionTimeout, startpage } }: Props) => {
  const { t } = useTranslation('users');
  const isGlobalTimeoutEnabled = useIsGlobalTimeoutEnabled();

  return (
    <SectionComponent title={t('user_fields.settings')}>
      <ReadOnlyFormGroup
        label={t('user_fields.sessions_timeout')}
        value={_sessionTimeout(sessionTimeout, isGlobalTimeoutEnabled, t)}
      />
      <ReadOnlyFormGroup label={t('user_form.service_account')} value={serviceAccount} />
      <ReadOnlyFormGroup label={t('user_details.settings.timezone')} value={timezone} />
      <ReadOnlyFormGroup
        label={t('user_details.settings.startpage')}
        value={<StartpageValue type={startpage?.type} id={startpage?.id} />}
      />
    </SectionComponent>
  );
};

export default SettingsSection;
