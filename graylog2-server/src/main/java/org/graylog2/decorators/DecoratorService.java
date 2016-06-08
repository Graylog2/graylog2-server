package org.graylog2.decorators;

import com.google.inject.ImplementedBy;

import java.util.List;

@ImplementedBy(DecoratorServiceImpl.class)
public interface DecoratorService {
    List<Decorator> findForStream(String streamId);
    List<Decorator> findForGlobal();
    List<Decorator> findAll();
    Decorator create(String type, String field, String stream);
    Decorator create(String type, String field);
    Decorator save(Decorator decorator);
}
