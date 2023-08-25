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

import useCarouselAction from 'hooks/useCarouselAction';
import { Icon } from 'components/common';
import { Button } from 'components/bootstrap';

const ContentStreamNewsContentActions = () => {
  const { scrollPrev, scrollNext } = useCarouselAction('.carousel');

  return (
    <>
      <Button onClick={() => scrollPrev()}><Icon name="arrow-left" /></Button>
      <Button onClick={() => scrollNext()}><Icon name="arrow-right" /></Button>
    </>
  );
};

export default ContentStreamNewsContentActions;
