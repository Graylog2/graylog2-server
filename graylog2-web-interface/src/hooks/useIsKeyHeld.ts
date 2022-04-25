import { useState, useEffect } from 'react';

const useIsKeyHeld = (buttonKey: string) => {
  const [shiftHeld, setShiftHeld] = useState(false);

  useEffect(() => {
    function downHandler({ key }) {
      if (key === buttonKey) {
        setShiftHeld(true);
      }
    }

    function upHandler({ key }) {
      if (key === buttonKey) {
        setShiftHeld(false);
      }
    }

    window.addEventListener('keydown', downHandler);
    window.addEventListener('keyup', upHandler);

    return () => {
      window.removeEventListener('keydown', downHandler);
      window.removeEventListener('keyup', upHandler);
    };
  }, [setShiftHeld, buttonKey]);

  return shiftHeld;
};

export default useIsKeyHeld;
