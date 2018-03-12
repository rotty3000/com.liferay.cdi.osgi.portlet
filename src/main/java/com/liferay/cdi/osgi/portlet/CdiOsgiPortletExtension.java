package com.liferay.cdi.osgi.portlet;

import java.util.Dictionary;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.portlet.Portlet;

import org.apache.pluto.container.PortletInvokerService;
import org.apache.pluto.container.om.portlet.PortletDefinition;
import org.apache.pluto.container.om.portlet.impl.ConfigurationHolder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdiOsgiPortletExtension implements Extension {

	private static final Logger _log = LoggerFactory.getLogger(CdiOsgiPortletExtension.class);

	private List<Entry<Portlet, Dictionary<String, Object>>> portlets = new CopyOnWriteArrayList<>();
	private List<ServiceRegistration<Portlet>> registrations = new CopyOnWriteArrayList<>();

	private volatile BundleContext bc;

	//
	// TODO populate 'portlets' list
	//

	void buildPortlets(Set<Class<?>> classes) throws Exception {
		try {
			// scan for method annotations

			ConfigurationHolder holder = new ConfigurationHolder();
			//holder.scanMethodAnnotations(ctx);

			// Read the annotated configuration

			if (classes != null) {
				holder.processConfigAnnotations(classes);
			}

			if (_log.isDebugEnabled()) {
				_log.debug("CdiOsgiPortletExtension on {} for annotations {}", bc.getBundle(), classes);
			}

			holder.validate();

			// Reconcile the bean config with the explicitly declared portlet configuration.

			holder.reconcileBeanConfig();

			// If portlets have been found in this servlet context, launch the portlet servlets

			for (PortletDefinition pd : holder.getPad().getPortlets()) {
				String pn = pd.getPortletName();
				String mapping = PortletInvokerService.URIPREFIX + pn;
				String servletName = pn + "_PS3";

				if (_log.isDebugEnabled()) {
					StringBuilder sb = new StringBuilder();
					sb.append("Adding Portlet: ");
					sb.append(pn);
					sb.append(", servlet name: ").append(servletName);
					sb.append(", mapping: ").append(mapping);
					_log.debug(sb.toString());
				}

				// make portlet
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

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
