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

import { Icon } from 'components/common';
import { Button } from 'components/bootstrap';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { CAROUSEL_ID } from 'components/content-stream/ContentStreamNews';
import { useCarouselActions } from 'components/common/Carousel';

const ContentStreamNewsContentActions = () => {
  const { scrollPrev, scrollNext, nextBtnDisabled, prevBtnDisabled } = useCarouselActions(CAROUSEL_ID);
  const sendTelemetry = useSendTelemetry();

  const handlePrev = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.CONTENTSTREAM.PREV_ARROW_CLICKED, {
      app_pathname: 'welcome',
      app_section: 'content-stream',
    });

    scrollPrev();
  };

  const handleNext = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.CONTENTSTREAM.NEXT_ARROW_CLICKED, {
      app_pathname: 'welcome',
      app_section: 'content-stream',
    });

    scrollNext();
  };

  return (
    <>
      <Button onClick={() => handlePrev()} disabled={prevBtnDisabled}>
        <Icon name="chevron_left" />
      </Button>
      <Button onClick={() => handleNext()} disabled={nextBtnDisabled}>
        <Icon name="chevron_right" />
      </Button>
    </>
  );
};

export default ContentStreamNewsContentActions;
