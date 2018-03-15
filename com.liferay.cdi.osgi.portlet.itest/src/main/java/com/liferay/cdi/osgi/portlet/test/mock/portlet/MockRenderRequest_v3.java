package com.liferay.cdi.osgi.portlet.test.mock.portlet;

import javax.portlet.PortalContext;
import javax.portlet.PortletContext;
import javax.portlet.PortletMode;
import javax.portlet.RenderParameters;
import javax.portlet.WindowState;

import org.springframework.mock.web.portlet.MockRenderRequest;

public class MockRenderRequest_v3 extends MockRenderRequest {

	private PortletContext _portletContext;
	private RenderParameters _renderParameters = new MockRenderParameters();

	public MockRenderRequest_v3() {
		super();
	}

	public MockRenderRequest_v3(PortalContext portalContext, PortletContext portletContext) {
		super(portalContext, portletContext);
		_portletContext = portletContext;
	}

	public MockRenderRequest_v3(PortletContext portletContext) {
		super(portletContext);
		_portletContext = portletContext;
	}

	public MockRenderRequest_v3(PortletMode portletMode, WindowState windowState) {
		super(portletMode, windowState);
	}

	public MockRenderRequest_v3(PortletMode portletMode) {
		super(portletMode);
	}

	@Override
	public PortletContext getPortletContext() {
		return _portletContext;
	}

	@Override
	public String getUserAgent() {
		return "user-agent";
	}

	@Override
	public RenderParameters getRenderParameters() {
		return _renderParameters;
	}

	public void setRenderParameters(RenderParameters renderParameters) {
		_renderParameters = renderParameters;
	}

}
