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
import { Trans, useTranslation } from 'react-i18next';

import { Button } from 'components/bootstrap';
import Popover from 'components/common/Popover';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';

import DecoratorStyles from './decoratorStyles.css';

// `Trans` provides the link text as children.
const DecoratorsDocsLink = ({ children = undefined }: React.PropsWithChildren) => (
  <DocumentationLink page={DocsHelper.PAGES.DECORATORS} text={children} />
);

const PopoverHelp = () => {
  const { t } = useTranslation('widgets');

  return (
    <Popover width={275} position="right" withArrow withinPortal>
      <Popover.Target>
        <Button bsStyle="link" className={DecoratorStyles.helpLink}>
          {t('message_table.decorators.help_title')}
        </Button>
      </Popover.Target>
      <Popover.Dropdown>
        <p className="description">
          <Trans t={t} i18nKey="message_table.decorators.help_description" components={{ strong: <strong /> }} />
        </p>
        <p className="description">{t('message_table.decorators.help_order')}</p>
        <p>
          <Trans t={t} i18nKey="message_table.decorators.help_docs" components={{ docsLink: <DecoratorsDocsLink /> }} />
        </p>
      </Popover.Dropdown>
    </Popover>
  );
};

export default PopoverHelp;
