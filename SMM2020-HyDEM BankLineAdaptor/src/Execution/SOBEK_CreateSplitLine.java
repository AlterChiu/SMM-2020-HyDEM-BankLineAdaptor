package Execution;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gdal.ogr.Geometry;

import geo.gdal.GdalGlobal;
import geo.gdal.SpatialReader;
import geo.gdal.SpatialWriter;

public class SOBEK_CreateSplitLine {
	// workSpace
	public static String workSpace = "E:\\LittleProject\\報告書\\109 - SMM\\測試\\溢堤線更新\\";
	public static String sobekObjectWorkSpace = workSpace + "SOBEK物件\\shp-file\\";
	public static String hydemObjectWorkSpace = workSpace + "溢堤線\\第一期\\";
	public static String testingWorkSpace = workSpace + "testing\\";

	// creating fileName
	public static String pairseBankLine_Error = "SOBEK_BankLinepairesError.shp";
	public static String pairseBankLine = "SOBEK_BankLinepaires.shp";
	public static String pariseBankPointsError = "SOBEK_BankPointspairesError.shp";
	public static String pariseBankPoints = "SOBEK_BankPointspaires.shp";
	public static String reachNodesShp = "SOBEK_ReachNode.shp";
	public static String splitLinePairseBankPoints = "SOBEK_BankPointsLine.shp";

	public static String splitHydemPolygons = "HyDEM_SplitPolygons.shp";
	public static String splitHydemLines = "HyDEM_SplitLine.shp";
	public static String mergedHydemPolygons = "HyDEM_MergedBankLine.shp";
	public static String centerLineHydemPolygons = "HyDEM_CenterLine.shp";
	public static String bankLineHydem = "HyDEM_BankLine.shp";

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		// setting Variables
		String testingWorkSpace = SOBEK_CreateSplitLine.testingWorkSpace;
		String pariseBankPointsAdd = SOBEK_CreateSplitLine.pariseBankPoints;
		String splitLinePairseBankPointsAdd = SOBEK_CreateSplitLine.splitLinePairseBankPoints;

		// TODO Auto-generated method stub
		// <================================================>
		// <======== Create SlpitLine =============================>
		// <================================================>
		/*
		 * @ Create splitLine from pairs bankPoints, which double distance of
		 * pairseBankPoints
		 * 
		 * @input : pariseBankPoints
		 * 
		 * @output :
		 * 
		 * @ SplitLine => splitLinePairseBankPoints
		 * 
		 */

		// read pairs point properties
		SpatialReader pairsBankPointsShp = new SpatialReader(testingWorkSpace + pariseBankPointsAdd);
		List<Map<String, Object>> pointsAttr = pairsBankPointsShp.getAttributeTable();
		Map<String, List<Geometry>> geoMap = pairsBankPointsShp.getGeoListMap("ID");

		// create pairs line
		Set<String> usedID = new HashSet<>();
		List<Geometry> outList = new ArrayList<>();
		for (int index = 0; index < pointsAttr.size(); index++) {
			String currentID = (String) pointsAttr.get(index).get("ID");
			String linkedID = (String) pointsAttr.get(index).get("LinkedID");

			if (!usedID.contains(currentID)) {
				Geometry currentGeometry = geoMap.get(currentID).get(0);
				double currentX = currentGeometry.GetX();
				double currentY = currentGeometry.GetY();

				Geometry linkedGeometry = geoMap.get(linkedID).get(0);
				double linkedX = linkedGeometry.GetX();
				double linkedY = linkedGeometry.GetY();

				double outX1 = currentX + 0.5 * (currentX - linkedX) / 2;
				double outY1 = currentY + 0.5 * (currentY - linkedY) / 2;
				double outX2 = linkedX + 0.5 * (linkedX - currentX) / 2;
				double outY2 = linkedY + 0.5 * (linkedY - currentY) / 2;

				outList.add(GdalGlobal.CreateLine(outX1, outY1, outX2, outY2));
			}

			usedID.add(currentID);
			usedID.add(linkedID);
		}

		new SpatialWriter().setGeoList(outList).saveAsShp(testingWorkSpace + splitLinePairseBankPointsAdd);
		System.out.println("create split line complete, " + splitLinePairseBankPointsAdd);

	}

}
