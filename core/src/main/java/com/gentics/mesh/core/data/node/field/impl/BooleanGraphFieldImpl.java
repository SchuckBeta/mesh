package com.gentics.mesh.core.data.node.field.impl;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.field.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.CompareUtils;
import com.syncleus.ferma.AbstractVertexFrame;

import rx.Observable;

public class BooleanGraphFieldImpl extends AbstractBasicField<BooleanField> implements BooleanGraphField {

	public BooleanGraphFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}

	@Override
	public void setBoolean(Boolean bool) {
		if (bool == null) {
			setFieldProperty("boolean", null);
		} else {
			setFieldProperty("boolean", String.valueOf(bool));
		}
	}

	@Override
	public Boolean getBoolean() {
		String fieldValue = getFieldProperty("boolean");
		if (fieldValue == null || fieldValue.equals("null")) {
			return null;
		}
		return Boolean.valueOf(fieldValue);
	}

	@Override
	public Observable<BooleanField> transformToRest(ActionContext ac) {
		BooleanFieldImpl restModel = new BooleanFieldImpl();
		restModel.setValue(getBoolean());
		return Observable.just(restModel);
	}

	@Override
	public void removeField(GraphFieldContainer container) {
		setFieldProperty("boolean", null);
		setFieldKey(null);
	}

	@Override
	public BooleanGraphField cloneTo(GraphFieldContainer container) {
		BooleanGraphField clone = container.createBoolean(getFieldKey());
		clone.setBoolean(getBoolean());
		return clone;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BooleanGraphField) {
			Boolean valueA = getBoolean();
			Boolean valueB = ((BooleanGraphField) obj).getBoolean();
			return CompareUtils.equals(valueA, valueB);
		}
		if (obj instanceof BooleanField) {
			Boolean valueA = getBoolean();
			Boolean valueB = ((BooleanField) obj).getValue();
			return CompareUtils.equals(valueA, valueB);
		}
		return false;
	}
}