package com.github.izbay.regengine;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.bukkit.Location;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import java.io.File;
import java.util.Map;

public class XMLFromFile {

	// store the files as blocks in the hashmap to be restored
	public static void FileToBlock() {

		try {

			File fXmlFile = new File(RegEnginePlugin.getInstance().getDataFolder() + File.separator + "blocks.xml");// insert filename
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

					//System.out.println(eElement.getElementsByTagName("Date").item(0).getTextContent());
					// eElement.getElementsByTagName("World").item(0).getTextContent(),
					// eElement.getElementsByTagName("BlockType").item(0).getTextContext(),
					// eElement.getElementsByTagName("Data").item(0).getTextContext(),
					// eElement.getElementsByTagName("Sign Text").item(0).getTextContext(),
					// eElement.getElementsByTagName("Chest Inventory").item(0).getTextContext(),
						//if there is a chest inventroy perform this
						//NodeList iList = doc.getElementsByTagName("Inventory");
							//for (int j = 0; j < iList.getLength(); j++) 
							//{
								//Node cNode = iList.item(j)
								//Element chestElement = (Element) nNode;
								//chestElement.getAttribute("item name");
								//chestElement.getElementsByTagName("type")
								//if(isBook)
								//{
								//chestElement.getElementsByTagName("Name")
								//chestElement.getElementsByTagName("title")
								//chestElement.getElementsByTagName("Author")
								//chestElement.getElementsByTagName("pages")
								//}
								//if(enchantment)
								//{
								//chestElement.getElementsByTagName("enchantname")
								//chestElement.getElementsByTagName("enchantLevels")
								//}
								//chestElement.getElementsByTagName("amount")
								//chestElement.getElementsByTagName("durability")
								//chestElement.getElementsByTagName("lore")
							//}
					
					
					// eElement.getElementsByTagName("xLoc").item(i).getTextContext()
					// eElement.getElementsByTagName("yLoc").item(i).getTextContext()
					// eElement.getElementsByTagName("zLoc").item(i).getTextContext());

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void serializeBlocks (Map<Location, SerializedBlock> map){
		if(map.isEmpty()){return;}
		int i=0;
		for(SerializedBlock sb: map.values()){
			BlockToFile(sb, i++);
		}
	}
	
	private static void BlockToFile(SerializedBlock block, int blockNum) {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// root elements
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("Block");
			doc.appendChild(rootElement);

			// set attribute to block number
			Attr attr = doc.createAttribute("Number");
			attr.setValue(Integer.toString(blockNum));


			rootElement.setAttributeNode(attr);

			// shorten way
			// staff.setAttribute("id", "1");

			// Block elements
			Element BlockType = doc.createElement("BlockType");
			BlockType.appendChild(doc.createTextNode(block.getBlockType()));
			rootElement.appendChild(BlockType);

			// Date elements
			Element Date = doc.createElement("Date");
			Date.appendChild(doc.createTextNode(block.getBlockDate()));
			rootElement.appendChild(Date);

			// World elements
			Element World = doc.createElement("World");
			World.appendChild(doc.createTextNode(block.getBlockWorld()));
			rootElement.appendChild(World);

			// Data elements
			Element Data = doc.createElement("Data");
			Data.appendChild(doc.createTextNode(block.getBlockData()));
			rootElement.appendChild(Data);

			// SignText
			Element SignText = doc.createElement("SignText");
			String[] signtext = block.getBlockSignText();
			if(signtext != null){
				for(String s : signtext){
					SignText.appendChild(doc.createTextNode(s));
				}
			}
			rootElement.appendChild(SignText);

			// ChestnSuch
			Element ChestInventory = doc.createElement("ChestInventory");
			ChestInventory.appendChild(doc.createTextNode(block
					.getBlockChestInventory()));
			rootElement.appendChild(ChestInventory);

			// XLoc
			Element xLoc = doc.createElement("xLoc");
			xLoc.appendChild(doc.createTextNode(block.getBlockXLoc()));
			rootElement.appendChild(xLoc);

			// YLoc
			Element yLoc = doc.createElement("yLoc");
			yLoc.appendChild(doc.createTextNode(block.getBlockYLoc()));
			rootElement.appendChild(yLoc);

			// YLoc
			Element zLoc = doc.createElement("zLoc");
			zLoc.appendChild(doc.createTextNode(block.getBlockZLoc()));
			rootElement.appendChild(zLoc);

			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(RegEnginePlugin.getInstance().getDataFolder() + File.separator + "blocks.txt"));

			transformer.transform(source, result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
