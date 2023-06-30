package io;

import java.io.File;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SVGUtil {
	
	public static Element setupSVG(Document doc, int width, int height) {
		Element svg = doc.createElement("svg");
		doc.appendChild(svg);

		svg.setAttribute("viewport", "0 0 " + width + " " + height);
		svg.setAttribute("xmlns", "http://www.w3.org/2000/svg");
		svg.setAttribute("version", "1.1");
		svg.setAttribute("width", (width + 50) + "");
		svg.setAttribute("height", (height + 120) + "");
		return svg;
	}

	public static void createSVGFile(Document doc, String fileName) {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(fileName));
			transformer.transform(source, result);
		} catch (TransformerException | TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		}
	}
}
