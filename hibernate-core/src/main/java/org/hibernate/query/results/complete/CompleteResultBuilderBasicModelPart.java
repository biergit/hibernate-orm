/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.complete;

import java.util.function.BiFunction;

import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.SqlSelectionImpl;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.query.results.ResultsHelper.impl;
import static org.hibernate.query.results.ResultsHelper.jdbcPositionToValuesArrayPosition;

/**
 * CompleteResultBuilder for basic-valued ModelParts
 *
 * @author Steve Ebersole
 */
public class CompleteResultBuilderBasicModelPart
		implements CompleteResultBuilderBasicValued, ModelPartReferenceBasic {
	private final NavigablePath navigablePath;
	private final BasicValuedModelPart modelPart;
	private final String columnAlias;

	public CompleteResultBuilderBasicModelPart(
			NavigablePath navigablePath,
			BasicValuedModelPart modelPart,
			String columnAlias) {
		this.navigablePath = navigablePath;
		this.modelPart = modelPart;
		this.columnAlias = columnAlias;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public BasicValuedModelPart getReferencedPart() {
		return modelPart;
	}

	@Override
	public BasicResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationStateImpl = impl( domainResultCreationState );

		final TableGroup tableGroup = creationStateImpl.getFromClauseAccess().getTableGroup( navigablePath.getParent() );
		final TableReference tableReference = tableGroup.getTableReference( modelPart.getContainingTableExpression() );
		final String mappedColumn = modelPart.getMappedColumnExpression();

		final int jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( columnAlias );
		final int valuesArrayPosition = jdbcPositionToValuesArrayPosition( jdbcPosition );

		creationStateImpl.resolveSqlSelection(
				creationStateImpl.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey( tableReference, mappedColumn ),
						processingState -> new SqlSelectionImpl( valuesArrayPosition, modelPart )
				),
				modelPart.getJavaTypeDescriptor(),
				creationStateImpl.getSessionFactory().getTypeConfiguration()
		);

		//noinspection unchecked
		return new BasicResult(
				valuesArrayPosition,
				columnAlias,
				modelPart.getJavaTypeDescriptor()
		);
	}
}