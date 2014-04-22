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
import org.w3c.dom.Text;

import java.io.File;
import java.util.LinkedList;

public class XMLFromFile {

	// store the files as blocks in the hashmap to be restored
//	public static void FileToBlock() {
//		
//		private final String x, y, z;
//		private final String date;
//		private final String blockType, world;
//		private final String data;
//		private final String[] signtext;
//		private final String inventory;
//		
//		private final String itemName;
//		private final String materialType;
//		private final String amount;
//		private final String durability;
//		private final String[] lore;
//
//		//Book Info
//		private final bool isBook = false;
//		private final String name, author, title;
//		private final String[] pages;
//
//		//Enchantment Info
//		private final bool hasEnchant = false;
//		private final String[] enchant; //so tempted to name this array "hoes"
//		private String[] enchantLevels;
//
//		try {
//
//			//file path
//			File fXmlFile = new File(RegEnginePlugin.getInstance().getDataFolder() + File.separator + "blocks.xml");// insert filename
//			
//			//doucment builder factory and builder initalization
//			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
//			Document doc = dBuilder.parse(fXmlFile);
//
//			//normalize teh document
//			doc.getDocumentElement().normalize();
//
//			//create a list of the blocks that need to be looped through
//			NodeList nList = doc.getElementsByTagName("Block");
//
//			//loop through the list
//			for (int temp = 0; temp < nList.getLength(); temp++) {
//
//				Node nNode = nList.item(temp);
//
//				System.out.println("\nCurrent Element :" + nNode.getNodeName());
//
//				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
//
//					@SuppressWarnings("unused")
//					Element eElement = (Element) nNode;
//
//					date =eElement.getElementsByTagName("Date").item(0).getTextContent();
//					world = eElement.getElementsByTagName("World").item(0).getTextContent();
//					blockType = eElement.getElementsByTagName("BlockType").item(0).getTextContent();
//					data = eElement.getElementsByTagName("Data").item(0).getTextContent();
//
//					
//					NodeList signList = eElement.getElementsByTagName("SignText").item(0).getTextContent();
//					for (int k = 0; k < signList.getLength(); k++) 
//					{
//						Node signNode = signList.item(k)
//						Element signElement = (Element) signNode;
//						signtext[k] = signElement;
//					}
//					inventory = eElement.getElementsByTagName("Chest Inventory").item(0).getTextContent();
//						
//					//check for inventroy stuffs
//					if(inventory != null)
//						{
//						NodeList iList = doc.getElementsByTagName("Inventory").item(0).getTextContent();
//							for (int j = 0; j < iList.getLength(); j++) 
//							{
//								Node cNode = iList.item(j)
//									Element chestElement = (Element) nNode;
//								itemName = chestElement.getAttribute("ItemName");
//								type = chestElement.getElementsByTagName("type").item(0).getTextContent();
//								isBook = chestElement.getAttribute("isBook");
//								if(isBook)
//								{
//									title = chestElement.getElementsByTagName("Title").item(0).getTextContent();
//									Author = chestElement.getElementsByTagName("Author").item(0).getTextContent();
//									
//									NodeList pagesList = eElement.getElementsByTagName("Pages").item(0).getTextContent();
//									for (int k = 0; k < pagesList.getLength(); k++) 
//									{
//										Node pageNode = pagesList.item(k)
//										Element pageElement = (Element) pageNode;
//										pages[k] = pageElement;
//									}
//								}
//								enchant = chestElement.getElementsByTagName("enchantName").item(0).getTextContent();
//								if(enchant != null)
//								{
//									enchantLevels = chestElement.getElementsByTagName("enchantLevels").item(0).getTextContent();
//								}
//								amount = chestElement.getElementsByTagName("Amount").item(0).getTextContent();
//								durability = chestElement.getElementsByTagName("Durability").item(0).getTextContent();
//								
//								NodeList lList = chestElement.getElementsByTagName("Lore").item(0).getTextContent();
//								for (int k = 0; k < lList.getLength(); k++) 
//								{
//									Node lNode = lList.item(k)
//									Element loreElement = (Element) lNode;
//									Lore[k] = loreElement;
//								}
//								
//								NodeList enchantList = eElement.getElementsByTagName("enchant").item(0).getTextContent();
//								for (int k = 0; k < enchantList.getLength(); k++) 
//								{
//									Node enchantNode = enchantList.item(k)
//									Element enchantElement = (Element) enchantNode;
//									enchant[k] = enchantElement;
//								}
//								
//								NodeList enchantLevelsList = eElement.getElementsByTagName("enchantLevels").item(0).getTextContent();
//								for (int k = 0; k < enchantLevelsList.getLength(); k++) 
//								{
//									Node enchantNode = enchantLevelsList.item(k)
//									Element enchantListElement = (Element) enchantNode;
//									enchantLevels[k] = enchantListElement;
//								}
//							}
//						}
//					
//					 x = eElement.getElementsByTagName("xLoc").item(0).getTextContent();
//					 y = eElement.getElementsByTagName("yLoc").item(0).getTextContent();
//					 z = eElement.getElementsByTagName("zLoc").item(0).getTextContent();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
	
	public static void BlocksToFile(LinkedList<SerializedBlock> writeOut) {
		if(writeOut.isEmpty()){return;}
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();	
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("BlockList");
			doc.appendChild(rootElement);
			
			int i=0;
			for(SerializedBlock sb: writeOut){
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
						
						Element Amount = doc.createElement("Amount");
						Amount.appendChild(doc.createTextNode(item.getAmount()));
						thisItem.appendChild(Amount);
						
						Element Lore = doc.createElement("Lore");
						String[] lore = item.getLore();
						if(lore != null){
							for(String s : lore){
								Lore.appendChild(doc.createTextNode(s));
							}
							thisItem.appendChild(Lore);
						}
						
						Element isBook = doc.createElement("isBook");
						isBook.appendChild(doc.createTextNode(item.isBook()));
						thisItem.appendChild(isBook);
						
						Element Title = doc.createElement("Title");
						Title.appendChild(doc.createTextNode(item.getTitle()));
						thisItem.appendChild(Title);
						
						Element Pages = doc.createElement("Pages");
						String[] pages = item.getPages();
						if(pages != null){
							for(String s : pages){
								Pages.appendChild(doc.createTextNode(s));
							}
							thisItem.appendChild(Pages);
						}
						
						Element Enchants = doc.createElement("Enchants");
						String[] enchants = item.getEnchants();
						if(enchants != null){
							for(String s : enchants){
								Enchants.appendChild(doc.createTextNode(s));
							}
							thisItem.appendChild(Enchants);
						}
						
						
						Element EnchantLevels = doc.createElement("EnchantLevels");
						String[] enchantLevels = item.getEnchantLevels();
						if(enchantLevels != null){
							for(String s : enchantLevels){
								EnchantLevels.appendChild(doc.createTextNode(s));
							}
							thisItem.appendChild(EnchantLevels);
						}
						if(thisItem!=null)
						ChestInventory.appendChild(thisItem);
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