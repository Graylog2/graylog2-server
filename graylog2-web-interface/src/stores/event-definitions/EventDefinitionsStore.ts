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
import Reflux from 'reflux';
import URI from 'urijs';
import cloneDeep from 'lodash/cloneDeep';
import concat from 'lodash/concat';
import defaultTo from 'lodash/defaultTo';
import pick from 'lodash/pick';
import omit from 'lodash/omit';

import * as URLUtils from 'util/URLUtils';
import PaginationURL from 'util/PaginationURL';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import { singletonStore, singletonActions } from 'logic/singleton';

type EventDefinitionsActionsType = {
  listAll: () => Promise<unknown>;
  listPaginated: (params: { query?: string; page?: number; pageSize?: number }) => Promise<unknown>;
  searchPaginated: (newPage: number, newPerPage: number, newQuery: string, additional: unknown) => Promise<unknown>;
  get: (eventDefinitionId: string) => Promise<unknown>;
  create: (newEventDefinition: Record<string, unknown>) => Promise<unknown>;
  copy: (eventDefinition: Record<string, unknown>) => Promise<unknown>;
  update: (eventDefinitionId: string, updatedEventDefinition: Record<string, unknown>) => Promise<unknown>;
  delete: (eventDefinition: Record<string, unknown>) => Promise<unknown>;
  enable: (eventDefinition: Record<string, unknown>) => Promise<unknown>;
  disable: (eventDefinition: Record<string, unknown>) => Promise<unknown>;
  clearNotificationQueue: (eventDefinition: Record<string, unknown>) => Promise<unknown>;
};

export const EventDefinitionsActions = singletonActions('core.EventDefinitions', () =>
  Reflux.createActions<EventDefinitionsActionsType>({
    listAll: { asyncResult: true },
    listPaginated: { asyncResult: true },
    searchPaginated: { asyncResult: true },
    get: { asyncResult: true },
    create: { asyncResult: true },
    copy: { asyncResult: true },
    update: { asyncResult: true },
    delete: { asyncResult: true },
    enable: { asyncResult: true },
    disable: { asyncResult: true },
    clearNotificationQueue: { asyncResult: true },
  }),
);

export const EventDefinitionsStore = singletonStore('core.EventDefinitions', () =>
  Reflux.createStore({
    listenables: [EventDefinitionsActions],
    sourceUrl: '/events/definitions',
    all: undefined,
    eventDefinitions: undefined,
    context: undefined,
    query: undefined,
    pagination: {
      count: undefined,
      page: undefined,
      pageSize: undefined,
      total: undefined,
      grandTotal: undefined,
    },

    getInitialState() {
      return this.getState();
    },

    propagateChanges() {
      this.trigger(this.getState());
    },

    getState() {
      return {
        all: this.all,
        eventDefinitions: this.eventDefinitions,
        context: this.context,
        query: this.query,
        pagination: this.pagination,
      };
    },

    eventDefinitionsUrl({ segments = [], query = {} }: { segments?: Array<string>; query?: Record<string, unknown> }) {
      const uri = new URI(this.sourceUrl);
      const nextSegments = concat(uri.segment(), segments);

      uri.segmentCoded(nextSegments);
      uri.query(query);

      return URLUtils.qualifyUrl(uri.resource());
    },

    refresh() {
      if (this.all) {
        this.listAll();
      }

      if (this.pagination.page) {
        this.listPaginated({
          query: this.query,
          page: this.pagination.page,
          pageSize: this.pagination.pageSize,
        });
      }
    },

    listAll() {
      const promise = fetch('GET', this.eventDefinitionsUrl({ query: { per_page: 0 } }));

      promise.then((response: unknown) => {
        const resp = response as Record<string, unknown>;
        this.all = resp.event_definitions;
        this.context = resp.context;
        this.propagateChanges();

        return response;
      });

      EventDefinitionsActions.listAll.promise(promise);
    },

    listPaginated({ query = '', page = 1, pageSize = 10 }: { query?: string; page?: number; pageSize?: number }) {
      const promise = fetch(
        'GET',
        this.eventDefinitionsUrl({
          query: {
            query: query,
            page: page,
            per_page: pageSize,
          },
        }),
      );

      promise
        .then((response: unknown) => {
          const resp = response as Record<string, unknown>;
          this.eventDefinitions = resp.event_definitions;
          this.context = resp.context;
          this.query = resp.query;

          this.pagination = {
            count: resp.count,
            page: resp.page,
            pageSize: resp.per_page,
            total: resp.total,
            grandTotal: resp.grand_total,
          };

          this.propagateChanges();

          return response;
        })
        .catch((error: unknown) => {
          UserNotification.error(
            `Fetching event definitions failed with status: ${error}`,
            'Could not retrieve event definitions',
          );
        });

      EventDefinitionsActions.listPaginated.promise(promise);
    },

    searchPaginated(newPage: number, newPerPage: number, newQuery: string, additional: unknown) {
      const url = PaginationURL(`${this.sourceUrl}/paginated`, newPage, newPerPage, newQuery, additional);
      const promise = fetch('GET', URLUtils.qualifyUrl(url)).then((response: unknown) => {
        const resp = response as Record<string, unknown>;
        const {
          elements,
          query,
          attributes,
        } = resp;
        const pagination = resp.pagination as Record<string, unknown>;
        const { count, total, page, per_page: perPage } = pagination;

        return {
          elements,
          attributes,
          pagination: {
            count,
            total,
            page,
            perPage,
            query,
          },
        };
      });

      EventDefinitionsActions.searchPaginated.promise(promise);

      return promise;
    },
    get(eventDefinitionId: string) {
      const promise = fetch('GET', this.eventDefinitionsUrl({ segments: [eventDefinitionId, 'with-context'] }));

      promise
        .then((response: unknown) => {
          const resp = response as Record<string, unknown>;

          return {
            eventDefinition: resp.event_definition,
            context: resp.context,
            is_mutable: resp.is_mutable,
          };
        })
        .catch((error: { status?: number; additional?: { body?: { message?: string } } }) => {
          if (error.status === 404) {
            UserNotification.error(
              `Unable to find Event Definition with id <${eventDefinitionId}>, please ensure it wasn't deleted.`,
              'Could not retrieve Event Definition',
            );
          }
        });

      EventDefinitionsActions.get.promise(promise);

      return promise;
    },

    setAlertFlag(eventDefinition: Record<string, unknown>) {
      const notifications = eventDefinition.notifications as Array<unknown>;
      const isAlert = notifications.length > 0;

      return { ...eventDefinition, alert: isAlert };
    },

    extractSchedulerInfo(eventDefinition: Record<string, unknown>) {
      // Removes the internal "_is_scheduled" field from the event definition data. We only use this to pass-through
      // the flag from the form.
      const clonedEventDefinition = cloneDeep(eventDefinition);
      const config = clonedEventDefinition.config as Record<string, unknown>;
      const { _is_scheduled } = pick(config, ['_is_scheduled']);

      clonedEventDefinition.config = omit(config, ['_is_scheduled']);

      return { eventDefinition: clonedEventDefinition, isScheduled: defaultTo(_is_scheduled, true) };
    },

    create(newEventDefinition: Record<string, unknown>) {
      const { eventDefinition, isScheduled } = this.extractSchedulerInfo(newEventDefinition);
      const promise = fetch(
        'POST',
        this.eventDefinitionsUrl({ query: { schedule: isScheduled } }),
        this.setAlertFlag(eventDefinition),
      );

      promise.then(
        (response: unknown) => {
          UserNotification.success(
            'Event Definition created successfully',
            `Event Definition "${eventDefinition.title}" was created successfully.`,
          );

          this.refresh();

          return response;
        },
        (error: { status?: number; additional?: { body?: { failed?: boolean } } }) => {
          if (error.status !== 400 || !error.additional.body || !error.additional.body.failed) {
            UserNotification.error(
              `Creating Event Definition "${eventDefinition.title}" failed with status: ${error}`,
              'Could not save Event Definition',
            );
          }
        },
      );

      EventDefinitionsActions.create.promise(promise);
    },

    copy(eventDefinition: Record<string, unknown>) {
      const promise = fetch('POST', this.eventDefinitionsUrl({ segments: [eventDefinition.id as string, 'duplicate'] }));

      promise.then(
        (response: unknown) => {
          const resp = response as Record<string, unknown>;
          UserNotification.success(
            'Event Definition duplicated successfully',
            `Event Definition "${resp.title}" was created successfully.`,
          );

          this.refresh();

          return response;
        },
        (error: { status?: number; additional?: { body?: { failed?: boolean } } }) => {
          if (error.status !== 400 || !error.additional.body || !error.additional.body.failed) {
            UserNotification.error(
              `Duplicating Event Definition "${eventDefinition.title}" failed with status: ${error}`,
              'Could not duplicate Event Definition',
            );
          }
        },
      );

      EventDefinitionsActions.copy.promise(promise);
    },

    update(eventDefinitionId: string, updatedEventDefinition: Record<string, unknown>) {
      const { eventDefinition, isScheduled } = this.extractSchedulerInfo(updatedEventDefinition);
      const promise = fetch(
        'PUT',
        this.eventDefinitionsUrl({ segments: [eventDefinitionId], query: { schedule: isScheduled } }),
        this.setAlertFlag(eventDefinition),
      );

      promise.then(
        (response: unknown) => {
          UserNotification.success(
            'Event Definition updated successfully',
            `Event Definition "${eventDefinition.title}" was updated successfully.`,
          );

          this.refresh();

          return response;
        },
        (error: { status?: number; additional?: { body?: { failed?: boolean } } }) => {
          if (error.status !== 400 || !error.additional.body || !error.additional.body.failed) {
            UserNotification.error(
              `Updating Event Definition "${eventDefinition.title}" failed with status: ${error}`,
              'Could not update Event Definition',
            );
          }
        },
      );

      EventDefinitionsActions.update.promise(promise);
    },

    delete(eventDefinition: Record<string, unknown>) {
      const promise = fetch('DELETE', this.eventDefinitionsUrl({ segments: [eventDefinition.id as string] }));

      EventDefinitionsActions.delete.promise(promise);
    },

    enable(eventDefinition: Record<string, unknown>) {
      const promise = fetch('PUT', this.eventDefinitionsUrl({ segments: [eventDefinition.id as string, 'schedule'] }));

      promise.then(
        (response: unknown) => {
          UserNotification.success(
            'Event Definition successfully enabled',
            `Event Definition "${eventDefinition.title}" was successfully enabled.`,
          );

          this.refresh();

          return response;
        },
        (error: { status?: number; additional?: { body?: { failed?: boolean } } }) => {
          if (error.status !== 400 || !error.additional.body || !error.additional.body.failed) {
            UserNotification.error(
              `Enabling Event Definition "${eventDefinition.title}" failed with status: ${error}`,
              'Could not enable Event Definition',
            );
          }
        },
      );

      EventDefinitionsActions.enable.promise(promise);
    },

    disable(eventDefinition: Record<string, unknown>) {
      const promise = fetch('PUT', this.eventDefinitionsUrl({ segments: [eventDefinition.id as string, 'unschedule'] }));

      promise.then(
        (response: unknown) => {
          UserNotification.success(
            'Event Definition successfully disabled',
            `Event Definition "${eventDefinition.title}" was successfully disabled.`,
          );

          this.refresh();

          return response;
        },
        (error: { status?: number; additional?: { body?: { failed?: boolean } } }) => {
          if (error.status !== 400 || !error.additional.body || !error.additional.body.failed) {
            UserNotification.error(
              `Disabling Event Definition "${eventDefinition.title}" failed with status: ${error}`,
              'Could not disable Event Definition',
            );
          }
        },
      );

      EventDefinitionsActions.disable.promise(promise);
    },

    clearNotificationQueue(eventDefinition: Record<string, unknown>) {
      const promise = fetch(
        'PUT',
        this.eventDefinitionsUrl({ segments: [eventDefinition.id as string, 'clear-notification-queue'] }),
      );

      promise.then(
        (response: unknown) => {
          UserNotification.success('Queued notifications cleared.', 'Queued notifications were successfully cleared.');

          this.refresh();

          return response;
        },
        (error: { status?: number; additional?: { body?: { failed?: boolean } } }) => {
          if (error.status !== 400 || !error.additional.body || !error.additional.body.failed) {
            UserNotification.error(
              `Clearing queued notifications failed with status: ${error}`,
              'Could not clear queued notifications',
            );
          }
        },
      );

      EventDefinitionsActions.clearNotificationQueue.promise(promise);
    },
  }),
);
