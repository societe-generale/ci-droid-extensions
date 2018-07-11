package com.societegenerale.cidroid.extensions.actionToReplicate;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.IOException;
import java.io.StringWriter;

public class XMLUtils {

    public static String prettyPrint(Document originalDocument) throws IOException {

        StringWriter sw = new StringWriter();

        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter(sw, format);
        writer.write(originalDocument);

        return sw.toString();
    }

}
