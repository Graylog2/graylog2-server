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

import { Button } from 'components/bootstrap';

type Props = {
  children: React.ReactElement;
  disabled?: boolean;
  onCancel: () => void;
  onSubmitForm: (arg: any) => void;
};

const InlineForm = (submitTitle: string | undefined = undefined): React.ComponentType<Props> => {
  const InlineFormInner = (
    { children, disabled = false, onCancel, onSubmitForm }: Props,
    ref: React.ForwardedRef<HTMLFormElement>,
  ) => {
    const { t } = useTranslation('widgets');
    const onSubmit = (event) => {
      event.stopPropagation();
      const { target: formDOMNode } = event;

      if (typeof formDOMNode.checkValidity === 'function' && !formDOMNode.checkValidity()) {
        event.preventDefault();

        return;
      }

      // If function is not given, let the browser continue propagating the submit event
      if (typeof onSubmitForm === 'function') {
        event.preventDefault();
        onSubmitForm(event);
      }
    };

    return (
      <form onSubmit={onSubmit} ref={ref}>
        {children}
        <Button type="submit" bsStyle="primary" disabled={disabled}>
          {submitTitle ?? t('message_table.decorators.create')}
        </Button>{' '}
        <Button type="button" disabled={disabled} onClick={onCancel}>
          {t('actions.cancel', { ns: 'common' })}
        </Button>
      </form>
    );
  };

  return React.forwardRef(InlineFormInner);
};

export default InlineForm;
