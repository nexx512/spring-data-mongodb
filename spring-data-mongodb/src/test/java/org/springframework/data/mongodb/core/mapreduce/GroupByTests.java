/*
 * Copyright 2011-2019 the original author or authors.
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
package org.springframework.data.mongodb.core.mapreduce;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.data.Offset.offset;
import static org.springframework.data.mongodb.core.mapreduce.GroupBy.*;
import static org.springframework.data.mongodb.core.query.Criteria.*;

import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.test.util.MongoVersionRule;
import org.springframework.data.util.Version;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.mongodb.client.MongoCollection;

/**
 * Integration tests for group-by operations.
 *
 * @author Mark Pollack
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:infrastructure.xml")
public class GroupByTests {

	public static @ClassRule MongoVersionRule REQUIRES_AT_MOST_4_0 = MongoVersionRule.atMost(Version.parse("4.0.999"));

	@Autowired MongoTemplate mongoTemplate;

	@Before
	public void setUp() {
		cleanDb();
	}

	@After
	public void cleanUp() {
		cleanDb();
	}

	protected void cleanDb() {
		mongoTemplate.dropCollection(mongoTemplate.getCollectionName(XObject.class));
		mongoTemplate.dropCollection("group_test_collection");
	}

	@Test
	public void singleKeyCreation() {

		Document gc = new GroupBy("a").getGroupByObject();

		assertThat(gc).isEqualTo(Document.parse("{ \"key\" : { \"a\" : 1} , \"$reduce\" :  null  , \"initial\" :  null }"));
	}

	@Test
	public void multipleKeyCreation() {

		Document gc = GroupBy.key("a", "b").getGroupByObject();

		assertThat(gc).isEqualTo(
				Document.parse("{ \"key\" : { \"a\" : 1 , \"b\" : 1} , \"$reduce\" :  null  , \"initial\" :  null }"));
	}

	@Test
	public void keyFunctionCreation() {

		Document gc = GroupBy.keyFunction("classpath:keyFunction.js").getGroupByObject();

		assertThat(gc).isEqualTo(
				Document.parse("{ \"$keyf\" : \"classpath:keyFunction.js\" , \"$reduce\" :  null  , \"initial\" :  null }"));
	}

	@Test
	public void simpleGroupFunction() {

		createGroupByData();
		GroupByResults<XObject> results = mongoTemplate.group("group_test_collection", GroupBy.key("x")
				.initialDocument(new Document("count", 0)).reduceFunction("function(doc, prev) { prev.count += 1 }"),
				XObject.class);

		assertMapReduceResults(results);
	}

	@Test
	public void simpleGroupWithKeyFunction() {

		createGroupByData();
		GroupByResults<XObject> results = mongoTemplate
				.group(
						"group_test_collection", GroupBy.keyFunction("function(doc) { return { x : doc.x }; }")
								.initialDocument("{ count: 0 }").reduceFunction("function(doc, prev) { prev.count += 1 }"),
						XObject.class);

		assertMapReduceResults(results);
	}

	@Test
	public void simpleGroupWithFunctionsAsResources() {

		createGroupByData();
		GroupByResults<XObject> results = mongoTemplate.group("group_test_collection",
				GroupBy.keyFunction("classpath:keyFunction.js").initialDocument("{ count: 0 }")
						.reduceFunction("classpath:groupReduce.js"),
				XObject.class);

		assertMapReduceResults(results);
	}

	@Test
	public void simpleGroupWithQueryAndFunctionsAsResources() {

		createGroupByData();
		GroupByResults<XObject> results = mongoTemplate.group(where("x").gt(0), "group_test_collection",
				keyFunction("classpath:keyFunction.js").initialDocument("{ count: 0 }")
						.reduceFunction("classpath:groupReduce.js"),
				XObject.class);

		assertMapReduceResults(results);
	}

	private void assertMapReduceResults(GroupByResults<XObject> results) {

		int numResults = 0;
		for (XObject xObject : results) {
			if (xObject.getX() == 1) {
				assertThat(xObject.getCount()).isCloseTo(2, offset(0.001f));
			}
			if (xObject.getX() == 2) {
				assertThat(xObject.getCount()).isCloseTo(1, offset(0.001f));
			}
			if (xObject.getX() == 3) {
				assertThat(xObject.getCount()).isCloseTo(3, offset(0.001f));
			}
			numResults++;
		}
		assertThat(numResults).isEqualTo(3);
		assertThat(results.getKeys()).isEqualTo(3);
		assertThat(results.getCount()).isCloseTo(6, offset(0.001));
	}

	private void createGroupByData() {

		MongoCollection<Document> c = mongoTemplate.getDb().getCollection("group_test_collection", Document.class);

		c.insertOne(new Document("x", 1));
		c.insertOne(new Document("x", 1));
		c.insertOne(new Document("x", 2));
		c.insertOne(new Document("x", 3));
		c.insertOne(new Document("x", 3));
		c.insertOne(new Document("x", 3));
	}
}
