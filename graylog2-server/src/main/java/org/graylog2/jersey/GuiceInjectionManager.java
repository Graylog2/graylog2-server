package org.graylog2.jersey;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.glassfish.jersey.internal.inject.Binder;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.ClassBinding;
import org.glassfish.jersey.internal.inject.ForeignDescriptor;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InjectionResolverBinding;
import org.glassfish.jersey.internal.inject.InstanceBinding;
import org.glassfish.jersey.internal.inject.ServiceHolder;
import org.glassfish.jersey.internal.inject.ServiceHolderImpl;
import org.glassfish.jersey.internal.inject.SupplierClassBinding;
import org.glassfish.jersey.internal.inject.SupplierInstanceBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuiceInjectionManager implements InjectionManager {
  private static final Logger LOG = LoggerFactory.getLogger(GuiceInjectionManager.class);
  private static final Injector INJECTOR = Guice.createInjector();

  private final Injector parent;
  private final ImmutableSet.Builder<Binding> bindings;
  private final ImmutableSet.Builder<Module> modulesBuilder;

  private Injector injector;

  public GuiceInjectionManager(Injector parent) {
    this.parent = parent;
    bindings = ImmutableSet.builder();
    modulesBuilder = ImmutableSet.builder();
  }

  @Override
  public void completeRegistration() {
    // collect all registered bindings into a module and combine it with the modules we've been given
    final BindingsModule bindingsModule = new BindingsModule(bindings.build());
    final Set<Module> allModules = modulesBuilder
        .add(bindingsModule)
        .add(binder -> binder.bind(InjectionManager.class).toInstance(this))
        .build();
    if (parent == null) {
      injector = Guice.createInjector(allModules);
    } else {
      injector = parent.createChildInjector(allModules);
    }
  }

  @Override
  public void shutdown() {}

  @Override
  public void register(Binding binding) {
    logBinding(binding);
    bindings.add(binding);
  }

  private void logBinding(Binding binding) {
    final Set contracts = binding.getContracts();
    if (binding instanceof InstanceBinding) {
      final InstanceBinding ib = (InstanceBinding) binding;
      LOG.trace("[Instance]: contracts {} -> {}",
          contracts, ib.getService().getClass().getCanonicalName());
    } else if (binding instanceof ClassBinding) {
      final ClassBinding cb = (ClassBinding) binding;
      LOG.trace("[Class]: contracts {} -> {}",
          contracts, cb.getService().getCanonicalName());
    } else if (binding instanceof InjectionResolverBinding) {
      final InjectionResolverBinding irb = (InjectionResolverBinding) binding;
      LOG.trace("[InjectionResolver]: contracts {} -> {}",
          contracts, irb.getResolver().getClass().getCanonicalName());
    } else if (binding instanceof SupplierClassBinding) {
      final SupplierClassBinding scb = (SupplierClassBinding) binding;
      LOG.trace("[SupplierClass]: contracts {} -> {}",
          contracts, scb.getSupplierClass().getCanonicalName());
    } else if (binding instanceof SupplierInstanceBinding) {
      final SupplierInstanceBinding sib = (SupplierInstanceBinding) binding;
      LOG.trace("[Supplier]: contracts {} -> {}",
          contracts, sib.getSupplier().getClass().getCanonicalName());
    }
  }

  @Override
  public void register(Iterable<Binding> descriptors) {
    descriptors.forEach(this::register);
  }

  @Override
  public void register(Binder binder) {
    Bindings.getBindings(this, binder).forEach(this::register);
  }

  @Override
  public void register(Object provider) throws IllegalArgumentException {
    if (!isRegistrable(provider.getClass())) {
      throw new IllegalArgumentException("Provider is not registrable");
    }

    modulesBuilder.add((Module) provider);
  }

  @Override
  public boolean isRegistrable(Class<?> clazz) {
    return Module.class.isAssignableFrom(clazz);
  }

  @Override
  public <T> T createAndInitialize(Class<T> createMe) {
    LOG.trace("Creating new instance of {}", createMe.getCanonicalName());

    // TODO should we just create the instance directly instead of using a shared injector?
    return INJECTOR.getInstance(createMe);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> List<ServiceHolder<T>> getAllServiceHolders(
      Class<T> contractOrImpl, Annotation... qualifiers) {
    List<ServiceHolder<T>> result = new ArrayList<>();
    final TypeLiteral<Set<T>> setTypeLiteral =
        (TypeLiteral<Set<T>>) TypeLiteral.get(Types.setOf(contractOrImpl));
    final Set<T> instances = injector.getInstance(Key.get(setTypeLiteral));
    instances
        .stream()
        .map(t -> new ServiceHolderImpl(t, Collections.singleton(contractOrImpl)))
        .forEach(result::add);

    return result;
  }

  @Override
  public <T> T getInstance(Class<T> contractOrImpl, Annotation... qualifiers) {
    // TODO is it enough to simply take the first element?
    return getInstance(
        qualifiers.length == 0 ? Key.get(contractOrImpl) : Key.get(contractOrImpl, qualifiers[0]));
  }

  @Override
  public <T> T getInstance(Class<T> contractOrImpl, String classAnalyzer) {
    return getInstance(contractOrImpl);
  }

  @Override
  public <T> T getInstance(Class<T> contractOrImpl) {
    return getInstance(Key.get(contractOrImpl));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getInstance(Type contractOrImpl) {
    return (T) getInstance(Key.get(contractOrImpl));
  }

  @Override
  public Object getInstance(ForeignDescriptor foreignDescriptor) {
    return null;
  }

  private <T> T getInstance(Key<T> key) {
    return injector.getInstance(key);
  }

  @Override
  public ForeignDescriptor createForeignDescriptor(Binding binding) {
    return null;
  }

  @Override
  public <T> List<T> getAllInstances(Type contractOrImpl) {
    final Key<?> key = Key.get(contractOrImpl);
    return Collections.emptyList();
  }

  @Override
  public void inject(Object injectMe) {
    injector.injectMembers(injectMe);
  }

  @Override
  public void inject(Object injectMe, String classAnalyzer) {
    // according to javadoc classAnalyzer is only used for legacy CDI integration, so we ignore it
    inject(injectMe);
  }

  @Override
  public void preDestroy(Object preDestroyMe) {}

  public Injector injector() {
    return injector;
  }
}
