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
import styled from 'styled-components';

import { OverlayTrigger, Icon } from 'components/common';
import { Table, Button } from 'components/bootstrap';

type CommonFields = 'id' | 'title' | 'name' | 'description' | 'summary';

type Props = {
  commonFields?: Array<CommonFields>;
  fieldMap?: { [field: string]: string };
  example?: React.ReactNode;
  entityName?: string;
};

const QueryHelpButton = styled(Button)`
  padding: 6px 8px;
`;

// Entity names are passed as English words. Known ones are translated, so they can be used in other languages' sentences.
const useTranslatedEntityName = (entityName: string) => {
  const { t, i18n } = useTranslation('common');
  const key = `entity_types.${entityName.toLowerCase().replace(/\s+/g, '_')}`;

  return i18n.exists(key, { ns: 'common' }) ? t(key) : entityName;
};

const row = (field: string, description: string) => (
  <tr key={`row-field-${field}`}>
    <td>{field}</td>
    <td>{description}</td>
  </tr>
);

const DefaultExample = ({ entityName }: { entityName: string }) => {
  const { t } = useTranslation('common');

  return (
    <>
      <p>
        {t('query_helper.example_description', { entityName })}
        <br />
        <code>{t('query_helper.example_query_description')}</code>
        <br />
      </p>
      <p>
        {t('query_helper.example_id', { entityName })}
        <br />
        <code>{t('query_helper.example_query_id')}</code>
        <br />
      </p>
    </>
  );
};

type PopoverProps = Pick<Props, 'commonFields' | 'fieldMap' | 'example' | 'entityName'>;

const QueryHelpPopover = ({ commonFields, fieldMap, example, entityName }: PopoverProps) => {
  const { t } = useTranslation('common');
  const translatedEntityName = useTranslatedEntityName(entityName);

  return (
    <>
      <p>
        <strong>{t('query_helper.available_fields')}</strong>
      </p>
      <Table condensed>
        <thead>
          <tr>
            <th>{t('query_helper.field')}</th>
            <th>{t('query_helper.description')}</th>
          </tr>
        </thead>
        <tbody>
          {commonFields.map((field) =>
            row(field, t(`query_helper.common_fields.${field}`, { entityName: translatedEntityName })),
          )}
          {Object.keys(fieldMap).map((field) => row(field, fieldMap[field]))}
        </tbody>
      </Table>
      <p>
        <strong>{t('query_helper.examples')}</strong>
      </p>
      {example || <DefaultExample entityName={translatedEntityName} />}
    </>
  );
};

const QueryHelper = ({
  commonFields = ['id', 'title', 'description'],
  fieldMap = {},
  example = undefined,
  entityName = 'entity',
}: Props) => {
  const { t } = useTranslation('common');

  return (
    <OverlayTrigger
      trigger="click"
      rootClose
      placement="right"
      overlay={
        <QueryHelpPopover commonFields={commonFields} fieldMap={fieldMap} example={example} entityName={entityName} />
      }
      title={t('query_helper.title')}
      width={500}>
      <QueryHelpButton bsStyle="link" title={t('query_helper.show_help')}>
        <Icon name="help" />
      </QueryHelpButton>
    </OverlayTrigger>
  );
};

export default QueryHelper;
