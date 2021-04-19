package Execution;

import java.io.UnsupportedEncodingException;
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

	public static void main(String[] args) throws UnsupportedEncodingException {
		// TODO Auto-generated method stub

		// @ input
		// -SOBEK_BankPoints (String, spatailFile path)
		// -Buffer (Integer , 0 - 100)

		// @Output
		// -SOBEK_SplitLine (String , spatialFile path)

		Map<String, String> parameter = WorkSpace.settingVariables(args);

		// setting Variables
		String pariseBankPointsAdd = parameter.get("-SOBEK_BankPoints");
		String splitLinePairseBankPointsAdd = parameter.get("-SOBEK_SplitLine");
		int buffer = Integer.parseInt(parameter.get("-Buffer"));

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
		SpatialReader pairsBankPointsShp = new SpatialReader(pariseBankPointsAdd);
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

				double outX1 = currentX + (0.01 * buffer) * (currentX - linkedX) / 2;
				double outY1 = currentY + (0.01 * buffer) * (currentY - linkedY) / 2;
				double outX2 = linkedX + (0.01 * buffer) * (linkedX - currentX) / 2;
				double outY2 = linkedY + (0.01 * buffer) * (linkedY - currentY) / 2;

				outList.add(GdalGlobal.CreateLineString(outX1, outY1, outX2, outY2));
			}

			usedID.add(currentID);
			usedID.add(linkedID);
		}

		new SpatialWriter().setGeoList(outList).saveAsShp(splitLinePairseBankPointsAdd);
		System.out.println("create split line complete, " + splitLinePairseBankPointsAdd);

	}

}
