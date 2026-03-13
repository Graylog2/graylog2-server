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
import styled, { css } from 'styled-components';

import { Alert, Row, Col } from 'components/bootstrap';
import Icon from 'components/common/Icon';

type NotificationSeverity = 'danger' | 'warning';

type NotificationItem = {
  id?: string;
  severity: NotificationSeverity;
  message: React.ReactNode;
};

type Props = {
  title: string;
  items: Array<NotificationItem>;
};

const StyledAlert = styled(Alert)(
  ({ theme }) => css`
    margin-top: ${theme.spacings.sm};
  `,
);

const NotificationList = styled.ul(
  ({ theme }) => css`
    list-style: none;
    margin: 0;
    padding: 0;

    > li {
      display: flex;
      align-items: center;
      margin-bottom: ${theme.spacings.xs};
    }

    > li:last-child {
      margin-bottom: 0;
    }
  `,
);

const DangerIcon = styled(Icon)(
  ({ theme }) => css`
    color: ${theme.colors.variant.danger};
    margin-right: ${theme.spacings.xs};
  `,
);

const WarningIcon = styled(Icon)(
  ({ theme }) => css`
    color: ${theme.colors.variant.warning};
    margin-right: ${theme.spacings.xs};
  `,
);

const ICON_BY_SEVERITY: Record<NotificationSeverity, typeof DangerIcon | typeof WarningIcon> = {
  danger: DangerIcon,
  warning: WarningIcon,
};

const NotificationBanner = ({ title, items }: Props) => {
  if (items.length === 0) {
    return null;
  }

  return (
    <Row className="content">
      <Col md={12}>
        <StyledAlert bsStyle="info" noIcon title={title}>
          <NotificationList>
            {items.map((item, index) => {
              const SeverityIcon = ICON_BY_SEVERITY[item.severity];
              const key = item.id ?? (typeof item.message === 'string' ? item.message : `${item.severity}-${index}`);

              return (
                <li key={key}>
                  <SeverityIcon name="error" />
                  <span>{item.message}</span>
                </li>
              );
            })}
          </NotificationList>
        </StyledAlert>
      </Col>
    </Row>
  );
};

export type { NotificationItem, NotificationSeverity };
export default NotificationBanner;
