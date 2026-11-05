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
import React from 'react';
import { Trans, useTranslation } from 'react-i18next';

const COMPONENTS = { bold: <b />, code: <code />, em: <em /> };

const EventKeyHelpPopover = () => {
  const { t } = useTranslation('event_definitions');

  return (
    <>
      <Trans t={t} i18nKey="event_key_help.intro" components={COMPONENTS} />
      <p />
      <Trans t={t} i18nKey="event_key_help.where_it_matters" components={COMPONENTS} />
      <p />
      <Trans t={t} i18nKey="event_key_help.example" components={COMPONENTS} />
      <p />
      <Trans t={t} i18nKey="event_key_help.event_count" components={COMPONENTS} />
    </>
  );
};

export default EventKeyHelpPopover;
