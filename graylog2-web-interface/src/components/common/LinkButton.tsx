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
import * as React from 'react';
import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';

import { Button } from 'components/bootstrap';

type ButtonProps = React.ComponentProps<typeof Button>;

type Props = Omit<ButtonProps, 'target'> & {
  to: string,
  target?: '_blank' | '_self' | '_parent' | '_top' | 'framename',
}

const LinkButton = ({ to, target, onClick, ...restButtonProps }: Props) => {
  const navigate = useNavigate();
  const handleOnClick = useCallback((e) => {
    if (target === '_self') {
      navigate(to);
    } else {
      window.open(to, target);
    }

    if (onClick) onClick(e);
  }, [target, onClick, navigate, to]);

  return <Button onClick={handleOnClick} {...restButtonProps} />;
};

LinkButton.defaultProps = {
  target: '_self',
};

export default LinkButton;
