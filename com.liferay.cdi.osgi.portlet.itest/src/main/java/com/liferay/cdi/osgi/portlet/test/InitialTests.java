package com.liferay.cdi.osgi.portlet.test;

import static org.junit.Assert.*;
import org.junit.Test;

import com.liferay.cdi.osgi.portlet.test.mock.MockCdiContainer;

public class InitialTests {

	@Test
	public void test1() throws Exception {
		try (MockCdiContainer c = new MockCdiContainer("test1")) {
			assertNotNull(c.getBeanManager());
		}
	}

}
