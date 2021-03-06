package ru.otus.appcontainer;

import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import ru.otus.adapters.impl.ClassAdapter;
import ru.otus.adapters.impl.PackageAdapter;
import ru.otus.appcontainer.api.AppComponent;
import ru.otus.appcontainer.api.AppComponentsContainer;
import ru.otus.appcontainer.api.AppComponentsContainerConfig;

import java.lang.reflect.Method;
import java.util.*;

public class AppComponentsContainerImpl implements AppComponentsContainer {

	private final List<Object> appComponents = new ArrayList<>();
	private final Map<String, Object> appComponentsByName = new HashMap<>();
	private final Reflections reflections;

	public AppComponentsContainerImpl(final Class<?>... classes) {
		this.reflections = new ClassAdapter(classes).getReflections();
		processConfig();
	}

	public AppComponentsContainerImpl(final String packageName) {
		this.reflections = new PackageAdapter(packageName).getReflections();
		processConfig();
	}

	private void processConfig() {
		final List<Class<?>> configs = new ArrayList<>(
				reflections.getTypesAnnotatedWith(AppComponentsContainerConfig.class)
		);

		if (configs.isEmpty()) {
			throw new IllegalArgumentException("configuration classes not found!");
		}

		sortConfigsByOrder(configs);

		configs.forEach((clazz) -> {
			final Object appConfig = initAppConfig(clazz);
			final List<Method> methods = getAnnotatedMethods(clazz);

			if (methods.isEmpty()) {
				throw new IllegalArgumentException("components not found!");
			}

			sortMethodsByOrder(methods);
			fillComponentsStore(methods, appConfig);
		});
	}

	private void sortMethodsByOrder(final List<Method> methods) {
		methods.sort((m1, m2) -> {
			final int order1 = m1.getAnnotation(AppComponent.class).order();
			final int order2 = m2.getAnnotation(AppComponent.class).order();

			return Integer.compare(order1, order2);
		});
	}

	private void sortConfigsByOrder(final List<Class<?>> configs) {
		configs.sort((c1, c2) -> {
			final int order1 = c1.getAnnotation(AppComponentsContainerConfig.class).order();
			final int order2 = c2.getAnnotation(AppComponentsContainerConfig.class).order();

			return Integer.compare(order1, order2);
		});
	}

	private void fillComponentsStore(final List<Method> methods, final Object appConfig) {
		methods.forEach((method) -> {
			final String componentName = method.getAnnotation(AppComponent.class).name();
			final Object[] args = getArguments(method);
			final Object result = invokeMethod(appConfig, method, args);

			appComponents.add(result);
			appComponentsByName.put(componentName, result);
		});
	}

	private Object[] getArguments(final Method method) {
		return Arrays.stream(method.getParameters())
				.map((parameter) ->
						appComponents.stream().filter((component) ->
								parameter.getType().isAssignableFrom(component.getClass())
						).findFirst().orElse(null)
				).toArray();
	}

	@SuppressWarnings("unchecked")
	private List<Method> getAnnotatedMethods(final Class<?> clazz) {
		return new ArrayList<>(
				ReflectionUtils.getMethods(
						clazz,
						(method) -> Objects.requireNonNull(method).isAnnotationPresent(AppComponent.class)
				)
		);
	}

	private Object initAppConfig(final Class<?> clazz) {
		try {
			return clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("class for configuration must have a default constructor!", e);
		}
	}

	private Object invokeMethod(final Object owner, final Method method, final Object... args) {
		try {
			return method.invoke(owner, args);
		} catch (Exception e) {
			throw new RuntimeException(String.format("method execution error, method name: %s", method.getName()), e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <C> C getAppComponent(final Class<C> componentClass) {
		return (C) appComponents.stream().filter((component) ->
				componentClass.isAssignableFrom(component.getClass())
		).findFirst().orElse(null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <C> C getAppComponent(final String componentName) {
		return (C) appComponentsByName.get(componentName);
	}
}
