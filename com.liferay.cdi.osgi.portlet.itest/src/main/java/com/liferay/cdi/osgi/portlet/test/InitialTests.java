package com.liferay.cdi.osgi.portlet.test;

import static org.junit.Assert.*;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import com.liferay.cdi.osgi.portlet.test.mock.BundleWrapper;
import com.liferay.cdi.osgi.portlet.test.mock.MockCdiContainer;

public class InitialTests {

	volatile BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();

	@Test
	public void test1() throws Exception {
		try (BundleWrapper bw = new BundleWrapper(bc, "testbundle1.jar");
			MockCdiContainer c = new MockCdiContainer(
				"test1", MockCdiContainer.bundles(bw.getBundle()))) {

			assertNotNull(c.getBeanManager());
		}
	}

}
