import EmblaCarousel from 'embla-carousel';
import { useCallback, useMemo, useEffect, useState } from 'react';

const useCarouselAction = (carouselElementClass: string) => {
  const [carousel, setCarousel] = useState<HTMLElement>(undefined);

  useEffect(() => {
    setCarousel(document.querySelector<HTMLElement>(carouselElementClass));
  }, [carouselElementClass]);

  const emblaApi = useMemo(() => carousel && EmblaCarousel(carousel, { containScroll: 'trimSnaps' }), [carousel]);

  const scrollPrev = useCallback(() => {
    if (emblaApi) emblaApi.scrollPrev();
  }, [emblaApi]);
  const scrollNext = useCallback(() => {
    if (emblaApi) emblaApi.scrollNext();
  }, [emblaApi]);

  return {
    scrollNext,
    scrollPrev,
  };
};

export default useCarouselAction;
