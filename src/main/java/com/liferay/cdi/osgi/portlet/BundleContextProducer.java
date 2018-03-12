package com.liferay.cdi.osgi.portlet;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

@ApplicationScoped
public class BundleContextProducer {

	@Produces
	@ApplicationScoped
	BundleContext bundleContext = FrameworkUtil.getBundle(BundleContextProducer.class).getBundleContext();

}
