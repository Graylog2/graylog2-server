package org.graylog2.decorators;

import com.google.inject.ImplementedBy;

import java.util.List;
import java.util.Map;

@ImplementedBy(DecoratorServiceImpl.class)
public interface DecoratorService {
    List<Decorator> findForStream(String streamId);
    List<Decorator> findForGlobal();
    List<Decorator> findAll();
    Decorator create(String type, Map<String, Object> config, String stream);
    Decorator create(String type, Map<String, Object> config);
    Decorator save(Decorator decorator);
    int delete(String decoratorId);
}
