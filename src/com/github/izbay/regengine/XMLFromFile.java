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
import org.w3c.dom.Text;

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
	
	private static Text checkAndCreate(Document doc, String arg){
		//if(arg == null){
		//	arg = "";
		//}
		return doc.createTextNode(arg);
	}
	
	public static void BlocksToFile(Map<Location, SerializedBlock> map) {
		if(map.isEmpty()){return;}
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();	
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("BlockList");
			doc.appendChild(rootElement);
			
			int i=0;
			for(SerializedBlock sb: map.values()){
				SerializedBlock block = sb;
				
				// root elements
				Element thisBlock = doc.createElement("Block");
				rootElement.appendChild(thisBlock);

				// set attribute to block number
				Attr attr = doc.createAttribute("Number");
				attr.setValue(Integer.toString(i++));

				thisBlock.setAttributeNode(attr);

				// Block elements
				Element BlockType = doc.createElement("BlockType");
				BlockType.appendChild(doc.createTextNode(block.getBlockType()));
				thisBlock.appendChild(BlockType);

				// Date elements
				Element Date = doc.createElement("Date");
				Date.appendChild(doc.createTextNode(block.getBlockDate()));
				thisBlock.appendChild(Date);

				// World elements
				Element World = doc.createElement("World");
				World.appendChild(doc.createTextNode(block.getBlockWorld()));
				thisBlock.appendChild(World);

				// Data elements
				Element Data = doc.createElement("Data");
				Data.appendChild(doc.createTextNode(block.getBlockData()));
				thisBlock.appendChild(Data);

				// SignText
				Element SignText = doc.createElement("SignText");
				String[] signtext = block.getBlockSignText();
				if(signtext != null){
					for(String s : signtext){
						SignText.appendChild(doc.createTextNode(s));
					}
				}
				thisBlock.appendChild(SignText);

				// ChestnSuch
				Element ChestInventory = doc.createElement("ChestInventory");
				if(block.getBlockChestInventory() != null){
					int j=0;
					for(SerializedItem item: block.getBlockChestInventory().getSerializedInventory()){
						if(item == null){
							j++;
							continue;
						}
						Element thisItem = doc.createElement("Item");
						
						// set attribute to item number
						Attr itemattr = doc.createAttribute("Number");
						itemattr.setValue(Integer.toString(j++));
						thisItem.setAttributeNode(itemattr);
						
						Element Name = doc.createElement("Name");
						Name.appendChild(checkAndCreate(doc, item.getName()));
						thisItem.appendChild(Name);
						
						Element Amount = doc.createElement("Amount");
						Amount.appendChild(checkAndCreate(doc, item.getAmount()));
						thisItem.appendChild(Amount);
						
						Element Durability = doc.createElement("Durability");
						Durability.appendChild(checkAndCreate(doc, item.getDurability()));
						thisItem.appendChild(Durability);
						
						/*Insert all the item fields here.
						public String[] getLore(){
							return lore;
						}
						public boolean isBook(){
							return isBook;
						}
						public String getName(){
							return name;
						}
						public String getAuthor(){
							return author;
						}
						public String getTitle(){
							return title;
						}
						public String[] getPages(){
							return pages;
						}
						public String[] getEnchants(){
							return enchant;
						}
						public Integer[] getEnchantLevels(){
							return enchantLevels;
						}*/
					}
				}
				thisBlock.appendChild(ChestInventory);
				
				// XLoc
				Element xLoc = doc.createElement("xLoc");
				xLoc.appendChild(doc.createTextNode(block.getBlockXLoc()));
				thisBlock.appendChild(xLoc);

				// YLoc
				Element yLoc = doc.createElement("yLoc");
				yLoc.appendChild(doc.createTextNode(block.getBlockYLoc()));
				thisBlock.appendChild(yLoc);

				// YLoc
				Element zLoc = doc.createElement("zLoc");
				zLoc.appendChild(doc.createTextNode(block.getBlockZLoc()));
				thisBlock.appendChild(zLoc);
			}
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
