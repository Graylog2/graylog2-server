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
import { useTranslation } from 'react-i18next';

import { Input } from 'components/bootstrap';
import useProductName from 'brand-customization/useProductName';

type Props = {
  id: string;
  defaultChecked: boolean;
  onChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
};

const ThrottlingCheckbox = ({ id, defaultChecked, onChange }: Props) => {
  const { t } = useTranslation('integrations');
  const productName = useProductName();

  return (
    <Input
      id={id}
      type="checkbox"
      value="enable-throttling"
      defaultChecked={defaultChecked}
      onChange={onChange}
      label={t('throttling_checkbox.label')}
      help={t('throttling_checkbox.help', { productName })}
    />
  );
};

export default ThrottlingCheckbox;
