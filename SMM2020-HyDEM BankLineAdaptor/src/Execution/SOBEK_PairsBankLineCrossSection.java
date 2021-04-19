package Execution;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gdal.ogr.Geometry;

import geo.gdal.SpatialReader;
import geo.gdal.SpatialWriter;
import geo.gdal.application.IrregularReachBasicControl;
import testFolder.SOBEK_OBJECT.SobekBankLine;
import testFolder.SOBEK_OBJECT.SobekBankPoint;
import usualTool.AtCommonMath;

public class SOBEK_PairsBankLineCrossSection {

	public static void main(String[] args) throws UnsupportedEncodingException {

		// @ input
		// -SOBEK_SpatialFolder (String, folder path)
		// -SOBEK_BankLine (String , spatialFile path)

		// @Output
		// -SOBEK_CrossSectionPoints (String, spatialFile path)
		// -SOBEK_CrossSectionPointsError (String, spatialFile path)
		// -SOBEK_ReachNode (String, spatialFile path)

		Map<String, String> parameter = WorkSpace.settingVariables(args);

		// setting Variables
		String sobekObjectWorkSpace = parameter.get("-SOBEK_SpatialFolder");
		String pairseBankLine = parameter.get("-SOBEK_BankLine");
		String pariseBankPointsError = parameter.get("-SOBEK_CrossSectionPointsError");
		String pariseBankPoints = parameter.get("-SOBEK_CrossSectionPoints");
		String reachNodesShp = parameter.get("-SOBEK_ReachNode");

		// <================================================>
		// <======== pairs bankPoints ===========================>
		// <================================================>

		/*
		 * @ check bankPoints in SobekObject to find out is there any bankPoints not
		 * paired
		 * 
		 * 
		 * @input SobekObject: Sbk_C&LR_n.shp、Sbk_LConn_n.shp
		 * 
		 * @input newObject: pairseBankLine.shp
		 * 
		 * @output :
		 * 
		 * @ bankPoints => pariseBankPointsError or pariseBankPoints
		 * 
		 * @ reachPoint => reachNodesShp
		 * 
		 * 
		 */

		// TODO Auto-generated method stub
		// get bank line
		String pariseBaneLine = pairseBankLine;
		SpatialReader bankShp = new SpatialReader(pariseBaneLine);
		List<Geometry> bankGeoList = bankShp.getGeometryList();
		List<Map<String, Object>> bankAttrList = bankShp.getAttributeTable();

		// create bankLine object
		Map<Integer, SobekBankLine> sobekBankLineMap = new HashMap<>();
		for (int index = 0; index < bankGeoList.size(); index++) {
			Geometry bankLineGeo = bankGeoList.get(index);
			int bankLineID = (int) bankAttrList.get(index).get("ID");
			int linkedID = (int) bankAttrList.get(index).get("LinkedID");
			int linkedDirection = (int) bankAttrList.get(index).get("Direction");

			sobekBankLineMap.put(bankLineID, new SobekBankLine(bankLineGeo));
			sobekBankLineMap.get(bankLineID).setID(bankLineID);
			sobekBankLineMap.get(bankLineID).setLinkedPointID(linkedID);
			sobekBankLineMap.get(bankLineID).setLinkedDirection(linkedDirection);
		}

		// get crossSection points
		// crossSection
		String crossSectionPoint = sobekObjectWorkSpace + "Sbk_LConn_n.shp";
		SpatialReader crossSectionShp = new SpatialReader(crossSectionPoint);
		List<Geometry> crossSectionGeoList = crossSectionShp.getGeometryList();

		// crossSection start point
		String crossSectionStartPoint = sobekObjectWorkSpace + "Sbk_C&LR_n.shp";
		new SpatialReader(crossSectionStartPoint).getGeometryList().forEach(geo -> crossSectionGeoList.add(geo));

		Map<String, Geometry> crossSectionGeoMap = new HashMap<>();
		for (int index = 0; index < crossSectionGeoList.size(); index++) {
			String xString = AtCommonMath.getDecimal_String(crossSectionGeoList.get(index).GetX(),
					IrregularReachBasicControl.dataDecimale);
			String yString = AtCommonMath.getDecimal_String(crossSectionGeoList.get(index).GetY(),
					IrregularReachBasicControl.dataDecimale);
			String key = xString + "_" + yString;
			crossSectionGeoMap.put(key, crossSectionGeoList.get(index));
		}

		// division point to bankPoint, and reach point
		Set<String> bankPointRecord = new HashSet<>();
		sobekBankLineMap.keySet().forEach(bankLineKey -> {
			SobekBankLine temptBankLine = sobekBankLineMap.get(bankLineKey);
			List<String> temptNodeList = temptBankLine.getPointKeys();

			for (String temptNodeKey : temptNodeList) {
				if (crossSectionGeoMap.containsKey(temptNodeKey)) {
					temptBankLine.addBankPoint(crossSectionGeoMap.get(temptNodeKey));
					bankPointRecord.add(temptNodeKey);
				}
			}
		});

		// pairs bankPoints in bankLine
		SpatialWriter bankPointsPairs = new SpatialWriter();
		bankPointsPairs.addFieldType("ID", "String");
		bankPointsPairs.addFieldType("LinkedID", "String");
		bankPointsPairs.addFieldType("BankLineID", "Integer");

		List<Geometry> errorBnakPoints = new ArrayList<>();
		Set<Integer> bankLineRecord = new HashSet<>();
		for (Integer currentBankLineID : sobekBankLineMap.keySet()) {

			if (!bankLineRecord.contains(currentBankLineID)) {
				SobekBankLine currentBankLine = sobekBankLineMap.get(currentBankLineID);
				List<SobekBankPoint> currentBankPoints = currentBankLine.getBankPoints();

				SobekBankLine linkedBankLine = sobekBankLineMap.get(currentBankLine.getLinkedPointID());
				List<SobekBankPoint> linkedBankPoints = linkedBankLine.getBankPoints();

				// 0 for HeadToHead , 1 for HeadToEnd
				if (currentBankLine.getLinkedDirection() == 1) {
					Collections.reverse(currentBankPoints);
				}

				// check for points number is correct or not
				if (currentBankPoints.size() != linkedBankPoints.size()) {
					currentBankPoints.forEach(bnakPoint -> errorBnakPoints.add(bnakPoint.getGeo()));
					linkedBankPoints.forEach(bnakPoint -> errorBnakPoints.add(bnakPoint.getGeo()));
				}

				// if no error start pairs
				else {
					for (int index = 0; index < currentBankPoints.size(); index++) {
						SobekBankPoint temptCurrentBankPoint = currentBankPoints.get(index);
						SobekBankPoint temptLinkedBankPoints = linkedBankPoints.get(index);

						// current feature
						Map<String, Object> currentAttr = new HashMap<>();
						currentAttr.put("ID", temptCurrentBankPoint.getID());
						currentAttr.put("LinkedID", temptLinkedBankPoints.getID());
						currentAttr.put("BankLineID", temptCurrentBankPoint.getBelongBankLineID());
						bankPointsPairs.addFeature(temptCurrentBankPoint.getGeo(), currentAttr);

						// linked feature
						Map<String, Object> linkedtAttr = new HashMap<>();
						linkedtAttr.put("ID", temptLinkedBankPoints.getID());
						linkedtAttr.put("LinkedID", temptCurrentBankPoint.getID());
						linkedtAttr.put("BankLineID", temptLinkedBankPoints.getBelongBankLineID());
						bankPointsPairs.addFeature(temptLinkedBankPoints.getGeo(), linkedtAttr);
					}

					// remove bankPoint id from
					bankLineRecord.add(currentBankLine.getID());
					bankLineRecord.add(linkedBankLine.getID());
				}
			}
		}

		// create reachPoint and bnakPoint files
		if (errorBnakPoints.size() != 0) {
			// throw error message
			System.out
					.println("bankPoint pairs error, bankPoint amount not match, create file " + pariseBankPointsError);
			new SpatialWriter().setGeoList(errorBnakPoints).saveAsShp(pariseBankPointsError);
			System.out.println("please checkout files, Sbk_LConn_n.shp and Sbk_C&LR_n.shp");

		} else {
			System.out.println("bankPoint pairs complete, create file " + pariseBankPoints);

			// create bankPoints
			bankPointsPairs.saveAsShp(pariseBankPoints);

			// create reachPoint
			List<Geometry> reachPoints = new ArrayList<>();
			crossSectionGeoMap.keySet().forEach(crossSectionKey -> {
				if (!bankPointRecord.contains(crossSectionKey))
					reachPoints.add(crossSectionGeoMap.get(crossSectionKey));
			});
			new SpatialWriter().setGeoList(reachPoints).saveAsShp(reachNodesShp);
		}
	}

}
