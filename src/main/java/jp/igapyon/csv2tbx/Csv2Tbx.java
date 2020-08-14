/*
 * Copyright 2020 Toshiki Iga
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.igapyon.csv2tbx;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Convert csv to tbx.
 * 
 * @author Toshiki Iga
 */
public class Csv2Tbx {
    /** Input CSV */
    private static final String INPUT_CSV = "src/main/resources/glossary.csv";
    // private static final String INPUT_CSV = "target/glossary.csv";

    /** Input Lang */
    private static final String INPUT_LANG = "en-US";

    /** Output TBX */
    private static final String OUTPUT_TBX = "target/glossary.tbx";

    /** Output Lang */
    private static final String OUTPUT_LANG = "ja-JP";

    private static final String ADMIN_LANG = "en-US";

    /**
     * Entry point.
     * @param args arguments.
     * @throws IOException IO Exception occurred.
     */
    public static void main(String[] args) throws IOException {
        System.err.println("csv2tbx: begin: " + INPUT_CSV);

        // Create empty document.
        final Document document = createEmptyDocument();

        final Element eleTmx = document.createElement("martif");
        eleTmx.setAttribute("version", "1.4");
        document.appendChild(eleTmx);

        // header
        {
            Element eleHeader = document.createElement("header");
            eleHeader.setAttribute("creationtool", "igapyon csv2tbx"); // Tool name.
            eleHeader.setAttribute("creationtoolversion", "1.0"); // Tool version.
            eleHeader.setAttribute("segtype", "block"); // block, paragraph, sentence, phrase
            eleHeader.setAttribute("o-tmf", "XLIFF");
            eleHeader.setAttribute("adminlang", ADMIN_LANG);
            eleHeader.setAttribute("srclang", INPUT_LANG);
            eleHeader.setAttribute("datatype", "unknown"); // default.
            eleTmx.appendChild(eleHeader);
        }

        // root element.
        Element eleBody = document.createElement("body");
        eleTmx.appendChild(eleBody);

        System.err.println("csv2tbx: read csv file.");
        try (CSVParser parseCsv = CSVFormat.DEFAULT.parse(new BufferedReader(
                new InputStreamReader(new BOMInputStream(new FileInputStream(INPUT_CSV)), "UTF-8")))) {
            for (CSVRecord record : parseCsv.getRecords()) {
                Element eleTu = document.createElement("tu");
                eleBody.appendChild(eleTu);

                Element eleTuvOrg = document.createElement("tuv");
                eleTuvOrg.setAttribute("xml:lang", INPUT_LANG);
                {
                    Element eleSeg = document.createElement("seg");
                    eleTuvOrg.appendChild(eleSeg);
                    // 1st column.
                    eleSeg.appendChild(document.createTextNode(record.get(0)));
                }
                eleTu.appendChild(eleTuvOrg);

                Element eleTuvDst = document.createElement("tuv");
                eleTuvDst.setAttribute("xml:lang", OUTPUT_LANG);
                {
                    Element eleSeg = document.createElement("seg");
                    eleTuvDst.appendChild(eleSeg);
                    // 2nd column.
                    eleSeg.appendChild(document.createTextNode(record.get(1)));
                }
                eleTu.appendChild(eleTuvDst);
            }
        }

        // Convert document to xml.
        System.err.println("csv2tbx: write xml file.");
        dom2xml(eleTmx);

        System.err.println("csv2tbx: end: " + OUTPUT_TBX);
    }

    /**
     * Create empty document.
     * 
     * @return empty document.
     * @throws IOException IO Exception occurred.
     */
    private static Document createEmptyDocument() throws IOException {
        try {
            final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return builder.newDocument();
        } catch (ParserConfigurationException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Convert document to xml.
     * 
     * @param element input root element.
     * @throws IOException IO Exception occurred.
     */
    private static void dom2xml(Element element) throws IOException {
        try {
            new File("target").mkdirs();
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            final DOMSource source = new DOMSource(element);
            final OutputStream outStream = new BufferedOutputStream(new FileOutputStream(new File(OUTPUT_TBX)));
            final StreamResult target = new StreamResult(outStream);
            transformer.transform(source, target);
        } catch (TransformerFactoryConfigurationError ex1) {
            throw new IOException(ex1);
        } catch (TransformerConfigurationException ex2) {
            throw new IOException(ex2);
        } catch (TransformerException ex3) {
            throw new IOException(ex3);
        }
    }
}
