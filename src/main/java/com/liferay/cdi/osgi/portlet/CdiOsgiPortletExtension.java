package com.liferay.cdi.osgi.portlet;

import java.lang.annotation.Annotation;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.portlet.Portlet;
import javax.portlet.annotations.PortletApplication;
import javax.portlet.annotations.PortletConfiguration;
import javax.portlet.annotations.PortletConfigurations;
import javax.portlet.annotations.PortletLifecycleFilter;
import javax.portlet.annotations.PortletListener;
import javax.portlet.annotations.PortletPreferencesValidator;

import org.apache.pluto.container.bean.processor.AnnotatedMethodStore;
import org.apache.pluto.container.bean.processor.ConfigSummary;
import org.apache.pluto.container.bean.processor.InvalidAnnotationException;
import org.apache.pluto.container.bean.processor.PortletAnnotationRecognizer;
import org.apache.pluto.container.bean.processor.PortletInvoker;
import org.apache.pluto.container.om.portlet.InitParam;
import org.apache.pluto.container.om.portlet.PortletDefinition;
import org.apache.pluto.container.om.portlet.Supports;
import org.apache.pluto.container.om.portlet.impl.ConfigurationHolder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdiOsgiPortletExtension implements Extension {

	private static final Logger _log = LoggerFactory.getLogger(CdiOsgiPortletExtension.class);

	private final List<Entry<Portlet, Dictionary<String, Object>>> portlets = new CopyOnWriteArrayList<>();
	private final List<ServiceRegistration<Portlet>> registrations = new CopyOnWriteArrayList<>();
	private final Set<Class<?>> portletConfigurationBeanClasses = ConcurrentHashMap.newKeySet();
	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();
	private final ConfigSummary configSummary = new ConfigSummary();
	private final AnnotatedMethodStore methodStore = new AnnotatedMethodStore(configSummary);
	private final ConfigurationHolder holder = new ConfigurationHolder();
	private final PAR par;

	private final List<Class<? extends Annotation>> CONFIGRATION_ANNOTATIONS = Arrays.asList(
		PortletApplication.class,
		PortletConfiguration.class,
		PortletConfigurations.class,
		PortletLifecycleFilter.class,
		PortletListener.class,
		PortletPreferencesValidator.class
	);

	class PAR extends PortletAnnotationRecognizer {

		public PAR(AnnotatedMethodStore pms, ConfigSummary summary) {
			super(pms, summary);
		}

		@Override
		public void checkForMethodAnnotations(Class<?> aClass) {
			super.checkForMethodAnnotations(aClass);
		}

	}

	public CdiOsgiPortletExtension() {
		holder.setConfigSummary(configSummary);
		holder.setMethodStore(methodStore);
		par = new PAR(methodStore, configSummary);
	}

	void afterTypeDiscovery(@Observes AfterTypeDiscovery atd, BeanManager bm) {
		try {
			holder.processConfigAnnotations(portletConfigurationBeanClasses);

			if (_log.isDebugEnabled()) {
				_log.debug(
					"CdiOsgiPortletExtension on {} for annotations {}",
					bc.getBundle(), portletConfigurationBeanClasses);
			}

			holder.validate();

			// Reconcile the bean config with the explicitly declared portlet configuration.

			holder.reconcileBeanConfig();
			holder.instantiatePortlets(bm);

			// If portlets have been found create the Portlet instances

			for (PortletDefinition pd : holder.getPad().getPortlets()) {
				if (_log.isDebugEnabled()) {
					_log.debug("Creating Portlet {}", pd.getPortletName());
				}

				portlets.add(
					new AbstractMap.SimpleEntry<>(
						new PortletInvoker(holder.getMethodStore(), pd.getPortletName()),
						toDictionary(pd)));
			}
		}
		catch (Exception e) {
			_log.error(e.getMessage(), e);
		}
	}

	void afterDeploymentValidation(@Observes AfterDeploymentValidation adv, BeanManager bm) {
		par.getStateScopedConfig().activate(bm);
		par.getSessionScopedConfig().activate(bm);
	}

	void applicationScopedInitialized(
		@Observes @Initialized(ApplicationScoped.class) Object ignore, BundleContext bc) {

		registrations.addAll(
			portlets.stream().map(
				entry -> register(bc, entry)
			).collect(Collectors.toList())
		);
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

	void processAnnotatedType(@Observes ProcessAnnotatedType<?> pat) {
		try {
			par.checkAnnotatedType(pat);

			Class<?> javaClass = pat.getAnnotatedType().getJavaClass();

			// TODO This assumes every portlet method class is also somehow
			// bean annotated

			par.checkForMethodAnnotations(javaClass);

			for (Class<? extends Annotation> anno : CONFIGRATION_ANNOTATIONS) {
				if (javaClass.getAnnotation(anno) != null) {
					portletConfigurationBeanClasses.add(javaClass);

					break;
				}
			}
		}
		catch (InvalidAnnotationException e) {
			_log.error(e.getMessage(), e);
		}
	}

	ServiceRegistration<Portlet> register(
		BundleContext bc, Entry<Portlet, Dictionary<String, Object>> entry) {

		return bc.registerService(Portlet.class, entry.getKey(), entry.getValue());
	}

	Dictionary<String, Object> toDictionary(PortletDefinition pd) {
		Dictionary<String, Object> properties = new Hashtable<>();

		properties.put("javax.portlet.description", pd.getDescriptions());
		properties.put("javax.portlet.name", pd.getPortletName());
		properties.put("javax.portlet.display-name", pd.getDisplayNames());

		for (InitParam param : pd.getInitParams()) {
			properties.put(
				"javax.portlet.init-param." + param.getParamName(),
				param.getParamValue());
		}

		properties.put("javax.portlet.expiration-cache", pd.getExpirationCache());
		properties.put(
			"javax.portlet.mime-type",
			pd.getSupports().stream().map(
				s -> s.getMimeType()
			).collect(Collectors.toList())
		);
		properties.put(
			"javax.portlet.portlet-mode",
			pd.getSupports().stream().map(
				s -> s.getMimeType() + ";" + s.getPortletModes().stream().collect(Collectors.joining(","))
			).collect(Collectors.toList())
		);
		properties.put(
			"javax.portlet.window-state",
			pd.getSupports().stream().map(
				s -> s.getMimeType() + ";" + s.getWindowStates().stream().collect(Collectors.joining(","))
			).collect(Collectors.toList())
		);
		properties.put("javax.portlet.resource-bundle", pd.getResourceBundle());
		properties.put("javax.portlet.info.title", pd.getPortletInfo().getTitle());
		properties.put("javax.portlet.info.short-title", pd.getPortletInfo().getShortTitle());
		properties.put("javax.portlet.info.keywords", pd.getPortletInfo().getKeywords());
		properties.put("javax.portlet.preferences", pd.getPortletPreferences());
		//properties.put("javax.portlet.preferences", "classpath:<path_to_file_in_jar>");
		properties.put(
			"javax.portlet.security-role-ref",
			pd.getSecurityRoleRefs().stream().map(
				srr -> srr.getRoleName() + ',' + srr.getRoleLink()
			).collect(Collectors.toList())
		);
		properties.put(
			"javax.portlet.supported-processing-event",
			pd.getSupportedProcessingEvents().stream().map(
				pe -> pe.getQualifiedName()
			).map(
				qn -> qn.getLocalPart() + ((qn.getNamespaceURI() != null) ? ";" + qn.getNamespaceURI() : "")
			).collect(Collectors.toList())
		);
		properties.put(
			"javax.portlet.supported-publishing-event",
			pd.getSupportedPublishingEvents().stream().map(
				pe -> pe.getQualifiedName()
			).map(
				qn -> qn.getLocalPart() + ((qn.getNamespaceURI() != null) ? ";" + qn.getNamespaceURI() : "")
			).collect(Collectors.toList())
		);
		properties.put(
			"javax.portlet.supported-public-render-parameter",
			pd.getSupportedPublicRenderParameters()
		);

		/*
		 * TODO Liferay specific ones...
		/liferay-portlet-app/portlet/icon 	com.liferay.portlet.icon=<String>
		/liferay-portlet-app/portlet/virtual-path 	com.liferay.portlet.virtual-path=<String>
		/liferay-portlet-app/portlet/struts-path 	com.liferay.portlet.struts-path=<String>
		/liferay-portlet-app/portlet/parent-struts-path 	com.liferay.portlet.parent-struts-path=<String>
		/liferay-portlet-app/portlet/configuration-path 	com.liferay.portlet.configuration-path=<String>
		/liferay-portlet-app/portlet/friendly-url-mapping 	com.liferay.portlet.friendly-url-mapping=<String>
		/liferay-portlet-app/portlet/friendly-url-routes 	com.liferay.portlet.friendly-url-routes=<String>
		/liferay-portlet-app/portlet/control-panel-entry-category 	com.liferay.portlet.control-panel-entry-category=<String>
		/liferay-portlet-app/portlet/control-panel-entry-weight 	com.liferay.portlet.control-panel-entry-weight=<double>
		/liferay-portlet-app/portlet/preferences-company-wide 	com.liferay.portlet.preferences-company-wide=<boolean>
		/liferay-portlet-app/portlet/preferences-unique-per-layout 	com.liferay.portlet.preferences-unique-per-layout=<boolean>
		/liferay-portlet-app/portlet/preferences-owned-by-group 	com.liferay.portlet.preferences-owned-by-group=<boolean>
		/liferay-portlet-app/portlet/use-default-template 	com.liferay.portlet.use-default-template=<boolean>
		/liferay-portlet-app/portlet/show-portlet-access-denied 	com.liferay.portlet.show-portlet-access-denied=<boolean>
		/liferay-portlet-app/portlet/show-portlet-inactive 	com.liferay.portlet.show-portlet-inactive=<boolean>
		/liferay-portlet-app/portlet/action-url-redirect 	com.liferay.portlet.action-url-redirect=<boolean>
		/liferay-portlet-app/portlet/restore-current-view 	com.liferay.portlet.restore-current-view=<boolean>
		/liferay-portlet-app/portlet/maximize-edit 	com.liferay.portlet.maximize-edit=<boolean>
		/liferay-portlet-app/portlet/maximize-help 	com.liferay.portlet.maximize-help=<boolean>
		/liferay-portlet-app/portlet/pop-up-print 	com.liferay.portlet.pop-up-print=<boolean>
		/liferay-portlet-app/portlet/layout-cacheable 	com.liferay.portlet.layout-cacheable=<boolean>
		/liferay-portlet-app/portlet/instanceable 	com.liferay.portlet.instanceable=<boolean>
		/liferay-portlet-app/portlet/remoteable 	com.liferay.portlet.remoteable=<boolean>
		/liferay-portlet-app/portlet/scopeable 	com.liferay.portlet.scopeable=<boolean>
		/liferay-portlet-app/portlet/single-page-application 	com.liferay.portlet.single-page-application=<boolean>
		/liferay-portlet-app/portlet/user-principal-strategy 	com.liferay.portlet.user-principal-strategy=<String>
		/liferay-portlet-app/portlet/private-request-attributes 	com.liferay.portlet.private-request-attributes=<boolean>
		/liferay-portlet-app/portlet/private-session-attributes 	com.liferay.portlet.private-session-attributes=<boolean>
		/liferay-portlet-app/portlet/autopropagated-parameters 	com.liferay.portlet.autopropagated-parameters=<String>2
		/liferay-portlet-app/portlet/requires-namespaced-parameters 	com.liferay.portlet.requires-namespaced-parameters=<boolean>
		/liferay-portlet-app/portlet/action-timeout 	com.liferay.portlet.action-timeout=<int>
		/liferay-portlet-app/portlet/render-timeout 	com.liferay.portlet.render-timeout=<int>
		/liferay-portlet-app/portlet/render-weight 	com.liferay.portlet.render-weight=<int>
		/liferay-portlet-app/portlet/ajaxable 	com.liferay.portlet.ajaxable=<boolean>
		/liferay-portlet-app/portlet/header-portal-css 	com.liferay.portlet.header-portal-css=<String>2
		/liferay-portlet-app/portlet/header-portlet-css 	com.liferay.portlet.header-portlet-css=<String>2
		/liferay-portlet-app/portlet/header-portal-javascript 	com.liferay.portlet.header-portal-javascript=<String>2
		/liferay-portlet-app/portlet/header-portlet-javascript 	com.liferay.portlet.header-portlet-javascript=<String>2
		/liferay-portlet-app/portlet/footer-portal-css 	com.liferay.portlet.footer-portal-css=<String>2
		/liferay-portlet-app/portlet/footer-portlet-css 	com.liferay.portlet.footer-portlet-css=<String>2
		/liferay-portlet-app/portlet/footer-portal-javascript 	com.liferay.portlet.footer-portal-javascript=<String>2
		/liferay-portlet-app/portlet/footer-portlet-javascript 	com.liferay.portlet.footer-portlet-javascript=<String>2
		/liferay-portlet-app/portlet/css-class-wrapper 	com.liferay.portlet.css-class-wrapper=<String>
		/liferay-portlet-app/portlet/facebook-integration 	com.liferay.portlet.facebook-integration=<String>
		/liferay-portlet-app/portlet/add-default-resource 	com.liferay.portlet.add-default-resource=<boolean>
		/liferay-portlet-app/portlet/system 	com.liferay.portlet.system=<boolean>
		/liferay-portlet-app/portlet/active 	com.liferay.portlet.active=<boolean>
		 */

		return properties;
	}

}
