/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mongodb.core.aggregation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.mongodb.core.aggregation.Fields.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.data.mongodb.core.aggregation.Fields.*;

/**
 * Unit tests for {@link Fields}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class FieldsUnitTests {

	@Rule public ExpectedException exception = ExpectedException.none();

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullFieldVarArgs() {
		Fields.from((Field[]) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullFieldNameVarArgs() {
		Fields.fields((String[]) null);
	}

	@Test
	public void createsFieldFromNameOnly() {
		verify(Fields.field("foo"), "foo", null);
	}

	@Test
	public void createsFieldFromNameAndTarget() {
		verify(Fields.field("foo", "bar"), "foo", "bar");
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullFieldName() {
		Fields.field(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullFieldNameIfTargetGiven() {
		Fields.field(null, "foo");
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsEmptyFieldName() {
		Fields.field("");
	}

	@Test
	public void createsFieldsFromFieldInstances() {

		AggregationField reference = new AggregationField("foo");
		Fields fields = Fields.from(reference);

		assertThat(fields).hasSize(1);
		assertThat(fields).contains(reference);
	}

	@Test
	public void aliasesPathExpressionsIntoLeafForImplicits() {
		verify(Fields.field("foo.bar"), "bar", "foo.bar");
	}

	@Test
	public void fieldsFactoryMethod() {

		Fields fields = fields("a", "b").and("c").and("d", "e");

		assertThat(fields).hasSize(4);

		verify(fields.getField("a"), "a", null);
		verify(fields.getField("b"), "b", null);
		verify(fields.getField("c"), "c", null);
		verify(fields.getField("d"), "d", "e");
	}

	@Test
	public void rejectsAmbiguousFieldNames() {

		exception.expect(IllegalArgumentException.class);

		fields("b", "a.b");
	}

	@Test // DATAMONGO-774
	public void stripsLeadingDollarsFromName() {

		assertThat(Fields.field("$name").getName()).isEqualTo("name");
		assertThat(Fields.field("$$$$name").getName()).isEqualTo("name");
	}

	@Test(expected = IllegalArgumentException.class) // DATAMONGO-774
	public void rejectsNameConsistingOfDollarOnly() {
		Fields.field("$");
	}

	@Test // DATAMONGO-774
	public void stripsLeadingDollarsFromTarget() {

		assertThat(Fields.field("$target").getTarget()).isEqualTo("target");
		assertThat(Fields.field("$$$$target").getTarget()).isEqualTo("target");
	}

	private static void verify(Field field, String name, String target) {

		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo(name);
		assertThat(field.getTarget()).isEqualTo(target != null ? target : name);
	}
}
