// @flow strict
import React, { useCallback, useState } from 'react';
import PropTypes from 'prop-types';
import { cloneDeep } from 'lodash';

import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import { Button, Modal } from 'components/graylog';
import { IfPermitted } from 'components/common';
import type { Stream } from 'stores/streams/StreamsStore';
import DecoratorList from 'views/components/messagelist/decorators/DecoratorList';
import AddDecoratorButton from 'views/components/messagelist/decorators/AddDecoratorButton';
import type { Decorator } from 'views/components/messagelist/decorators/Types';
import StreamSelect, { DEFAULT_SEARCH_ID, DEFAULT_STREAM_ID } from './StreamSelect';
import formatDecorator from './FormatDecorator';

type Props = {
  streams: Array<Stream>,
  decorators: Array<Decorator>,
  types: { [string]: any },
  show?: boolean,
  onCancel: () => void,
  onSave: (Array<Decorator>) => mixed,
};

const _updateOrder = (orderedDecorators, decorators, onChange) => {
  const newDecorators = cloneDeep(decorators);
  orderedDecorators.forEach((item, idx) => {
    const decorator = newDecorators.find(i => i.id === item.id);
    if (decorator) {
      decorator.order = idx;
    }
  });

  onChange(newDecorators);
};


const DecoratorsConfigUpdate = ({ streams, decorators, types, show = false, onCancel, onSave }: Props, modalRef) => {
  const [currentStream, setCurrentStream] = useState(DEFAULT_STREAM_ID);
  const [modifiedDecorators, setModifiedDecorators] = useState(decorators);
  const onCreate = useCallback(
    ({ stream, ...rest }) => setModifiedDecorators([...modifiedDecorators, { ...rest, stream: stream === DEFAULT_SEARCH_ID ? null : stream }]),
    [modifiedDecorators, setModifiedDecorators],
  );
  const onReorder = useCallback(
    orderedDecorators => _updateOrder(orderedDecorators, modifiedDecorators, setModifiedDecorators),
    [modifiedDecorators, setModifiedDecorators],
  );
  const onSubmit = useCallback(() => onSave(modifiedDecorators), [onSave, modifiedDecorators]);

  const currentDecorators = modifiedDecorators.filter(decorator => (decorator.stream || DEFAULT_SEARCH_ID) === currentStream);
  const decoratorItems = currentDecorators
    .sort((d1, d2) => d1.order - d2.order)
    .map(decorator => formatDecorator(decorator, modifiedDecorators, types, setModifiedDecorators));

  const nextOrder = currentDecorators.reduce((currentMax, decorator) => Math.max(currentMax, decorator.order), 0) + 1;

  return (
    <BootstrapModalWrapper ref={modalRef}
                           showModal={show}
                           onHide={onCancel}>
      <Modal.Header closeButton>
        <Modal.Title>Update Default Decorators Configuration</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <p>Select the stream for which you want to change the set of default decorators.</p>
        <StreamSelect onChange={setCurrentStream} value={currentStream} streams={streams} />

        <IfPermitted permissions="decorators:create">
          <p>Select the type to create a new decorator for this stream:</p>
          <AddDecoratorButton stream={currentStream} nextOrder={nextOrder} decoratorTypes={types} onCreate={onCreate} showHelp={false} />
        </IfPermitted>

        <p>Use drag and drop to change the execution order of the decorators.</p>

        <DecoratorList decorators={decoratorItems} onReorder={onReorder} />
      </Modal.Body>
      <Modal.Footer>
        <Button type="button" onClick={onCancel}>Cancel</Button>
        <Button bsStyle="primary" onClick={onSubmit}>Save</Button>
      </Modal.Footer>
    </BootstrapModalWrapper>
  );
};

DecoratorsConfigUpdate.propTypes = {
  streams: PropTypes.array.isRequired,
  decorators: PropTypes.array.isRequired,
  types: PropTypes.object.isRequired,
  show: PropTypes.bool,
  onCancel: PropTypes.func.isRequired,
  onSave: PropTypes.func.isRequired,
};

DecoratorsConfigUpdate.defaultProps = {
  show: false,
};

export default React.forwardRef<Props, BootstrapModalWrapper>(DecoratorsConfigUpdate);
