package com.liferay.cdi.osgi.portlet.test.mock.portlet;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.portlet.MutablePortletParameters;
import javax.portlet.MutableRenderParameters;
import javax.portlet.PortletParameters;
import javax.portlet.RenderParameters;

public class MockRenderParameters implements MutableRenderParameters, RenderParameters {

	private static final String NULL = "$%^&NULL&^%$";

	private final Map<String, List<String>> _parameters = new ConcurrentHashMap<>();

	@Override
	public String getValue(String name) {
		String value = Optional.ofNullable(_parameters.get(name)).map(v -> v.get(0)).orElse(null);
		return NULL.equals(value) ? null : value;
	}

	@Override
	public Set<String> getNames() {
		return _parameters.keySet();
	}

	@Override
	public String[] getValues(String name) {
		List<String> values = Optional.ofNullable(_parameters.get(name)).orElse(Collections.emptyList());
		return values.contains(NULL) ? new String[0] : values.toArray(new String[0]);
	}

	@Override
	public boolean isEmpty() {
		return _parameters.isEmpty();
	}

	@Override
	public int size() {
		return _parameters.size();
	}

	@Override
	public MutableRenderParameters clone() {
		return this;
	}

	@Override
	public boolean isPublic(String name) {
		return false;
	}

	@Override
	public String setValue(String name, String value) {
		value = value == null ? NULL : value;
		List<String> previous = _parameters.put(name, Arrays.asList(value));
		String pre = Optional.ofNullable(previous).map(p -> p.get(0)).orElse(null);
		return pre == NULL ? null : pre;
	}

	@Override
	public String[] setValues(String name, String... values) {
		List<String> v = values == null ? Arrays.asList(NULL) : Arrays.asList(values);
		List<String> previous = _parameters.put(name, v);
		String[] pre = Optional.ofNullable(previous).map(p -> p.toArray(new String[0])).orElse(null);
		return pre == new String[] {NULL} ? null : pre;
	}

	@Override
	public boolean removeParameter(String name) {
		return _parameters.remove(name) != null;
	}

	@Override
	public MutablePortletParameters set(PortletParameters params) {
		clear();
		for (String name : params.getNames()) {
			setValues(name, params.getValues(name));
		}
		return this;
	}

	@Override
	public MutablePortletParameters add(PortletParameters params) {
		for (String name : params.getNames()) {
			setValues(name, params.getValues(name));
		}
		return this;
	}

	@Override
	public void clear() {
		for (String name : getNames()) {
			removeParameter(name);
		}
	}

	@Override
	public void clearPrivate() {
	}

	@Override
	public void clearPublic() {
	}

}
