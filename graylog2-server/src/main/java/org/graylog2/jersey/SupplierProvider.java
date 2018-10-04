package org.graylog2.jersey;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.function.Supplier;

/**
 * Given a supplier for a certain type, makes the value of that supplier available for injection.
 *
 * @param <S> the supplier's type, a subclass of Supplier
 * @param <T>  the type of results supplied by the supplier
 */
public class SupplierProvider<S extends Supplier<T>, T> implements Provider<T>{

  @Inject
  S supplier;

  @Override
  public T get() {
    return supplier.get();
  }
}
