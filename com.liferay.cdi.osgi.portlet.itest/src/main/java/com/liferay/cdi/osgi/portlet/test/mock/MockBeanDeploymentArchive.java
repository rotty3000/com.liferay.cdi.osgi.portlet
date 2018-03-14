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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.serialization.spi.ProxyServices;

public class MockBeanDeploymentArchive implements BeanDeploymentArchive {

	public <T extends ResourceLoader & ProxyServices> MockBeanDeploymentArchive(
		String id, T loader, String... beanClasses) {

		_id = id;
		_beanClasses = Arrays.asList(beanClasses);

		_services = new SimpleServiceRegistry();

		if (loader != null) {
			_services.add(ResourceLoader.class, loader);
			_services.add(ProxyServices.class, loader);
		}
	}

	@Override
	public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
		return Collections.emptyList();
	}

	@Override
	public Collection<String> getBeanClasses() {
		return _beanClasses;
	}

	@Override
	public BeansXml getBeansXml() {
		return BeansXml.EMPTY_BEANS_XML;
	}

	@Override
	public Collection<EjbDescriptor<?>> getEjbs() {
		return Collections.emptyList();
	}

	@Override
	public ServiceRegistry getServices() {
		return _services;
	}

	@Override
	public String getId() {
		return _id;
	}

	private final List<String> _beanClasses;
	private final String _id;
	private final ServiceRegistry _services;

}