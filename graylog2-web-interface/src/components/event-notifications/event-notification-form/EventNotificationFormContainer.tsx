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
import clone from 'lodash/clone';
import cloneDeep from 'lodash/cloneDeep';
import type { QueryClient } from '@tanstack/react-query';
import { useQueryClient } from '@tanstack/react-query';

import { ConfirmLeaveDialog } from 'components/common';
import Routes from 'routing/Routes';
import {
  createEventNotification,
  updateEventNotification,
  testEventNotification,
  EVENT_NOTIFICATIONS_QUERY_KEY,
} from 'components/event-notifications/hooks/useEventNotifications';
import withHistory from 'routing/withHistory';
import type CancellablePromise from 'logic/rest/CancellablePromise';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';

import EventNotificationForm from './EventNotificationForm';

const initialValidation = {
  errors: {},
};

const initialTestResult: TestResult = {
  isLoading: false,
};

type TestResult = { isLoading: false; error?: boolean; message?: string };

type EventNotificationFormContainerProps = {
  action?: 'create' | 'edit';
  notification?: any;
  /** Controls whether the form should be embedded into another one, and submitted/cancel externally */
  embedded?: boolean;
  /** Controls the ID of the form, so it can be controlled externally */
  formId?: string;
  onSubmit?: (...args: any[]) => void;
  history: any;
  queryClient: QueryClient;
};

class EventNotificationFormContainer extends React.Component<
  EventNotificationFormContainerProps,
  {
    [key: string]: any;
  }
> {
  static scrollToFirstError() {
    if (document.getElementsByClassName('has-error')[0] !== undefined) {
      document.getElementsByClassName('has-error')[0].scrollIntoView(true);
    }
  }

  static defaultProps: Partial<EventNotificationFormContainerProps> = {
    action: 'edit',
    notification: {
      title: '',
      description: '',
      config: {},
    },
    embedded: false,
    formId: undefined,
    onSubmit: () => {},
  };

  constructor(props: EventNotificationFormContainerProps) {
    super(props);

    this.state = {
      notification: props.notification,
      validation: initialValidation,
      testResult: initialTestResult,
      isDirty: false,
    };
  }

  componentWillUnmount() {
    // Test request may take a while to return a result, cancel the promise before leaving the page.
    if (this.testPromise) {
      this.testPromise.cancel();
    }
  }

  private testPromise: CancellablePromise<unknown>;

  handleChange = (key: string, value: unknown) => {
    const { notification } = this.state;
    const nextNotification = cloneDeep(notification);

    nextNotification[key] = value;
    this.setState({ notification: nextNotification, isDirty: true, testResult: initialTestResult });
  };

  handleCancel = () => {
    const { history } = this.props;
    history.push(Routes.ALERTS.NOTIFICATIONS.LIST);
  };

  handleSubmit = () => {
    const { action, embedded, onSubmit, history, queryClient } = this.props;
    const { notification } = this.state;
    const currentUser = CurrentUserStore.getInitialState();

    this.setState({ isDirty: false });

    let promise;

    if (action === 'create') {
      promise = createEventNotification(notification);

      promise
        .then((response) => {
          queryClient.invalidateQueries({ queryKey: EVENT_NOTIFICATIONS_QUERY_KEY });
          CurrentUserStore.update(currentUser.currentUser.username);

          return response;
        })
        .then(
          () => {
            if (!embedded) {
              history.push(Routes.ALERTS.NOTIFICATIONS.LIST);
            }
          },
          (errorResponse) => {
            const { body } = errorResponse.additional;

            if (errorResponse.status === 400 && body && body.failed) {
              this.setState({ validation: body });
              EventNotificationFormContainer.scrollToFirstError();
            }
          },
        );
    } else {
      promise = updateEventNotification(notification.id, notification);

      promise.then(
        () => {
          queryClient.invalidateQueries({ queryKey: EVENT_NOTIFICATIONS_QUERY_KEY });

          if (!embedded) {
            history.push(Routes.ALERTS.NOTIFICATIONS.LIST);
          }
        },
        (errorResponse) => {
          const { body } = errorResponse.additional;

          if (errorResponse.status === 400 && body && body.failed) {
            this.setState({ validation: body });
            EventNotificationFormContainer.scrollToFirstError();
          }
        },
      );
    }

    onSubmit(promise);
  };

  handleTest = () => {
    const { notification } = this.state;

    this.setState({ testResult: { isLoading: true }, validation: initialValidation });
    const testResult: TestResult = clone(initialTestResult);

    this.testPromise = testEventNotification(notification);

    this.testPromise
      .then(
        (response) => {
          testResult.error = false;
          testResult.message = 'Notification was executed successfully.';

          return response;
        },
        (errorResponse) => {
          testResult.error = true;
          const { body } = errorResponse.additional;

          if (errorResponse.status === 400 && body && body.failed) {
            testResult.message = 'Validation failed, please correct any errors in the form before continuing.';
            this.setState({ validation: body });
          } else {
            testResult.message = errorResponse.responseMessage || 'Unknown error, please check your server logs.';
          }
        },
      )
      .finally(() => {
        this.setState({ testResult: testResult });
        this.testPromise = undefined;
      });
  };

  render() {
    const { action, embedded, formId } = this.props;
    const { notification, validation, testResult, isDirty } = this.state;

    return (
      <>
        {!embedded && isDirty && (
          <ConfirmLeaveDialog question="Do you really want to abandon this page and lose your changes? This action cannot be undone." />
        )}
        <EventNotificationForm
          action={action}
          notification={notification}
          validation={validation}
          testResult={testResult}
          formId={formId}
          embedded={embedded}
          onChange={this.handleChange}
          onCancel={this.handleCancel}
          onSubmit={this.handleSubmit}
          onTest={this.handleTest}
        />
      </>
    );
  }
}

const EventNotificationFormContainerWithHistory = withHistory(EventNotificationFormContainer);

const EventNotificationFormContainerWithQueryClient = (
  props: Omit<EventNotificationFormContainerProps, 'history' | 'queryClient'>,
) => {
  const queryClient = useQueryClient();

  return <EventNotificationFormContainerWithHistory {...props} queryClient={queryClient} />;
};

export default EventNotificationFormContainerWithQueryClient;
