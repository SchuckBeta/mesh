package com.gentics.cailun.core.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;

import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.repository.generic.GenericNodeRepository;

public interface GroupRepository extends GenericNodeRepository<Group> {

	// @Query("MATCH (u:_User {0} ) MATCH (u)-[MEMBER_OF*]->(g) return g")

	/**
	 * Return all groups that are assigned to the user
	 * 
	 * @param user
	 * @return
	 */
	@Query("start u=node({0}) MATCH (u)-[MEMBER_OF*]->(g) return g")
	public List<Group> listAllGroups(User user);

	public Group findByName(String string);

	@Query(value = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(visibleGroup:Group) where id(requestUser) = {0} and perm.`permissions-read` = true return visibleGroup ORDER BY visibleGroup.name", countQuery = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(visibleGroup:Group) where id(requestUser) = {0} and perm.`permissions-read` = true return count(visibleGroup)")
	public Page<Group> findAll(User requestUser, Pageable pageable);

}
