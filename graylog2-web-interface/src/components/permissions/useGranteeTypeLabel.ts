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
import { useTranslation } from 'react-i18next';
import capitalize from 'lodash/capitalize';

/** Returns a function which translates the type of a grantee (user, team, ...). Unknown types are capitalized. */
const useGranteeTypeLabel = () => {
  const { t, i18n } = useTranslation('users');

  return (type: string) =>
    i18n.exists(`permissions.grantee_types.${type}`, { ns: 'users' })
      ? t(`permissions.grantee_types.${type}`)
      : capitalize(type);
};

export default useGranteeTypeLabel;
