package rdfex.xml;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import rdfex.MappingResult;

public class XmlMappingResult extends MappingResult {

	Document doc;
	
	public XmlMappingResult(Path outputPath) {
		Path outputPathWithoutExtension = this.removeFileExtension(outputPath);
		this.outputPath = outputPathWithoutExtension.resolveSibling(outputPathWithoutExtension.getFileName() + ".xml");
		
		this.setupDocument();
	}
	
	private void setupDocument() {
		// Create empty XML file
		// TODO: Output filepath might be of an existing file, in this case the document has to be created from that file
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder();
			
			if (Files.exists(outputPath)) {
				this.doc = docBuilder.parse(outputPath.toFile());
			} else {
				this.doc = docBuilder.newDocument();
			}
			
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Document getDocument() {
		return doc;
	}
	
	
	@Override
	public void writeResult() {
        DOMSource source = new DOMSource(this.doc);
        FileWriter writer;
		try {
			writer = new FileWriter(this.outputPath.toFile());
			StreamResult result = new StreamResult(writer);

	        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	        Transformer transformer = transformerFactory.newTransformer();
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
	        transformer.transform(source, result);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public String getStringResult() {
		String output = "";
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(this.doc), new StreamResult(writer));
			output = writer.getBuffer().toString().replaceAll("\n|\r", "");
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return output;
		
	}

	
}
