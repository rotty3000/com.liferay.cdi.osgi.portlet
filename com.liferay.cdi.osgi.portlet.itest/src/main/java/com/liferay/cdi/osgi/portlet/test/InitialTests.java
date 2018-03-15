package com.liferay.cdi.osgi.portlet.test;

import static org.junit.Assert.*;

import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.portlet.Portlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.RenderParameters;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.StateAwareResponse;

import org.apache.pluto.container.bean.processor.AnnotatedConfigBean;
import org.apache.pluto.container.bean.processor.PortletArtifactProducer;
import org.apache.pluto.container.bean.processor.PortletRequestScopedBeanHolder;
import org.apache.pluto.container.bean.processor.PortletSessionBeanHolder;
import org.apache.pluto.container.bean.processor.PortletStateScopedBeanHolder;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.mock.web.portlet.MockPortletConfig;
import org.springframework.mock.web.portlet.MockRenderRequest;
import org.springframework.mock.web.portlet.MockRenderResponse;

import com.liferay.cdi.osgi.portlet.test.mock.BundleWrapper;
import com.liferay.cdi.osgi.portlet.test.mock.MockCdiContainer;
import com.liferay.cdi.osgi.portlet.test.mock.portlet.MockRenderRequest_v3;

public class InitialTests {

	volatile BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();

	@Test
	public void test1() throws Exception {
		try (BundleWrapper bw = new BundleWrapper(bc, "testbundle1.jar");
			MockCdiContainer c = new MockCdiContainer(
				"test1", MockCdiContainer.bundles(bw.getBundle()))) {

			BeanManager bm = c.getBeanManager();
			assertNotNull(bm);

			ServiceTracker<Portlet, Portlet> t = new ServiceTracker<>(bc, Portlet.class, null);

			t.open();

			ServiceReference<Portlet>[] serviceReferences = t.getServiceReferences();

			assertEquals(2, serviceReferences.length);

			{
				ServiceReference<Portlet> sr = serviceReferences[0];
				MockPortletConfig config = new MockPortletConfig((String)sr.getProperty("javax.portlet.name"));
				Portlet portlet = t.getService(sr);
				portlet.init(config);

				MockRenderRequest_v3 request = new MockRenderRequest_v3();
				MockRenderResponse response = new MockRenderResponse();

				this.dispatch(
					(req, res) ->
						portlet.render((RenderRequest)req, (RenderResponse)res),
					request, response, config, bm);
			}
		}
	}

	interface Dispatch {
		void dispatch(PortletRequest request, PortletResponse response) throws Exception;
	}

	void dispatch(
		Dispatch dispatch,
		PortletRequest request,
		PortletResponse response,
		PortletConfig config,
		BeanManager bm) {

		Set<Bean<?>> beans = bm.getBeans(AnnotatedConfigBean.class);
		Bean<?> bean = bm.resolve(beans);
		AnnotatedConfigBean acb = (AnnotatedConfigBean)bm.getReference(
				bean, bean.getBeanClass(), bm.createCreationalContext(bean));

		try {
			beforeInvoke(acb, request, response, config);

			dispatch.dispatch(request, response);
		}
		catch (Throwable th) {
			th.printStackTrace();
		}
		finally {
			afterInvoke(acb, response);
		}
	}

	private void afterInvoke(AnnotatedConfigBean acb, PortletResponse resp) {
		if (acb != null) {
			// Remove the portlet session bean holder for the thread
			PortletRequestScopedBeanHolder.removeBeanHolder();

			// Remove the portlet session bean holder for the thread
			PortletSessionBeanHolder.removeBeanHolder();

			// Remove the render state bean holder. pass response if we're
			// dealing with a StateAwareResponse. The response is used for state
			// storage.

			StateAwareResponse sar = null;
			if (resp instanceof StateAwareResponse) {
				sar = (StateAwareResponse) resp;
			}

			PortletStateScopedBeanHolder.removeBeanHolder(sar);

			// remove the portlet artifact producer
			PortletArtifactProducer.remove();
		}
	}

	private void beforeInvoke(AnnotatedConfigBean acb, PortletRequest req, PortletResponse resp, PortletConfig config) {
		if (acb != null) {
			// Set the portlet session bean holder for the thread & session
			PortletRequestScopedBeanHolder.setBeanHolder();

			// Set the portlet session bean holder for the thread & session
			PortletSessionBeanHolder.setBeanHolder(req, acb.getSessionScopedConfig());

			// Set the render state scoped bean holder
			PortletStateScopedBeanHolder.setBeanHolder(req, acb.getStateScopedConfig());

			// Set up the artifact producer with request, response, and portlet config
			PortletArtifactProducer.setPrecursors(req, resp, config);
		}
	}

}
