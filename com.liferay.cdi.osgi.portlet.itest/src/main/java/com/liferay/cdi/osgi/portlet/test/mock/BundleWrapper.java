package com.liferay.cdi.osgi.portlet.test.mock;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class BundleWrapper implements AutoCloseable {

	public BundleWrapper(BundleContext bundleContext, String path) throws BundleException {
		bundle = bundleContext.installBundle(path, getClass().getClassLoader().getResourceAsStream(path));
		bundle.start();
	}

	@Override
	public void close() throws Exception {
		bundle.uninstall();
	}

	public Bundle getBundle() {
		return bundle;
	}

	private final Bundle bundle;
}