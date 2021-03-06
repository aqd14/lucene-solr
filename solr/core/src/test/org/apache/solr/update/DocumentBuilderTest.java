/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.solr.update;

import org.apache.lucene.document.Document;
import org.apache.lucene.util.TestUtil;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldType;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 *
 */
public class DocumentBuilderTest extends SolrTestCaseJ4 {

  @BeforeClass
  public static void beforeClass() throws Exception {
    initCore("solrconfig.xml", "schema.xml");
  }

  @Test
  public void testBuildDocument() throws Exception 
  {
    SolrCore core = h.getCore();
    
    // undefined field
    try {
      SolrInputDocument doc = new SolrInputDocument();
      doc.setField( "unknown field", 12345 );
      DocumentBuilder.toDocument( doc, core.getLatestSchema() );
      fail( "should throw an error" );
    }
    catch( SolrException ex ) {
      assertEquals( "should be bad request", 400, ex.code() );
    }
  }

  @Test
  public void testNullField() 
  {
    SolrCore core = h.getCore();
    
    // make sure a null value is not indexed
    SolrInputDocument doc = new SolrInputDocument();
    doc.addField( "name", null );
    Document out = DocumentBuilder.toDocument( doc, core.getLatestSchema() );
    assertNull( out.get( "name" ) );
  }

  @Test
  public void testExceptions() 
  {
    SolrCore core = h.getCore();
    
    // make sure a null value is not indexed
    SolrInputDocument doc = new SolrInputDocument();
    doc.addField( "id", "123" );
    doc.addField( "unknown", "something" );
    try {
      DocumentBuilder.toDocument( doc, core.getLatestSchema() );
      fail( "added an unknown field" );
    }
    catch( Exception ex ) {
      assertTrue( "should have document ID", ex.getMessage().indexOf( "doc=123" ) > 0 );
    }
    doc.remove( "unknown" );
    

    doc.addField( "weight", "not a number" );
    try {
      DocumentBuilder.toDocument( doc, core.getLatestSchema() );
      fail( "invalid 'float' field value" );
    }
    catch( Exception ex ) {
      assertTrue( "should have document ID", ex.getMessage().indexOf( "doc=123" ) > 0 );
      assertTrue( "cause is number format", ex.getCause() instanceof NumberFormatException );
    }
    
    // now make sure it is OK
    doc.setField( "weight", "1.34" );
    DocumentBuilder.toDocument( doc, core.getLatestSchema() );
  }

  @Test
  public void testMultiField() throws Exception {
    SolrCore core = h.getCore();

    // make sure a null value is not indexed
    SolrInputDocument doc = new SolrInputDocument();
    doc.addField( "home", "2.2,3.3" );
    Document out = DocumentBuilder.toDocument( doc, core.getLatestSchema() );
    assertNotNull( out.get( "home" ) );//contains the stored value and term vector, if there is one
    assertNotNull( out.getField( "home_0" + FieldType.POLY_FIELD_SEPARATOR + "double" ) );
    assertNotNull( out.getField( "home_1" + FieldType.POLY_FIELD_SEPARATOR + "double" ) );
  }
  
  /**
   * Even though boosts have been removed, we still support them for bw compat.
   */
  public void testBoost() throws Exception {
    XmlDoc xml = new XmlDoc();
    xml.xml = "<doc>"
        + "<field name=\"id\">0</field>"
        + "<field name=\"title\" boost=\"3.0\">mytitle</field>"
        + "</doc>";
    assertNull(h.validateUpdate(add(xml, new String[0])));
  }
  
  /**
   * It's ok to supply a document boost even if a field omits norms
   */
  public void testDocumentBoostOmitNorms() throws Exception {
    XmlDoc xml = new XmlDoc();
    xml.xml = "<doc boost=\"3.0\">"
        + "<field name=\"id\">2</field>"
        + "<field name=\"title_stringNoNorms\">mytitle</field>"
        + "</doc>";
    assertNull(h.validateUpdate(add(xml, new String[0])));
  }

  public void testSolrDocumentEquals() {

    String randomString = TestUtil.randomSimpleString(random());

    SolrDocument doc1 = new SolrDocument();
    doc1.addField("foo", randomString);

    SolrDocument doc2 = new SolrDocument();
    doc2.addField("foo", randomString);

    assertTrue(compareSolrDocument(doc1, doc2));

    doc1.addField("foo", "bar");

    assertFalse(compareSolrDocument(doc1, doc2));

    doc1 = new SolrDocument();
    doc1.addField("bar", randomString);

    assertFalse(compareSolrDocument(doc1, doc2));

    int randomInt = random().nextInt();
    doc1 = new SolrDocument();
    doc1.addField("foo", randomInt);
    doc2 = new SolrDocument();
    doc2.addField("foo", randomInt);

    assertTrue(compareSolrDocument(doc1, doc2));

    doc2 = new SolrDocument();
    doc2.addField("bar", randomInt);

    assertFalse(compareSolrDocument(doc1, doc2));

  }

  public void testSolrInputDocumentEquality() {

    String randomString = TestUtil.randomSimpleString(random());

    SolrInputDocument doc1 = new SolrInputDocument();
    doc1.addField("foo", randomString);
    SolrInputDocument doc2 = new SolrInputDocument();
    doc2.addField("foo", randomString);

    assertTrue(compareSolrInputDocument(doc1, doc2));


    doc1 = new SolrInputDocument();
    doc1.addField("foo", randomString);
    doc2 = new SolrInputDocument();
    doc2.addField("foo", randomString);

    SolrInputDocument childDoc = new SolrInputDocument();
    childDoc.addField("foo", "bar");

    doc1.addChildDocument(childDoc);
    assertFalse(compareSolrInputDocument(doc1, doc2));

    doc2.addChildDocument(childDoc);
    assertTrue(compareSolrInputDocument(doc1, doc2));

    SolrInputDocument childDoc1 = new SolrInputDocument();
    childDoc.addField(TestUtil.randomSimpleString(random()), TestUtil.randomSimpleString(random()));
    doc2.addChildDocument(childDoc1);
    assertFalse(compareSolrInputDocument(doc1, doc2));

  }

  public void testSolrInputFieldEquality() {
    String randomString = TestUtil.randomSimpleString(random(), 10, 20);

    int val = random().nextInt();
    SolrInputField sif1 = new SolrInputField(randomString);
    sif1.setValue(val);
    SolrInputField sif2 = new SolrInputField(randomString);
    sif2.setValue(val);

    assertTrue(assertSolrInputFieldEquals(sif1, sif2));

    sif2.setName("foo");
    assertFalse(assertSolrInputFieldEquals(sif1, sif2));


  }

}
