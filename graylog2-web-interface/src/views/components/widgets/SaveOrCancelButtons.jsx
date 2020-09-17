// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import { useFormikContext } from 'formik';

import { Button } from 'components/graylog';

type Props = {
  onCancel: () => void,
  onFinish: () => void,
};

const SaveOrCancelButtons = ({ onFinish, onCancel }: Props) => {
  const { handleSubmit, dirty } = useFormikContext();
  const _onFinish = useCallback((...args) => {
    if (handleSubmit && dirty) {
      handleSubmit();
    }

    return onFinish(...args);
  }, [onFinish, handleSubmit]);

  return (
    <>
      <Button onClick={_onFinish} bsStyle="primary">Save</Button>
      <Button onClick={onCancel}>Cancel</Button>
    </>
  );
};

export default SaveOrCancelButtons;
