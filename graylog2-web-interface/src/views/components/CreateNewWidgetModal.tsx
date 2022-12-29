import * as React from 'react';
import { useMemo } from 'react';

import { Modal, Button } from 'components/bootstrap';
import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import usePluginEntities from 'hooks/usePluginEntities';
import { WidgetActions } from 'views/stores/WidgetStore';
import generateId from 'logic/generateId';
import { CurrentViewStateActions } from 'views/stores/CurrentViewStateStore';
import { useStore } from 'stores/connect';
import { ViewStore } from 'views/stores/ViewStore';

const modalTitle = 'Create new widget';

type Props = {
  onCancel: () => void,
  position: WidgetPosition,
}

const CreateNewWidgetModal = ({ onCancel, position }: Props) => {
  const creators = usePluginEntities('widgetCreators');
  const view = useStore(ViewStore, (store) => store?.view);

  const widgetButtons = useMemo(() => creators.map(({ title, func }) => {
    const onClick = async () => {
      const newId = generateId();
      await CurrentViewStateActions.updateWidgetPosition(newId, position);
      const newWidget = func({ view }).toBuilder().id(newId).build();

      return WidgetActions.create(newWidget);
    };

    return (
      <Button type="button" onClick={onClick}>{title}</Button>
    );
  }), [creators, position, view]);

  return (
    <Modal title={modalTitle}
           onHide={onCancel}
           show>
      <Modal.Header closeButton>
        <Modal.Title>{modalTitle}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {widgetButtons}
      </Modal.Body>
      <Modal.Footer>
        <Button type="button" onClick={onCancel}>
          Cancel
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default CreateNewWidgetModal;
