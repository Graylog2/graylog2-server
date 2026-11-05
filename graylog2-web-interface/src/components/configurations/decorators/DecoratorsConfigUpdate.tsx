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
import React, { useCallback, useState, useMemo } from 'react';
import cloneDeep from 'lodash/cloneDeep';
import { useTranslation } from 'react-i18next';

import usePermissions from 'hooks/usePermissions';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import { Modal, Alert } from 'components/bootstrap';
import { IfPermitted, ModalSubmit } from 'components/common';
import type { Stream } from 'logic/streams/types';
import DecoratorList from 'views/components/messagelist/decorators/DecoratorList';
import AddDecoratorButton from 'views/components/messagelist/decorators/AddDecoratorButton';
import type { Decorator } from 'views/components/messagelist/decorators/Types';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import StreamSelect, { DEFAULT_SEARCH_ID, DEFAULT_STREAM_ID } from './StreamSelect';
import formatDecorator from './FormatDecorator';

type Props = {
  streams: Array<Stream>;
  decorators: Array<Decorator>;
  types: { [key: string]: any };

  show?: boolean;
  onCancel: () => void;
  onSave: (newDecorators: Array<Decorator>) => unknown;
};

const _updateOrder = (
  orderedDecorators: Array<{
    id: string;
  }>,
  decorators: Array<Decorator>,
  onChange: (decorators: Array<Decorator>) => void,
) => {
  const newDecorators = cloneDeep(decorators);

  orderedDecorators.forEach((item, idx) => {
    const decorator = newDecorators.find((i) => i.id === item.id);

    if (decorator) {
      decorator.order = idx;
    }
  });

  onChange(newDecorators);
};

const DecoratorsConfigUpdate = ({ streams, decorators, types, show = false, onCancel, onSave }: Props) => {
  const { t } = useTranslation('configurations');
  const [currentStream, setCurrentStream] = useState(DEFAULT_STREAM_ID);
  const [modifiedDecorators, setModifiedDecorators] = useState(decorators);
  const sendTelemetry = useSendTelemetry('decorators');
  const { isPermitted } = usePermissions();

  const canEditStream = useMemo(
    () => isPermitted(`streams:edit:${currentStream}`) || currentStream === DEFAULT_SEARCH_ID,
    [currentStream, isPermitted],
  );

  const onCreate = useCallback(
    ({ stream, ...rest }: Decorator) =>
      setModifiedDecorators([
        ...modifiedDecorators,
        {
          ...rest,
          stream: stream === DEFAULT_SEARCH_ID ? null : stream,
        },
      ]),
    [modifiedDecorators, setModifiedDecorators],
  );
  const onReorder = useCallback(
    (
      orderedDecorators: Array<{
        id: string;
      }>,
    ) => _updateOrder(orderedDecorators, modifiedDecorators, setModifiedDecorators),
    [modifiedDecorators, setModifiedDecorators],
  );
  const onSubmit = useCallback(() => {
    onSave(modifiedDecorators);

    sendTelemetry(TELEMETRY_EVENT_TYPE.CONFIGURATIONS.DECORATORS_UPDATED, {
      app_action_value: 'configuration-save',
    });
  }, [onSave, modifiedDecorators, sendTelemetry]);

  const currentDecorators = modifiedDecorators.filter(
    (decorator) => (decorator.stream || DEFAULT_SEARCH_ID) === currentStream,
  );
  const decoratorItems = currentDecorators
    .sort((d1, d2) => d1.order - d2.order)
    .map((decorator) =>
      formatDecorator(decorator, modifiedDecorators, types, canEditStream ? setModifiedDecorators : undefined),
    );

  const nextOrder = currentDecorators.reduce((currentMax, decorator) => Math.max(currentMax, decorator.order), 0) + 1;

  const _onCancel = useCallback(() => {
    setModifiedDecorators(decorators);
    onCancel();
  }, [decorators, onCancel]);

  return (
    <BootstrapModalWrapper showModal={show} onHide={_onCancel}>
      <Modal.Header>
        <Modal.Title>{t('decorators_update.title')}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <p>{t('decorators_update.select_stream')}</p>

        <StreamSelect onChange={setCurrentStream} value={currentStream} streams={streams} />

        {!canEditStream && currentStream && <Alert bsStyle="warning">{t('decorators_update.read_only')}</Alert>}

        <IfPermitted permissions="decorators:create">
          <p>{t('decorators_update.select_type')}</p>
          <AddDecoratorButton
            stream={currentStream}
            nextOrder={nextOrder}
            decoratorTypes={types}
            onCreate={onCreate}
            showHelp={false}
            disabled={!canEditStream}
          />
        </IfPermitted>

        <p>{t('decorators_update.drag_and_drop')}</p>

        <DecoratorList decorators={decoratorItems} onReorder={onReorder} />
      </Modal.Body>
      <Modal.Footer>
        <ModalSubmit
          onSubmit={onSubmit}
          onCancel={_onCancel}
          submitButtonText={t('decorators_update.submit')}
          disabledSubmit={!canEditStream}
        />
      </Modal.Footer>
    </BootstrapModalWrapper>
  );
};

export default DecoratorsConfigUpdate;
