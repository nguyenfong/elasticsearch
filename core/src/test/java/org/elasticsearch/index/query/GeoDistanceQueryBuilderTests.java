/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.query;

import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.elasticsearch.common.ParsingException;
import org.elasticsearch.common.geo.GeoDistance;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.search.internal.SearchContext;
import org.elasticsearch.test.AbstractQueryTestCase;
import org.elasticsearch.test.geo.RandomShapeGenerator;
import org.locationtech.spatial4j.shape.Point;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;

public class GeoDistanceQueryBuilderTests extends AbstractQueryTestCase<GeoDistanceQueryBuilder> {

    @Override
    protected GeoDistanceQueryBuilder doCreateTestQueryBuilder() {
        GeoDistanceQueryBuilder qb = new GeoDistanceQueryBuilder(GEO_POINT_FIELD_NAME);
        String distance = "" + randomDouble();
        if (randomBoolean()) {
            DistanceUnit unit = randomFrom(DistanceUnit.values());
            distance = distance + unit.toString();
        }
        int selector = randomIntBetween(0, 2);
        switch (selector) {
            case 0:
                qb.distance(randomDouble(), randomFrom(DistanceUnit.values()));
                break;
            case 1:
                qb.distance(distance, randomFrom(DistanceUnit.values()));
                break;
            case 2:
                qb.distance(distance);
                break;
        }

        Point p = RandomShapeGenerator.xRandomPoint(random());
        qb.point(new GeoPoint(p.getY(), p.getX()));

        if (randomBoolean()) {
            qb.setValidationMethod(randomFrom(GeoValidationMethod.values()));
        }

        if (randomBoolean()) {
            qb.geoDistance(randomFrom(GeoDistance.values()));
        }

        if (randomBoolean()) {
            qb.ignoreUnmapped(randomBoolean());
        }
        return qb;
    }

    public void testIllegalValues() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> new GeoDistanceQueryBuilder(""));
        assertEquals("fieldName must not be null or empty", e.getMessage());

        e = expectThrows(IllegalArgumentException.class, () -> new GeoDistanceQueryBuilder((String) null));
        assertEquals("fieldName must not be null or empty", e.getMessage());

        GeoDistanceQueryBuilder query = new GeoDistanceQueryBuilder("fieldName");
        e = expectThrows(IllegalArgumentException.class, () -> query.distance(""));
        assertEquals("distance must not be null or empty", e.getMessage());
        e = expectThrows(IllegalArgumentException.class, () -> query.distance(null));
        assertEquals("distance must not be null or empty", e.getMessage());
        e = expectThrows(IllegalArgumentException.class, () -> query.distance("", DistanceUnit.DEFAULT));
        assertEquals("distance must not be null or empty", e.getMessage());
        e = expectThrows(IllegalArgumentException.class, () -> query.distance(null, DistanceUnit.DEFAULT));
        assertEquals("distance must not be null or empty", e.getMessage());

        e = expectThrows(IllegalArgumentException.class, () -> query.distance("1", null));
        assertEquals("distance unit must not be null", e.getMessage());
        e = expectThrows(IllegalArgumentException.class, () -> query.distance(1, null));
        assertEquals("distance unit must not be null", e.getMessage());

        e = expectThrows(IllegalArgumentException.class, () -> query.distance(
                randomIntBetween(Integer.MIN_VALUE, 0), DistanceUnit.DEFAULT));
        assertEquals("distance must be greater than zero", e.getMessage());

        e = expectThrows(IllegalArgumentException.class, () -> query.geohash(null));
        assertEquals("geohash must not be null or empty", e.getMessage());
        e = expectThrows(IllegalArgumentException.class, () -> query.geohash(""));
        assertEquals("geohash must not be null or empty", e.getMessage());

        e = expectThrows(IllegalArgumentException.class, () -> query.geoDistance(null));
        assertEquals("geoDistance must not be null", e.getMessage());
    }

    /**
     * Overridden here to ensure the test is only run if at least one type is
     * present in the mappings. Geo queries do not execute if the field is not
     * explicitly mapped
     */
    @Override
    public void testToQuery() throws IOException {
        assumeTrue("test runs only when at least a type is registered", getCurrentTypes().length > 0);
        super.testToQuery();
    }

    @Override
    protected void doAssertLuceneQuery(GeoDistanceQueryBuilder queryBuilder, Query query, SearchContext context) throws IOException {
        // TODO: what can we check
    }

    public void testParsingAndToQuery1() throws IOException {
        assumeTrue("test runs only when at least a type is registered", getCurrentTypes().length > 0);
        String query = "{\n" +
                "    \"geo_distance\":{\n" +
                "        \"distance\":\"12mi\",\n" +
                "        \"" + GEO_POINT_FIELD_NAME + "\":{\n" +
                "            \"lat\":40,\n" +
                "            \"lon\":-70\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
        assertGeoDistanceRangeQuery(query, 40, -70, 12, DistanceUnit.DEFAULT);
    }

    public void testParsingAndToQuery2() throws IOException {
        assumeTrue("test runs only when at least a type is registered", getCurrentTypes().length > 0);
        String query = "{\n" +
                "    \"geo_distance\":{\n" +
                "        \"distance\":\"12mi\",\n" +
                "        \"" + GEO_POINT_FIELD_NAME + "\":[-70, 40]\n" +
                "    }\n" +
                "}\n";
        assertGeoDistanceRangeQuery(query, 40, -70, 12, DistanceUnit.DEFAULT);
    }

    public void testParsingAndToQuery3() throws IOException {
        assumeTrue("test runs only when at least a type is registered", getCurrentTypes().length > 0);
        String query = "{\n" +
                "    \"geo_distance\":{\n" +
                "        \"distance\":\"12mi\",\n" +
                "        \"" + GEO_POINT_FIELD_NAME + "\":\"40, -70\"\n" +
                "    }\n" +
                "}\n";
        assertGeoDistanceRangeQuery(query, 40, -70, 12, DistanceUnit.DEFAULT);
    }

    public void testParsingAndToQuery4() throws IOException {
        assumeTrue("test runs only when at least a type is registered", getCurrentTypes().length > 0);
        String query = "{\n" +
                "    \"geo_distance\":{\n" +
                "        \"distance\":\"12mi\",\n" +
                "        \"" + GEO_POINT_FIELD_NAME + "\":\"drn5x1g8cu2y\"\n" +
                "    }\n" +
                "}\n";
        assertGeoDistanceRangeQuery(query, 40, -70, 12, DistanceUnit.DEFAULT);
    }

    public void testParsingAndToQuery5() throws IOException {
        assumeTrue("test runs only when at least a type is registered", getCurrentTypes().length > 0);
        String query = "{\n" +
                "    \"geo_distance\":{\n" +
                "        \"distance\":12,\n" +
                "        \"unit\":\"mi\",\n" +
                "        \"" + GEO_POINT_FIELD_NAME + "\":{\n" +
                "            \"lat\":40,\n" +
                "            \"lon\":-70\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
        assertGeoDistanceRangeQuery(query, 40, -70, 12, DistanceUnit.DEFAULT);
    }

    public void testParsingAndToQuery6() throws IOException {
        assumeTrue("test runs only when at least a type is registered", getCurrentTypes().length > 0);
        String query = "{\n" +
                "    \"geo_distance\":{\n" +
                "        \"distance\":\"12\",\n" +
                "        \"unit\":\"mi\",\n" +
                "        \"" + GEO_POINT_FIELD_NAME + "\":{\n" +
                "            \"lat\":40,\n" +
                "            \"lon\":-70\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
        assertGeoDistanceRangeQuery(query, 40, -70, 12, DistanceUnit.DEFAULT);
    }

    public void testParsingAndToQuery7() throws IOException {
        assumeTrue("test runs only when at least a type is registered", getCurrentTypes().length > 0);
        String query = "{\n" +
                "  \"geo_distance\":{\n" +
                "      \"distance\":\"19.312128\",\n" +
                "      \"" + GEO_POINT_FIELD_NAME + "\":{\n" +
                "          \"lat\":40,\n" +
                "          \"lon\":-70\n" +
                "      }\n" +
                "  }\n" +
                "}\n";
        assertGeoDistanceRangeQuery(query, 40, -70, 0.012, DistanceUnit.DEFAULT);
    }

    public void testParsingAndToQuery8() throws IOException {
        assumeTrue("test runs only when at least a type is registered", getCurrentTypes().length > 0);
        String query = "{\n" +
                "    \"geo_distance\":{\n" +
                "        \"distance\":19.312128,\n" +
                "        \"" + GEO_POINT_FIELD_NAME + "\":{\n" +
                "            \"lat\":40,\n" +
                "            \"lon\":-70\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
        assertGeoDistanceRangeQuery(query, 40, -70, 12, DistanceUnit.KILOMETERS);
    }

    public void testParsingAndToQuery9() throws IOException {
        assumeTrue("test runs only when at least a type is registered", getCurrentTypes().length > 0);
        String query = "{\n" +
                "    \"geo_distance\":{\n" +
                "        \"distance\":\"19.312128\",\n" +
                "        \"unit\":\"km\",\n" +
                "        \"" + GEO_POINT_FIELD_NAME + "\":{\n" +
                "            \"lat\":40,\n" +
                "            \"lon\":-70\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
        assertGeoDistanceRangeQuery(query, 40, -70, 12, DistanceUnit.DEFAULT);
    }

    public void testParsingAndToQuery10() throws IOException {
        assumeTrue("test runs only when at least a type is registered", getCurrentTypes().length > 0);
        String query = "{\n" +
                "    \"geo_distance\":{\n" +
                "        \"distance\":19.312128,\n" +
                "        \"unit\":\"km\",\n" +
                "        \"" + GEO_POINT_FIELD_NAME + "\":{\n" +
                "            \"lat\":40,\n" +
                "            \"lon\":-70\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
        assertGeoDistanceRangeQuery(query, 40, -70, 12, DistanceUnit.DEFAULT);
    }

    public void testParsingAndToQuery11() throws IOException {
        assumeTrue("test runs only when at least a type is registered", getCurrentTypes().length > 0);
        String query = "{\n" +
                "    \"geo_distance\":{\n" +
                "        \"distance\":\"19.312128km\",\n" +
                "        \"" + GEO_POINT_FIELD_NAME + "\":{\n" +
                "            \"lat\":40,\n" +
                "            \"lon\":-70\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
        assertGeoDistanceRangeQuery(query, 40, -70, 12, DistanceUnit.DEFAULT);
    }

    public void testParsingAndToQuery12() throws IOException {
        assumeTrue("test runs only when at least a type is registered", getCurrentTypes().length > 0);
        String query = "{\n" +
                "    \"geo_distance\":{\n" +
                "        \"distance\":\"12mi\",\n" +
                "        \"unit\":\"km\",\n" +
                "        \"" + GEO_POINT_FIELD_NAME + "\":{\n" +
                "            \"lat\":40,\n" +
                "            \"lon\":-70\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
        assertGeoDistanceRangeQuery(query, 40, -70, 12, DistanceUnit.DEFAULT);
    }

    private void assertGeoDistanceRangeQuery(String query, double lat, double lon, double distance, DistanceUnit distanceUnit) throws IOException {
        assumeTrue("test runs only when at least a type is registered", getCurrentTypes().length > 0);
        Query parsedQuery = parseQuery(query).toQuery(createShardContext());
        // TODO: what can we check?
    }

    public void testFromJson() throws IOException {
        String json =
                "{\n" +
                "  \"geo_distance\" : {\n" +
                "    \"pin.location\" : [ -70.0, 40.0 ],\n" +
                "    \"distance\" : 12000.0,\n" +
                "    \"distance_type\" : \"sloppy_arc\",\n" +
                "    \"validation_method\" : \"STRICT\",\n" +
                "    \"ignore_unmapped\" : false,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}";
        GeoDistanceQueryBuilder parsed = (GeoDistanceQueryBuilder) parseQuery(json);
        checkGeneratedJson(json, parsed);
        assertEquals(json, -70.0, parsed.point().getLon(), 0.0001);
        assertEquals(json, 40.0, parsed.point().getLat(), 0.0001);
        assertEquals(json, 12000.0, parsed.distance(), 0.0001);
    }

    public void testOptimizeBboxIsDeprecated() throws IOException {
        String json =
            "{\n" +
                "  \"geo_distance\" : {\n" +
                "    \"pin.location\" : [ -70.0, 40.0 ],\n" +
                "    \"distance\" : 12000.0,\n" +
                "    \"distance_type\" : \"sloppy_arc\",\n" +
                "    \"optimize_bbox\" : \"memory\",\n" +
                "    \"validation_method\" : \"STRICT\",\n" +
                "    \"ignore_unmapped\" : false,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}";
        parseQuery(json);
        assertWarnings("Deprecated field [optimize_bbox] used, replaced by [no replacement: " +
                "`optimize_bbox` is no longer supported due to recent improvements]");
    }

    public void testFromCoerceIsDeprecated() throws IOException {
        String json =
                "{\n" +
                "  \"geo_distance\" : {\n" +
                "    \"pin.location\" : [ -70.0, 40.0 ],\n" +
                "    \"distance\" : 12000.0,\n" +
                "    \"distance_type\" : \"sloppy_arc\",\n" +
                "    \"coerce\" : true,\n" +
                "    \"ignore_unmapped\" : false,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}";
        parseQuery(json);
        assertWarnings("Deprecated field [coerce] used, replaced by [validation_method]");
    }

    public void testFromJsonIgnoreMalformedIsDeprecated() throws IOException {
        String json =
                "{\n" +
                "  \"geo_distance\" : {\n" +
                "    \"pin.location\" : [ -70.0, 40.0 ],\n" +
                "    \"distance\" : 12000.0,\n" +
                "    \"distance_type\" : \"sloppy_arc\",\n" +
                "    \"ignore_malformed\" : true,\n" +
                "    \"ignore_unmapped\" : false,\n" +
                "    \"boost\" : 1.0\n" +
                "  }\n" +
                "}";
        parseQuery(json);
        assertWarnings("Deprecated field [ignore_malformed] used, replaced by [validation_method]");
    }

    @Override
    public void testMustRewrite() throws IOException {
        assumeTrue("test runs only when at least a type is registered", getCurrentTypes().length > 0);
        super.testMustRewrite();
    }

    public void testIgnoreUnmapped() throws IOException {
        final GeoDistanceQueryBuilder queryBuilder = new GeoDistanceQueryBuilder("unmapped").point(0.0, 0.0).distance("20m");
        queryBuilder.ignoreUnmapped(true);
        QueryShardContext shardContext = createShardContext();
        Query query = queryBuilder.toQuery(shardContext);
        assertThat(query, notNullValue());
        assertThat(query, instanceOf(MatchNoDocsQuery.class));

        final GeoDistanceQueryBuilder failingQueryBuilder = new GeoDistanceQueryBuilder("unmapped").point(0.0, 0.0).distance("20m");
        failingQueryBuilder.ignoreUnmapped(false);
        QueryShardException e = expectThrows(QueryShardException.class, () -> failingQueryBuilder.toQuery(shardContext));
        assertThat(e.getMessage(), containsString("failed to find geo_point field [unmapped]"));
    }

    public void testParseFailsWithMultipleFields() throws IOException {
        String json = "{\n" +
                "  \"geo_distance\" : {\n" +
                "    \"point1\" : {\n" +
                "      \"lat\" : 30, \"lon\" : 12\n" +
                "    },\n" +
                "    \"point2\" : {\n" +
                "      \"lat\" : 30, \"lon\" : 12\n" +
                "    }\n" +
                "  }\n" +
                "}";
        ParsingException e = expectThrows(ParsingException.class, () -> parseQuery(json));
        assertEquals("[geo_distance] query doesn't support multiple fields, found [point1] and [point2]", e.getMessage());
    }
}
