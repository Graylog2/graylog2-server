package org.graylog2.jersey;

import com.google.inject.Injector;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InjectionManagerFactory;

public class GuiceInjectionManagerFactory implements InjectionManagerFactory {

  @Override
  public InjectionManager create(Object parent) {
    Injector parentInjector = null;
    if (parent != null) {
      if (parent instanceof Injector) {
        parentInjector = (Injector) parent;
      } else if (parent instanceof GuiceInjectionManager) {
        parentInjector = ((GuiceInjectionManager) parent).injector();
      }
    }
    return new GuiceInjectionManager(parentInjector);
  }

}
