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
import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Table, Button } from 'components/bootstrap';
import { Icon } from 'components/common';
import EventNotificationLink from 'components/event-notifications/event-notifications/EventNotificationLink';

import styles from './CommonNotificationSummary.css';

type Props = React.PropsWithChildren<{
  type: string;
  notification: any;
  definitionNotification?: any;
}>;

const CommonNotificationSummary = ({
  definitionNotification = undefined,
  type,
  notification,
  children = undefined,
}: Props) => {
  const { t } = useTranslation('event_notifications');
  const [displayDetails, setDisplayDetails] = useState(false);

  const toggleDisplayDetails = () => {
    setDisplayDetails((prevDisplayDetails) => !prevDisplayDetails);
  };

  return (
    <>
      <h4>
        <EventNotificationLink title={notification.title} id={definitionNotification.notification_id} />
      </h4>
      <dl>
        <dd>{type}</dd>
        <dd>
          <Button bsStyle="link" className="btn-text" bsSize="xsmall" onClick={toggleDisplayDetails}>
            <Icon name={`arrow_${displayDetails ? 'drop_down' : 'right'}`} />
            &nbsp;
            {displayDetails ? t('summary.less_details') : t('summary.more_details')}
          </Button>
          {displayDetails && (
            <Table condensed className={styles.fixedTable}>
              <tbody>
                <tr>
                  <td>{t('form.description')}</td>
                  <td>{notification.description || t('summary.no_description')}</td>
                </tr>
                {children}
              </tbody>
            </Table>
          )}
        </dd>
      </dl>
    </>
  );
};

export default CommonNotificationSummary;
