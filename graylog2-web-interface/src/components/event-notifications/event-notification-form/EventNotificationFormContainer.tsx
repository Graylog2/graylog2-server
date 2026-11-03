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

import i18n from 'i18n';
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
import CurrentUser from 'logic/users/CurrentUser';

import EventNotificationForm from './EventNotificationForm';
import { translateValidationResult } from 'util/translateApiError';

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

    this.setState({ isDirty: false });

    let promise;

    if (action === 'create') {
      promise = createEventNotification(notification);

      promise
        .then((response) => {
          queryClient.invalidateQueries({ queryKey: EVENT_NOTIFICATIONS_QUERY_KEY });
          CurrentUser.reload();

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
              this.setState({ validation: translateValidationResult(body) });
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
            this.setState({ validation: translateValidationResult(body) });
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
          testResult.message = i18n.t('test.success', { ns: 'event_notifications' });

          return response;
        },
        (errorResponse) => {
          testResult.error = true;
          const { body } = errorResponse.additional;

          if (errorResponse.status === 400 && body && body.failed) {
            testResult.message = i18n.t('test.validation_failed', { ns: 'event_notifications' });
            this.setState({ validation: translateValidationResult(body) });
          } else {
            testResult.message =
              errorResponse.responseMessage || i18n.t('test.unknown_error', { ns: 'event_notifications' });
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
          <ConfirmLeaveDialog question={i18n.t('form.confirm_leave', { ns: 'event_notifications' })} />
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
