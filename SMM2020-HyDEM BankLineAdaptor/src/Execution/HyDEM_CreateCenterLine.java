package Execution;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gdal.ogr.Geometry;

import geo.gdal.GdalGlobal;
import geo.gdal.SpatialReader;
import geo.gdal.SpatialWriter;
import geo.gdal.vector.GDAL_VECTOR_CenterLine;
import geo.gdal.vector.GDAL_VECTOR_Defensify;
import usualTool.FileFunction;

public class HyDEM_CreateCenterLine {
	public static int boundaryBufferPersentage = 10; // 10%
	public static double centerLineVerticeInterval = 5.0; // 5m

	public static void main(String[] args) throws Exception {

		// get variables

		// @ setting
		// -QgisRoot (String, folder path)

		// @ input
		// -HyDEM_BankLine (String , spatailFile path)
		// -Buffer (Integer, 0-100)
		// -VerticeInterval(double , >0)

		// @Output
		// -HyDEM_CenterLine (String , spatailFile path)

		Map<String, String> parameter = WorkSpace.settingVariables(args);

		String bankLineHydem = parameter.get("-HyDEM_BankLine");
		String centerLineHydemPolygons = parameter.get("-HyDEM_CenterLine");
		String qgisRoot = parameter.get("-QgisRoot");
		HyDEM_CreateCenterLine.boundaryBufferPersentage = Integer.parseInt(parameter.get("-Buffer"));
		HyDEM_CreateCenterLine.centerLineVerticeInterval = Double.parseDouble(parameter.get("-VerticeInterval"));

		// setting qgisRoot
		GdalGlobal.qgisBinFolder = qgisRoot;
		FileFunction.newFolder(GdalGlobal.temptFolder);

		// read file
		String hydemBankLineFileAdd = bankLineHydem;
		SpatialReader hydemBankLine = new SpatialReader(hydemBankLineFileAdd);
		Map<String, List<Geometry>> bankLineGeoList = hydemBankLine.getGeoListMap("ID");
		List<Map<String, Object>> bankLineAttr = hydemBankLine.getAttributeTable();

		// output
		SpatialWriter centerLineShp = new SpatialWriter();
		centerLineShp.addFieldType("ID", "int");
		centerLineShp.addFieldType("BankID1", "int");
		centerLineShp.addFieldType("BankID2", "int");

		// create centerLine in each pairs of bankLine
		Set<Integer> usedID = new HashSet<>();
		List<CenterLine> centerLineList = new ArrayList<>();
		for (int index = 0; index < bankLineAttr.size(); index++) {

			int bankLineID = (int) bankLineAttr.get(index).get("ID");
			if (!usedID.contains(bankLineID)) {

				// get properties
				int linkedID = (int) bankLineAttr.get(index).get("LinkedID");
				int direction = (int) bankLineAttr.get(index).get("Direction");

				Geometry bankLine1 = bankLineGeoList.get(bankLineID + "").get(0);
				Geometry bankLine2 = bankLineGeoList.get(linkedID + "").get(0);

				// add id to used ketSet
				usedID.add(linkedID);
				usedID.add(bankLineID);

				// add to list
				if (bankLine1.Length() > centerLineVerticeInterval * 4
						&& bankLine2.Length() > centerLineVerticeInterval * 4) {
					CenterLine temptCenterLine = new CenterLine(bankLine1, bankLine2, direction);
					temptCenterLine.setBankLineID(linkedID, bankLineID);
					centerLineList.add(temptCenterLine);
				}
			}
		}

		// output processing rate
		int completedPersantage = 0;
		System.out.print((int) completedPersantage + "....");

		for (int index = 0; index < centerLineList.size(); index++) {

			// output processing rate
			double currentPersantage = (int) ((index + 0.) * 100 / centerLineList.size());
			if (currentPersantage > completedPersantage) {
				System.out.print((int) currentPersantage + "....");
				completedPersantage = (int) currentPersantage;
			}

			// start centerLine processing
			CenterLine temptCenterLine = centerLineList.get(index);

			// create centerLine by bufferPolygon
			GDAL_VECTOR_CenterLine centerLineAlgorithm = new GDAL_VECTOR_CenterLine(
					temptCenterLine.getBufferBankLinePolygon());
			centerLineAlgorithm.setVerticeDensitive(centerLineVerticeInterval);
			List<Geometry> centerLineGeoList = centerLineAlgorithm.getGeoList();

			// clip centerLine by boundary bankLine
			Geometry mergedCenterLine = GdalGlobal.mergePolygons(centerLineGeoList);

			// output properties
			Map<String, Object> temptFeature = new HashMap<>();
			temptFeature.put("ID", centerLineShp.getSize());
			temptFeature.put("BankID1", temptCenterLine.getBankLineID().get(0));
			temptFeature.put("BankID2", temptCenterLine.getBankLineID().get(1));

			centerLineShp.addFeature(temptCenterLine.getBankLineBoundaryPolygon().Intersection(mergedCenterLine),
					temptFeature);
		}

		// output shpFile
		centerLineShp.saveAsShp(centerLineHydemPolygons);
		System.out.println("create centerLine complete, " + centerLineHydemPolygons);
	}

	public static class CenterLine {
		private List<Integer> bankLineID = new ArrayList<>();
		private Geometry bankLineBoundaryPolygon;
		private Geometry bankLineBufferPolygon;

		/*
		 * <+++++++++++++++++CONSTRUCTOR++++++++++++++++++++++>
		 */
		public CenterLine(Geometry bankLine1, Geometry bankLine2, int linkedDirection)
				throws IOException, InterruptedException {

			if (bankLine1.GetPointCount() < 4) {
				GDAL_VECTOR_Defensify defensify = new GDAL_VECTOR_Defensify(bankLine1);
				defensify.setInterval(centerLineVerticeInterval);
				bankLine1 = defensify.getGeoList().get(0);
			}

			if (bankLine2.GetPointCount() < 4) {
				GDAL_VECTOR_Defensify defensify = new GDAL_VECTOR_Defensify(bankLine2);
				defensify.setInterval(centerLineVerticeInterval);
				bankLine2 = defensify.getGeoList().get(0);
			}

			// create boundaryPolygon
			this.bankLineBoundaryPolygon = getPolygon(bankLine1, bankLine2, linkedDirection);

			// create buffer boundaryPolygon
			Geometry extensionGeometry = getBufferPolygon(bankLine1, bankLine2, linkedDirection);
			try {
				this.bankLineBufferPolygon = this.bankLineBoundaryPolygon.Union(extensionGeometry);
			} catch (Exception e) {
				this.bankLineBufferPolygon = this.bankLineBoundaryPolygon;
			}

		}

		private Geometry getPolygon(Geometry bankLine1, Geometry bankLine2, int linkedDirection) {
			// create boundary polygon
			List<Double[]> temptPoints = new ArrayList<>();

			getBankLinePoints(bankLine1).forEach(point -> temptPoints.add(point));

			// check direction
			if (linkedDirection == 0) { // head to head
				Collections.reverse(temptPoints);
			}

			getBankLinePoints(bankLine2).forEach(point -> temptPoints.add(point));

			// head to head, return to end
			if (linkedDirection == 0) {
				temptPoints.add(getStartEndPoint(bankLine1, true));

				// head to end, return to head
			} else if (linkedDirection == 1) {
				temptPoints.add(getStartEndPoint(bankLine1, false));
			}

			return GdalGlobal.CreatePolygon(temptPoints);
		}

		private Geometry getBufferPolygon(Geometry bankLine1, Geometry bankLine2, int direction) {

			List<Double[]> points1 = getBankLinePoints(bankLine1);
			List<Double[]> points2 = getBankLinePoints(bankLine2);

			// vector1
			Double[] headVector1 = getVector(points1, false);
			Double[] endVector1 = getVector(points1, true);
			Double[] headPoint1 = points1.get(0);
			Double[] endPoint1 = points1.get(points1.size() - 1);

			double headVectorLength1 = Math.sqrt(Math.pow(headVector1[0], 2) + Math.pow(headVector1[1], 2));
			double endVectorLength1 = Math.sqrt(Math.pow(endVector1[0], 2) + Math.pow(endVector1[1], 2));

			// vector2
			Double[] headVector2;
			Double[] endVector2;
			Double[] headPoint2;
			Double[] endPoint2;

			// head to end
			if (direction == 1) {
				headVector2 = getVector(points2, true);
				endVector2 = getVector(points2, false);
				headPoint2 = getStartEndPoint(bankLine2, true);
				endPoint2 = getStartEndPoint(bankLine2, false);

				// head to head
			} else {
				headVector2 = getVector(points2, false);
				endVector2 = getVector(points2, true);
				headPoint2 = getStartEndPoint(bankLine2, false);
				endPoint2 = getStartEndPoint(bankLine2, true);
			}

			// vector2
			double headVectorLength2 = Math.sqrt(Math.pow(headVector2[0], 2) + Math.pow(headVector2[1], 2));
			double endVectorLength2 = Math.sqrt(Math.pow(endVector2[0], 2) + Math.pow(endVector2[1], 2));

			double headVectorRate2 = headVectorLength1 / headVectorLength2;
			double endVectorRate2 = endVectorLength1 / endVectorLength2;

			headVector2 = new Double[] { headVector2[0] * headVectorRate2, headVector2[1] * headVectorRate2 };
			endVector2 = new Double[] { endVector2[0] * endVectorRate2, endVector2[1] * endVectorRate2 };

			// buffer length
			double bufferLength = (bankLine1.Length() + bankLine2.Length()) / 2
					* HyDEM_CreateCenterLine.boundaryBufferPersentage / 100.;
			if (bufferLength < 10 * HyDEM_CreateCenterLine.centerLineVerticeInterval) {
				bufferLength = 10 * HyDEM_CreateCenterLine.centerLineVerticeInterval;
			}

			// get head buffer vector
			Double[] headBufferVector = new Double[] { headVector2[0] + headVector1[0],
					headVector2[1] + headVector1[1] };
			double headBufferVectorLength = Math
					.sqrt(Math.pow(headBufferVector[0], 2) + Math.pow(headBufferVector[1], 2));
			double headBufferVectorRate = bufferLength / headBufferVectorLength;
			headBufferVector = new Double[] { headBufferVector[0] * headBufferVectorRate,
					headBufferVector[1] * headBufferVectorRate };

			// get head buffer geometry
			List<Double[]> headBufferPoints = new ArrayList<>();
			headBufferPoints.add(headPoint1);
			headBufferPoints
					.add(new Double[] { headPoint1[0] + headBufferVector[0], headPoint1[1] + headBufferVector[1] });
			headBufferPoints
					.add(new Double[] { headPoint2[0] + headBufferVector[0], headPoint2[1] + headBufferVector[1] });
			headBufferPoints.add(headPoint2);
			headBufferPoints.add(headPoint1);
			Geometry headBufferGeometry = GdalGlobal.CreatePolygon(headBufferPoints);

			// get end buffer center
			Double[] endBufferVector = new Double[] { endVector2[0] + endVector1[0], endVector2[1] + endVector1[1] };
			double endBufferVectorLength = Math.sqrt(Math.pow(endBufferVector[0], 2) + Math.pow(endBufferVector[1], 2));
			double endBufferVectorRate = bufferLength / endBufferVectorLength;
			endBufferVector = new Double[] { endBufferVector[0] * endBufferVectorRate,
					endBufferVector[1] * endBufferVectorRate };

			// get end buffer geometry
			List<Double[]> endBufferPoints = new ArrayList<>();
			endBufferPoints.add(endPoint1);
			endBufferPoints.add(new Double[] { endPoint1[0] + endBufferVector[0], endPoint1[1] + endBufferVector[1] });
			endBufferPoints.add(new Double[] { endPoint2[0] + endBufferVector[0], endPoint2[1] + endBufferVector[1] });
			endBufferPoints.add(endPoint2);
			endBufferPoints.add(endPoint1);
			Geometry endBufferGeometry = GdalGlobal.CreatePolygon(endBufferPoints);

			return headBufferGeometry.Union(endBufferGeometry);
		}

		private Double[] getVector(List<Double[]> points, Boolean isEnd) {
			Double[] startPoint;
			Double[] directionPoint;

			if (isEnd) {
				startPoint = points.get(points.size() - 2);
				directionPoint = points.get(points.size() - 1);
			} else {
				startPoint = points.get(1);
				directionPoint = points.get(0);
			}

			double vectorX = directionPoint[0] - startPoint[0];
			double vectorY = directionPoint[1] - startPoint[1];
			double vectorZ = directionPoint[2] - startPoint[2];

			return new Double[] { vectorX, vectorY, vectorZ };
		}

		private List<Double[]> getBankLinePoints(Geometry geometry) {
			List<Double[]> outList = new ArrayList<>();
			for (int pointIndex = 0; pointIndex < geometry.GetPointCount(); pointIndex++) {
				double zValue;
				try {
					zValue = geometry.GetZ(pointIndex);
				} catch (Exception e) {
					zValue = 0.0;
				}
				outList.add(new Double[] { geometry.GetX(pointIndex), geometry.GetY(pointIndex), zValue });
			}
			return outList;
		}

		private Double[] getStartEndPoint(Geometry geometry, boolean isEnd) {

			int pointIndex;
			if (isEnd) {
				pointIndex = geometry.GetPointCount() - 1;
			} else {
				pointIndex = 0;
			}

			double zValue;
			try {
				zValue = geometry.GetZ(pointIndex);
			} catch (Exception e) {
				zValue = 0.0;
			}
			return new Double[] { geometry.GetX(pointIndex), geometry.GetY(pointIndex), zValue };
		}

		/*
		 * <+++++++++++++++++OUTPUT FUNCTION+++++++++++++++++++++>
		 */

		public void setBankLineID(int id1, int id2) {
			this.bankLineID.clear();
			this.bankLineID.add(id1);
			this.bankLineID.add(id2);
		}

		public Geometry getBankLineBoundaryPolygon() {
			return this.bankLineBoundaryPolygon;
		}

		public Geometry getBufferBankLinePolygon() {
			return this.bankLineBufferPolygon;
		}

		public List<Integer> getBankLineID() {
			return this.bankLineID;
		}
	}

}
