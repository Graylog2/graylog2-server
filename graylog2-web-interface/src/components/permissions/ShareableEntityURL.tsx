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
import styled, { css } from 'styled-components';

import { ClipboardButton, Icon } from 'components/common';
import { FormGroup, InputGroup, FormControl } from 'components/bootstrap';
import useShowRouteFromGRN from 'routing/hooks/useShowRouteFromGRN';

const StyledFormControl = styled(FormControl)(
  ({ theme }) => css`
    &[readonly] {
      background-color: ${theme.colors.input.background};
    }
  `,
);

const InputGroupAddon = styled(InputGroup.Addon)`
  padding: 0;
`;

const StyledClipboardButton = styled(ClipboardButton)`
  border-radius: 0;
  border: 0;
`;

type Props = {
  entityGRN: string;
};

const ShareableEntityURL = ({ entityGRN }: Props) => {
  const { t } = useTranslation('users');
  const entityRoute = useShowRouteFromGRN(entityGRN);
  const entityUrl = `${window.location.origin.toString()}${entityRoute}`;

  return (
    <div>
      <FormGroup>
        <InputGroup>
          <StyledFormControl type="text" value={entityUrl} readOnly />
          <InputGroupAddon>
            <StyledClipboardButton
              text={entityUrl}
              buttonTitle={t('permissions.shareable_url.copy_title')}
              title={<Icon name="content_copy" />}
            />
          </InputGroupAddon>
        </InputGroup>
      </FormGroup>
      <div>{t('permissions.shareable_url.hint')}</div>
    </div>
  );
};

export default ShareableEntityURL;
