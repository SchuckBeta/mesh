package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.ADD_FIELD_AFTER_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.LIST_TYPE_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.TYPE_KEY;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.core.data.schema.AddFieldChange;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * @see AddFieldChange
 */
public class AddFieldChangeImpl extends AbstractSchemaFieldChange implements AddFieldChange {

	public static void checkIndices(Database database) {
		database.addVertexType(AddFieldChangeImpl.class);
	}

	@Override
	public SchemaChangeOperation getOperation() {
		return OPERATION;
	}

	@Override
	public AddFieldChange setType(String type) {
		setRestProperty(TYPE_KEY, type);
		return this;
	}

	@Override
	public String getType() {
		return getRestProperty(TYPE_KEY);
	}

	@Override
	public String getListType() {
		return getRestProperty(LIST_TYPE_KEY);
	}

	@Override
	public void setListType(String type) {
		setRestProperty(LIST_TYPE_KEY, type);
	}

	@Override
	public void setInsertAfterPosition(String fieldName) {
		setRestProperty(ADD_FIELD_AFTER_KEY, fieldName);
	}

	@Override
	public String getInsertAfterPosition() {
		return getRestProperty(ADD_FIELD_AFTER_KEY);
	}

	@Override
	public String getLabel() {
		return getRestProperty(SchemaChangeModel.LABEL_KEY);
	}

	@Override
	public void setLabel(String label) {
		setRestProperty(SchemaChangeModel.LABEL_KEY, label);
	}


	@Override
	public FieldSchemaContainer apply(FieldSchemaContainer container) {

		String position = getInsertAfterPosition();
		FieldSchema field = null;
		//TODO avoid case switches like this. We need a central delegator implementation which will be used in multiple places
		switch (getType()) {
		case "html":
			field = new HtmlFieldSchemaImpl();
			break;
		case "string":
			field = new StringFieldSchemaImpl();
			break;
		case "number":
			field = new NumberFieldSchemaImpl();
			break;
		case "binary":
			field = new BinaryFieldSchemaImpl();
			break;
		case "node":
			field = new NodeFieldSchemaImpl();
			break;
		case "micronode":
		field = 	new MicronodeFieldSchemaImpl();
			break;
		case "date":
			field = new DateFieldSchemaImpl();
			break;
		case "boolean":
			field = new BooleanFieldSchemaImpl().setName(getFieldName());
			break;
		case "list":
			ListFieldSchema listField = new ListFieldSchemaImpl();
			listField.setName(getFieldName());
			listField.setListType(getListType());
			field = listField;
			break;
		default:
			throw error(BAD_REQUEST, "Unknown type");
		}
		field.setName(getFieldName());
		field.setLabel(getLabel());
		container.addField(field, position);
		return container;
	}

}