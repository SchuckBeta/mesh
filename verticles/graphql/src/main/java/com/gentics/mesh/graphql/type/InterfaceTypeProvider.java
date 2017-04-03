package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInterfaceType.newInterface;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.CreatorTrackingVertex;
import com.gentics.mesh.core.data.EditorTrackingVertex;
import com.gentics.mesh.core.data.TransformableElement;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphql.context.GraphQLContext;

import dagger.Lazy;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLInterfaceType.Builder;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class InterfaceTypeProvider extends AbstractTypeProvider {

	@Inject
	public Lazy<UserTypeProvider> userTypeProvider;

	@Inject
	public InterfaceTypeProvider() {
	}

	protected GraphQLInterfaceType createCommonType() {

		Builder common = newInterface().name("MeshElement");
		// .uuid
		common.field(newFieldDefinition().name("uuid")
				.description("UUID of the element")
				.type(GraphQLString));

		// .edited
		common.field(newFieldDefinition().name("edited")
				.description("ISO8601 formatted edit timestamp")
				.type(GraphQLString));

		// .created
		common.field(newFieldDefinition().name("created")
				.description("ISO8601 formatted created date string")
				.type(GraphQLString));

		// .permissions
		common.field(newFieldDefinition().name("permissions")
				.description("Permission information of the element")
				.type(createPermInfoType()));

		//TODO add rolePerms

		// .creator
		common.field(newFieldDefinition().name("creator")
				.description("Creator of the element")
				.type(new GraphQLTypeReference("User")));

		// .editor
		common.field(newFieldDefinition().name("editor")
				.description("Editor of the element")
				.type(new GraphQLTypeReference("User")));

		common.typeResolver(resolver -> {
			return null;
		});
		return common.build();
	}

	/**
	 * Create the permission information type.
	 * 
	 * @return
	 */
	private GraphQLObjectType createPermInfoType() {
		graphql.schema.GraphQLObjectType.Builder builder = newObject().name("permissions")
				.description("Permission information");

		// .create
		builder.field(newFieldDefinition().name("create")
				.type(GraphQLBoolean)
				.description("Flag which idicates whether the create permission is granted."));

		// .read
		builder.field(newFieldDefinition().name("read")
				.type(GraphQLBoolean)
				.description("Flag which idicates whether the read permission is granted."));

		// .update
		builder.field(newFieldDefinition().name("update")
				.type(GraphQLBoolean)
				.description("Flag which idicates whether the update permission is granted."));

		// .delete
		builder.field(newFieldDefinition().name("delete")
				.type(GraphQLBoolean)
				.description("Flag which idicates whether the delete permission is granted."));

		// .publish
		builder.field(newFieldDefinition().name("publish")
				.type(GraphQLBoolean)
				.description("Flag which idicates whether the publish permission is granted."));

		// .readPublished
		builder.field(newFieldDefinition().name("readPublished")
				.type(GraphQLBoolean)
				.description("Flag which idicates whether the read published permission is granted."));
		return builder.build();
	}
	
	
	public void addCommonFields(graphql.schema.GraphQLObjectType.Builder builder) {
		addCommonFields(builder, false);
	}

	/**
	 * Add common fields to the given builder.
	 * 
	 * @param builder
	 * @param isNode
	 *            Flag which indicates whether the builder is for a node. Nodes do not require certain common fields. Those fields will be excluded.
	 */
	public void addCommonFields(graphql.schema.GraphQLObjectType.Builder builder, boolean isNode) {
		//builder.withInterface(createCommonType());

		// .uuid
		builder.field(newFieldDefinition().name("uuid")
				.description("UUID of the element")
				.type(GraphQLString)
				.dataFetcher(env -> {
					MeshElement element = env.getSource();
					return element.getUuid();
				}));

		// .etag
		builder.field(newFieldDefinition().name("etag")
				.description("ETag of the element")
				.type(GraphQLString)
				.dataFetcher(env -> {
					GraphQLContext gc = env.getContext();
					TransformableElement<?> element = env.getSource();
					return element.getETag(gc);
				}));

		// .permission
		builder.field(newFieldDefinition().name("permissions")
				.description("Permission information of the element")
				.type(createPermInfoType()));

		//TODO rolePerms

		// .created
		builder.field(newFieldDefinition().name("created")
				.description("ISO8601 formatted created date string")
				.type(GraphQLString)
				.dataFetcher(env -> {
					CreatorTrackingVertex vertex = env.getSource();
					return vertex.getCreationDate();
				}));

		// .creator
		builder.field(newFieldDefinition().name("creator")
				.description("Creator of the element")
				.type(new GraphQLTypeReference("User"))
				.dataFetcher(env -> {
					GraphQLContext gc = env.getContext();
					CreatorTrackingVertex vertex = env.getSource();
					return gc.requiresPerm(vertex.getCreator(), READ_PERM);
				}));

		if (!isNode) {
			// .edited
			builder.field(newFieldDefinition().name("edited")
					.description("ISO8601 formatted edit timestamp")
					.type(GraphQLString)
					.dataFetcher(env -> {
						EditorTrackingVertex vertex = env.getSource();
						return vertex.getLastEditedDate();
					}));

			// .editor
			builder.field(newFieldDefinition().name("editor")
					.description("Editor of the element")
					.type(new GraphQLTypeReference("User"))
					.dataFetcher(env -> {
						GraphQLContext gc = env.getContext();
						EditorTrackingVertex vertex = env.getSource();
						return gc.requiresPerm(vertex.getEditor(), READ_PERM);
					}));
		}

	}

}
