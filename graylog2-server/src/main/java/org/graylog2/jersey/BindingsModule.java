package org.graylog2.jersey;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Scope;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.function.Consumer;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.ClassBinding;
import org.glassfish.jersey.internal.inject.Custom;
import org.glassfish.jersey.internal.inject.InjectionResolver;
import org.glassfish.jersey.internal.inject.InjectionResolverBinding;
import org.glassfish.jersey.internal.inject.InstanceBinding;
import org.glassfish.jersey.internal.inject.PerLookup;
import org.glassfish.jersey.internal.inject.PerThread;
import org.glassfish.jersey.internal.inject.SupplierClassBinding;
import org.glassfish.jersey.internal.inject.SupplierInstanceBinding;
import org.glassfish.jersey.internal.spi.AutoDiscoverable;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.spi.internal.ValueParamProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BindingsModule extends AbstractModule {
  private static final Logger LOG = LoggerFactory.getLogger(BindingsModule.class);

  // the set of keys that we know we have to use set binders for
  private static final Set<Key<?>> MULTIBINDERS =
      ImmutableSet.of(
          Key.get(ValueParamProvider.class),
          Key.get(AutoDiscoverable.class),
          Key.get(ModelProcessor.class),
          Key.get(MessageBodyReader.class),
          Key.get(MessageBodyWriter.class),
          Key.get(MessageBodyReader.class, Custom.class),
          Key.get(MessageBodyWriter.class, Custom.class));

  private final ImmutableSet<Binding> bindings;
  private ImmutableMap<Key, Multibinder> setBinders;

  public BindingsModule(ImmutableSet<Binding> bindings) {
    this.bindings = bindings;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void configure() {
    // TODO this is wrong, we can probably detect whether something is an extension point by some
    // other means
    ImmutableMap.Builder<Key, Multibinder> multibinderBuilder = ImmutableMap.builder();
    for (Key<?> key : MULTIBINDERS) {
      multibinderBuilder.put(key, Multibinder.newSetBinder(binder(), key));
    }
    setBinders = multibinderBuilder.build();

    // Jersey has five different bindings, which are dispatched here.
    // it's not handled inline because of the generics and type cast nightmare
    // each of the actual handler methods also does the specific logging for each
    // bound type or provider
    for (Binding binding : bindings) {
      if (binding instanceof InstanceBinding) {
        bind((InstanceBinding) binding);
      } else if (binding instanceof ClassBinding) {
        bind((ClassBinding) binding);
      } else if (binding instanceof InjectionResolverBinding) {
        bind((InjectionResolverBinding) binding);
      } else if (binding instanceof SupplierClassBinding) {
        bind((SupplierClassBinding) binding);
      } else if (binding instanceof SupplierInstanceBinding) {
        bind((SupplierInstanceBinding) binding);
      }
    }
  }

  private <T> void maybeSetBinder(Key<T> key, Consumer<LinkedBindingBuilder<T>> linkedBindingBuilderConsumer) {
    if (setBinders.containsKey(key)) {
      //noinspection unchecked
      linkedBindingBuilderConsumer.accept(setBinders.get(key).addBinding());
    } else {
      linkedBindingBuilderConsumer.accept(bind(key));
    }
  }

  private <T> void bind(ClassBinding<T> cb) {
    final Scope guiceScope = jerseyToGuiceScope(cb.getScope());

    if (cb.getQualifiers().isEmpty()) {
      LOG.trace("[ClassBinding] {} in scope {}", cb.getService(), guiceScope);
      // there are not annotations, we can bind it directly
      bind(cb.getService()).in(guiceScope);
    } else {
      cb.getQualifiers()
          .forEach(
              annotation -> {
                LOG.trace(
                    "[ClassBinding]    {} (qualifier {}) in scope {}",
                    cb.getService(),
                    annotation,
                    guiceScope);
                bind(cb.getService()).annotatedWith(annotation).in(guiceScope);
              });
    }
  }

  private <T extends InjectionResolver> void bind(InjectionResolverBinding<T> irb) {
    final T resolver = irb.getResolver();
    if (irb.getContracts().isEmpty()) {
      LOG.trace("[InjectionResolverBinding] injection resolver: {}", resolver.getClass().getCanonicalName());
    } else {
      irb.getContracts()
          .forEach(
              type -> {
                final Scope guiceScope = jerseyToGuiceScope(irb.getScope());
                LOG.trace(
                    "[InjectionResolverBinding]    {} to injection resolver {} in scope {}",
                    type.getTypeName(),
                    resolver,
                    guiceScope);
                maybeSetBinder(typeLiteralKey(type), b -> b.toInstance(resolver));
              });
    }
  }

  private <T> void bind(InstanceBinding<T> ib) {
    if (ib.getContracts().isEmpty()) {
      LOG.error("[InstanceBinding] instance without contracts: {}", ib.getService());
    } else {
      ib.getContracts()
          .forEach(
              type -> {
                final Key<Object> key = typeLiteralKey(type);
                LOG.trace("[InstanceBinding]    {} to instance {}", type.getTypeName(), ib.getService());
                maybeSetBinder(key, b -> b.toInstance(ib.getService()));
//                if (setBinders.containsKey(key)) {
//                  // the binding is for a set binder
//                  LOG.trace(
//                      "[InstanceBinding]    set binder {} to instance {}", type.getTypeName(), ib.getService());
//                  //noinspection unchecked
//                  setBinders.get(key).addBinding().toInstance(ib.getService());
//                } else {
//                  // standard instance binding
//                  LOG.trace("[InstanceBinding]    {} to instance {}", type.getTypeName(), ib.getService());
//                  bind(key).toInstance(ib.getService());
//                }
              });
    }
  }

  private <T> void bind(SupplierClassBinding<T> scb) {
    final Scope supplierScope = jerseyToGuiceScope(scb.getSupplierScope());
    LOG.trace("[SupplierClassBinding] {} in scope {}", scb.getSupplierClass(), supplierScope);
    // supplier class bindings give us the class out of which we can make objects to supply a
    // certain value
    // thus we bind a provider that can supply that value, a SupplierProvider
    // we then bind each contract of the binding to that

    // first we create the untargetted binding for the supplier class, this allows guice to inject
    // instances of the supplier itself (into our SupplierProvider)
    bind(scb.getSupplierClass()).in(supplierScope);

    scb.getContracts().forEach(contract -> {
      LOG.trace("[SupplierClassBinding]    {} to supplier class {} in scope {}", contract, scb.getSupplierClass(), supplierScope);
      maybeSetBinder(typeLiteralKey(contract), b -> b.to(scb.getSupplierClass()).in(supplierScope));
    });
  }

  private <T> void bind(SupplierInstanceBinding<T> sib) {
    final Scope supplierScope = jerseyToGuiceScope(sib.getScope());
    LOG.trace("[SupplierInstanceBinding] {} in scope {}", sib.getSupplier(), supplierScope);

    // supplier instance bindings already give us the supplier for an instance, so unlike
    // supplier class bindings we only need to bridge them into a provider
    sib.getContracts().forEach(contract -> {
      LOG.trace("[SupplierInstanceBinding]    {} to supplier instance {} in scope {}", contract, sib.getSupplier(), supplierScope);
      maybeSetBinder(typeLiteralKey(contract), b -> b.toProvider(() -> sib.getSupplier().get()).in(supplierScope));
    });
  }

  /**
   * Converts a {@link Type} to the correctly generically typed {@link Key}.
   *
   * @param type type to get the key for
   * @param <T> the generic type of the key
   * @return Key for use with guice
   */
  @SuppressWarnings("unchecked")
  private <T> Key<T> typeLiteralKey(Type type) {
    return Key.get((TypeLiteral<T>) TypeLiteral.get(type));
  }

  /**
   * Converts the Jersey scope annotation into the appropriate Guice scope.
   *
   * @param scope the internal Jersey annotation
   * @return the Guice scope to bind in, defaults to Singleton
   */
  private Scope jerseyToGuiceScope(Class<? extends Annotation> scope) {
    if (scope == PerLookup.class || scope == null) {
      return Scopes.NO_SCOPE;
    } else if (scope == PerThread.class || scope == RequestScoped.class) {
      // TODO custom scope!
      return Scopes.NO_SCOPE;
    }
    return Scopes.SINGLETON;
  }
}
