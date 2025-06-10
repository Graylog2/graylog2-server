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

import { Table, Button } from 'components/bootstrap';
import { Icon } from 'components/common';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';

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
  const [displayDetails, setDisplayDetails] = React.useState(false);

  const toggleDisplayDetails = () => {
    setDisplayDetails((prevDisplayDetails) => !prevDisplayDetails);
  };

  return (
    <>
      <h4>
        <Link target="_blank" to={Routes.ALERTS.NOTIFICATIONS.show(definitionNotification.notification_id)}>
          {notification.title || definitionNotification.notification_id}
        </Link>
      </h4>
      <dl>
        <dd>{type}</dd>
        <dd>
          <Button bsStyle="link" className="btn-text" bsSize="xsmall" onClick={toggleDisplayDetails}>
            <Icon name={`arrow_${displayDetails ? 'drop_down' : 'right'}`} />
            &nbsp;
            {displayDetails ? 'Less details' : 'More details'}
          </Button>
          {displayDetails && (
            <Table condensed hover className={styles.fixedTable}>
              <tbody>
                <tr>
                  <td>Description</td>
                  <td>{notification.description || 'No description given'}</td>
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
