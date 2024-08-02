import { useState, useEffect, useRef } from 'react';

import EventHandlersThrottler from 'util/EventHandlersThrottler';

const useGraphWidth = (initialWidth: number = 600) => {
  const eventThrottler = useRef(new EventHandlersThrottler());
  const graphContainerRef = useRef(null);
  const [graphWidth, setGraphWidth] = useState(initialWidth);

  useEffect(() => {
    const resizeGraphs = () => {
      const { clientWidth } = graphContainerRef?.current ?? initialWidth;

      setGraphWidth(clientWidth);
    };

    const onResize = () => {
      eventThrottler.current.throttle(() => resizeGraphs());
    };

    resizeGraphs();

    window.addEventListener('resize', onResize);

    return () => {
      window.removeEventListener('resize', onResize);
    };
  }, [initialWidth]);

  return { graphWidth, graphContainerRef };
};

export default useGraphWidth;
