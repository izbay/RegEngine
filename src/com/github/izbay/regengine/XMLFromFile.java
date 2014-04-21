package com.github.izbay.regengine;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import java.io.File;

public class XMLFromFile {

	// store the files as blocks in the hashmap to be restored
	public void FileToBlock() {

		try {

			File fXmlFile = new File("insert filename here");// insert filename
																// here
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			// optional, but recommended
			// read this -
			// http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("Block");

			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);

				System.out.println("\nCurrent Element :" + nNode.getNodeName());

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					@SuppressWarnings("unused")
					Element eElement = (Element) nNode;

					// put backToBlock(
					// eElement.getElementsByTagName("Date").item(i).getTextContent(),
					// eElement.getElementsByTagName("World").item(i).getTextContent(),
					// eElement.getAttribute("BlockType").item(i).getTextContext(),
					// eElement.getAttribute("Data").item(i).getTextContext(),
					// eElement.getAttribute("Sign Text").item(i).getTextContext(),
					// eElement.getAttribute("Chest Inventory").item(i).getTextContext(),
					// eElement.getAttribute("xLoc").item(i).getTextContext()
					// eElement.getAttribute("yLoc").item(i).getTextContext()
					// eElement.getAttribute("zLoc").item(i).getTextContext());

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void BlockToFile(SerializedBlock block, int blockNum) {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// root elements
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("Block");
			doc.appendChild(rootElement);

			// staff elements
			Element BlockNum = doc.createElement("Block number");
			rootElement.appendChild(BlockNum);

			// set attribute to block number
			Attr attr = doc.createAttribute("Number");
			attr.setValue(Integer.toString(blockNum));
			BlockNum.setAttributeNode(attr);

			// shorten way
			// staff.setAttribute("id", "1");

			// Block elements
			Element BlockType = doc.createElement("BlockType");
			BlockType.appendChild(doc.createTextNode(block.getBlockType()));
			BlockNum.appendChild(BlockType);

			// Date elements
			Element Date = doc.createElement("Date");
			Date.appendChild(doc.createTextNode(block.getBlockDate()));
			BlockNum.appendChild(Date);

			// World elements
			Element World = doc.createElement("World");
			World.appendChild(doc.createTextNode(block.getBlockWorld()));
			BlockNum.appendChild(World);

			// Data elements
			Element Data = doc.createElement("Data");
			Data.appendChild(doc.createTextNode(block.getBlockData()));
			BlockNum.appendChild(Data);

			// SignText
			Element SignText = doc.createElement("SignText");
			String[] signtext = block.getBlockSignText();
			for(String s : signtext){
				SignText.appendChild(doc.createTextNode(s));
			}
			BlockNum.appendChild(SignText);

			// ChestnSuch
			Element ChestInventory = doc.createElement("Chest Inventory");
			ChestInventory.appendChild(doc.createTextNode(block
					.getBlockChestInventory()));
			BlockNum.appendChild(ChestInventory);

			// XLoc
			Element xLoc = doc.createElement("X Loc");
			xLoc.appendChild(doc.createTextNode(block.getBlockXLoc()));
			BlockNum.appendChild(xLoc);

			// YLoc
			Element yLoc = doc.createElement("Y Loc");
			yLoc.appendChild(doc.createTextNode(block.getBlockYLoc()));
			BlockNum.appendChild(yLoc);

			// YLoc
			Element zLoc = doc.createElement("Z Loc");
			zLoc.appendChild(doc.createTextNode(block.getBlockZLoc()));
			BlockNum.appendChild(zLoc);

			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File("C:\\file.xml"));

			// Output to console for testing
			// StreamResult result = new StreamResult(System.out);

			transformer.transform(source, result);

			System.out.println("File saved!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
