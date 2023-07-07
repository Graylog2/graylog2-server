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
import styled from 'styled-components';
import useEmblaCarousel from 'embla-carousel-react';

import CarouselSlide from 'components/common/carousel/CarouselSlide';

type Props = {
  children: React.ReactNode,
};

const StyledDiv = styled.div`
  &.carousel {
    overflow: hidden;

    .carousel-container {
      backface-visibility: hidden;
      display: flex;
      flex-direction: row;
      height: auto;
    }
  }
`;

const Carousel = ({ children }: Props) => {
  const [emblaRef] = useEmblaCarousel({ containScroll: 'trimSnaps' });

  return (
    <StyledDiv className="carousel" ref={emblaRef}>
      <div className="carousel-container">
        {children}
      </div>
    </StyledDiv>
  );
};

Carousel.Slide = CarouselSlide;
export default Carousel;
