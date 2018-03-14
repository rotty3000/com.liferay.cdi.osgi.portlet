/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liferay.cdi.osgi.portlet.test.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.junit.Assert;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class MockCdiContainer implements AutoCloseable {

	static Bundle bundle = FrameworkUtil.getBundle(MockCdiContainer.class);

	private static String[] toClasses(Bundle[] bundles) {
		return Arrays.stream(bundles).map(
			b -> (String)b.getHeaders().get("X-Bean-Classes")
		).filter(s -> Objects.nonNull(s)).flatMap(
			s -> Arrays.stream(s.split(","))
		).toArray(String[]::new);
	}

	public static Bundle[] bundles(Bundle... bundles) {
		List<Bundle> bs = new ArrayList<>(Arrays.asList(bundles));

		bs.add(bundle);

		Arrays.stream(
			bundle.getBundleContext().getBundles()
		).filter(
			b ->
				"com.liferay.cdi.osgi.portlet".equals(b.getSymbolicName()) ||
				"org.jboss.weld.osgi-bundle".equals(b.getSymbolicName())
		).forEach(bs::add);

		return bs.toArray(new Bundle[0]);
	}

	@SuppressWarnings("unchecked")
	private static <T extends ResourceLoader & ProxyServices> T loader(Bundle... bundles) {
		return (T)new BundleResourcesLoader(bundles);
	}

	public MockCdiContainer(String name, String... beanClasses) {
		this(name, loader(bundles()), beanClasses);
	}

	public MockCdiContainer(String name, Bundle... bundles) {
		this(name, loader(bundles), toClasses(bundles));
	}

	public <T extends ResourceLoader & ProxyServices> MockCdiContainer(
		String name, T loader, String... beanClasses) {

		_bda = new MockBeanDeploymentArchive(name, loader, beanClasses);

		List<Metadata<Extension>> extensions = new ArrayList<>();

		ServiceLoader<Extension> sl = ServiceLoader.load(
			Extension.class,
			loader.getClassLoader(null));

		sl.forEach(e -> extensions.add(meta(e)));

		Deployment deployment = new MockContainerDeployment(extensions, _bda);

		WeldBootstrap bootstrap = new WeldBootstrap();

		bootstrap.startExtensions(extensions);
		bootstrap.startContainer(new MockEnvironment(), deployment);
		bootstrap.startInitialization();
		bootstrap.deployBeans();
		bootstrap.validateBeans();
		bootstrap.endInitialization();

		_bootstrap = bootstrap;
	}

	@Override
	public void close() {
		_bootstrap.shutdown();
	}

	public Bean<?> getBean(Class<?> clazz) {
		final BeanManager managerImpl = getBeanManager();

		Set<javax.enterprise.inject.spi.Bean<?>> beans =
			managerImpl.getBeans(clazz, Any.Literal.INSTANCE);

		Assert.assertFalse(beans.isEmpty());

		return managerImpl.resolve(beans);
	}

	public BeanManager getBeanManager() {
		if (_beanManager != null) {
			return _beanManager;
		}

		return _beanManager = _bootstrap.getManager(_bda);
	}

	public WeldBootstrap getBootstrap() {
		return _bootstrap;
	}

	Metadata<Extension> meta(Extension e) {
		return new Metadata<Extension>() {

			@Override
			public Extension getValue() {
				return e;
			}

			@Override
			public String getLocation() {
				return e.toString();
			}
		};
	}

	private final BeanDeploymentArchive _bda;
	private BeanManager _beanManager;
	private final WeldBootstrap _bootstrap;

}