/*
 * This file is part of the DITA Open Toolkit project.
 *
 * Copyright 2011 Jarno Elovirta
 *
 * See the accompanying license.txt file for applicable licenses.
 */
package org.dita.dost.platform;

import static org.apache.commons.io.FileUtils.*;
import static org.dita.dost.platform.PluginParser.FEATURE_ELEM;
import static org.junit.Assert.assertEquals;
import static java.util.Arrays.*;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.dita.dost.util.XMLUtils.*;
import static javax.xml.XMLConstants.NULL_NS_URI;

import org.dita.dost.TestUtils;
import org.dita.dost.log.DITAOTLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class FileGeneratorTest {

    final File resourceDir = TestUtils.getResourceDir(FileGeneratorTest.class);
    private File tempDir;

    private static File tempFile;
    private final static Hashtable<String, List<String>> features = new Hashtable<String, List<String>>();
    static {
        features.put("element", asList("foo", "bar", "baz"));
        features.put("attribute", asList("foo", "bar", "baz"));
    }
    private final static Map<String, Features> plugins = new HashMap<String, Features>();
    static {
        Document doc;
        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (final ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        final Features a = new Features(null, null);
        final Element aFeature = doc.createElement(FEATURE_ELEM);
        aFeature.setAttribute("value", "foo,bar,baz");
        aFeature.setAttribute("type", "text");
        a.addFeature("element", aFeature);
        plugins.put("a", a);
        final Features b = new Features(null, null);
        final Element bFeature = doc.createElement(FEATURE_ELEM);
        bFeature.setAttribute("value", "foo,bar,baz");
        bFeature.setAttribute("type", "text");
        b.addFeature("attribute", bFeature);
        plugins.put("b", b);
    }

    @Before
    public void setUp() throws Exception {
        tempDir = TestUtils.createTempDir(getClass());
        tempFile = new File(tempDir, "dummy_template.xml");
        copyFile(new File(resourceDir, "src" + File.separator + "dummy_template.xml"), tempFile);
        TestUtils.resetXMLUnit();
    }

    @Test
    public void testGenerate() throws IOException, SAXException {
        final FileGenerator f = new FileGenerator(features, plugins);
        f.generate(tempFile);

        assertXMLEqual(new InputSource(new File(resourceDir, "exp" + File.separator + "dummy.xml").toURI().toString()),
                new InputSource(new File(tempDir, "dummy.xml").toURI().toString()));
    }

    @After
    public void tearDown() throws IOException {
        TestUtils.forceDelete(tempDir);
    }

    private static abstract class AbstractAction implements IAction {
        protected List<String> inputs = new ArrayList<String>();
        protected Map<String, String> params = new HashMap<String, String>();
        protected Map<String, Features> features;
        public void setInput(final List<String> input) {
            inputs.addAll(input);
        }
        public void addParam(final String name, final String value) {
            params.put(name, value);
        }
        public void setFeatures(final Map<String, Features> features) {
            this.features = features;
        }
        public abstract String getResult();
        public void setLogger(final DITAOTLogger logger) {
            // NOOP
        }
    }

    public static class ElementAction extends AbstractAction {
        @Override
        public void getResult(ContentHandler output) throws SAXException {
            final Map<String, String> paramsExp = new HashMap<String, String>();
            paramsExp.put(FileGenerator.PARAM_TEMPLATE, tempFile.getAbsolutePath());
            paramsExp.put("id", "element");
            paramsExp.put("behavior", this.getClass().getName());
            assertEquals(paramsExp, params);
            final List<String> inputExp = Arrays.asList(new String[] {"foo", "bar", "baz"});
            assertEquals(inputExp, inputs);
            assertEquals(FileGeneratorTest.plugins, features);
            output.startElement(NULL_NS_URI, "foo", "foo", new AttributesBuilder().add("bar", "baz").build());
            output.characters(new char[] {'q', 'u', 'z'}, 0, 3);
            output.endElement(NULL_NS_URI, "foo", "foo");
        }
        @Override
        public String getResult() {
            throw new UnsupportedOperationException();
        }
    }

    public static class AttributeAction extends AbstractAction {
        @Override
        public String getResult() {
            final Map<String, String> paramsExp = new HashMap<String, String>();
            paramsExp.put(FileGenerator.PARAM_TEMPLATE, tempFile.getAbsolutePath());
//            paramsExp.put(FileGenerator.PARAM_LOCALNAME, "foo");
            assertEquals(paramsExp, params);
            final List<String> inputExp = Arrays.asList(new String[] {"attribute"});
            assertEquals(inputExp, inputs);
            assertEquals(FileGeneratorTest.plugins, features);
            return "bar";
        }

        @Override
        public void getResult(ContentHandler output) throws SAXException {
            throw new UnsupportedOperationException();
        }
    }

}
