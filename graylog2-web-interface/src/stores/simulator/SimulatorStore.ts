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

import * as URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import MessageFormatter from 'logic/message/MessageFormatter';
import ObjectUtils from 'util/ObjectUtils';
import { singletonStore, singletonActions } from 'logic/singleton';

type SimulatorActionsType = {
  simulate: (stream: { id: string }, messageFields: unknown, inputId: string) => Promise<unknown>;
};

export const SimulatorActions = singletonActions('core.Simulator', () =>
  Reflux.createActions<SimulatorActionsType>({
    simulate: { asyncResult: true },
  }),
);

export const SimulatorStore = singletonStore('core.Simulator', () =>
  Reflux.createStore({
    listenables: [SimulatorActions],

    simulate(stream: { id: string }, messageFields: unknown, inputId: string) {
      const url = URLUtils.qualifyUrl(ApiRoutes.SimulatorController.simulate().url);
      const simulation = {
        stream_id: stream.id,
        message: messageFields,
        input_id: inputId,
      };

      type SimulateResponse = { messages: Array<unknown>; [key: string]: unknown };
      const promise = fetch<SimulateResponse>('POST', url, simulation).then((response) => {
        const formattedResponse = ObjectUtils.clone(response);

        formattedResponse.messages = response.messages.map((msg) => MessageFormatter.formatMessageSummary(msg));

        return formattedResponse;
      });

      SimulatorActions.simulate.promise(promise);
    },
  }),
);
