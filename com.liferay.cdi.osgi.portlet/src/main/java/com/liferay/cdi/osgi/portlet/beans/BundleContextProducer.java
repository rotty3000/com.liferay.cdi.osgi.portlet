package com.liferay.cdi.osgi.portlet.beans;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

@Dependent // to enable "annotated" bean scanning
public class BundleContextProducer {

	@Produces
	BundleContext bundleContext = FrameworkUtil.getBundle(BundleContextProducer.class).getBundleContext();

}
