package com.liferay.cdi.osgi.portlet;

import java.util.Dictionary;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.portlet.Portlet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdiOsgiPortletExtension implements Extension {

	private static final Logger _log = LoggerFactory.getLogger(CdiOsgiPortletExtension.class);

	private List<Entry<Portlet, Dictionary<String, Object>>> portlets = new CopyOnWriteArrayList<>();
	private List<ServiceRegistration<Portlet>> registrations = new CopyOnWriteArrayList<>();

	//
	// TODO populate 'portlets' list
	//

	void applicationScopedInitialized(
		@Observes @Initialized(ApplicationScoped.class) Object ignore, BundleContext bc) {

		registrations = portlets.stream().map(
			entry -> register(bc, entry)
		).collect(Collectors.toList());
	}

	void applicationScopedBeforeDestroyed(
		@Observes @Destroyed(ApplicationScoped.class) Object ignore) {

		if (registrations != null) {
			registrations.removeIf(
				r -> {
					r.unregister();

					return true;
				}
			);
		}
	}

	ServiceRegistration<Portlet> register(
		BundleContext bc, Entry<Portlet, Dictionary<String, Object>> entry) {

		return bc.registerService(Portlet.class, entry.getKey(), entry.getValue());
	}

}
