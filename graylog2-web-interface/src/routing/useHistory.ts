import { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';

const useHistory = () => {
  const navigate = useNavigate();

  return useMemo(() => ({
    goBack: () => navigate(-1),
    push: (to: string) => navigate(to),
    pushWithState: (to: string, state: any) => navigate(to, { state }),
    replace: (to: string) => navigate(to, { replace: true }),
  }), [navigate]);
};

export type HistoryFunction = ReturnType<typeof useHistory>;

export default useHistory;
