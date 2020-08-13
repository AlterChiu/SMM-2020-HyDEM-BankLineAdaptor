package Execution;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.gdal.ogr.Geometry;

import geo.gdal.GdalGlobal;
import geo.gdal.SpatialReader;
import geo.gdal.SpatialWriter;
import usualTool.AtCommonMath;
import usualTool.AtFileWriter;

public class HyDEM_CheckLevelContinue {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		// get variables
		// @ input
		// -HyDEM_BankLine (String , spatailFile path)

		// @Output
		// -HyDEM_BankLineLeveling (String , csvFile path)
		// -HyDEM_BankLineVertice (String , spatailFile path)
		Map<String, String> parameter = WorkSpace.settingVariables(args);

		String bankLineHydem = parameter.get("-HyDEM_BankLine");
		String bankLineHydem_Leveling = parameter.get("-HyDEM_BankLineLeveling");
		String bankLineHydem_Vertice = parameter.get("-HyDEM_BankLineVertice");

		// <================================================>
		// <====== Check Leveling Continue of HyDEM BankLine ============>
		// <================================================>

		String bankLineShpAdd = bankLineHydem;

		String bankLineVerticeAdd = bankLineHydem_Vertice;
		SpatialWriter bankLineVerticeShp = new SpatialWriter();
		bankLineVerticeShp.addFieldType("ID", "String");
		bankLineVerticeShp.addFieldType("X", "double");
		bankLineVerticeShp.addFieldType("Y", "double");
		bankLineVerticeShp.addFieldType("Z", "double");

		SpatialReader banLineShp = new SpatialReader(bankLineShpAdd);
		List<Geometry> bankLineGeoList = banLineShp.getGeometryList();
		List<Map<String, Object>> bankLineAttrList = banLineShp.getAttributeTable();

		Map<String, BankLineLevelingClass> bankLineLeveling = new TreeMap<>();

		for (int index = 0; index < bankLineGeoList.size(); index++) {
			int id = (int) bankLineAttrList.get(index).get("ID");
			Geometry temptGeo = bankLineGeoList.get(index);
			double length = temptGeo.Length();

			List<Double[]> outList = new ArrayList<>();// 0: length persantage, which 0-100 ; 1: z-value

			// get points
			List<Double[]> temptValueList = new ArrayList<>();
			for (double[] point : temptGeo.GetPoints()) {
				temptValueList.add(new Double[] { point[0], point[1], point[2] });
			}

			// check level, from low to high
			if (temptValueList.get(0)[2] > temptValueList.get(temptValueList.size() - 1)[2]) {
				Collections.reverse(temptValueList);
			}

			// output temptValueList to a points shapeFile
			for (int pointIndex = 0; pointIndex < temptValueList.size(); pointIndex++) {
				Map<String, Object> temptFeatureAttr = new HashMap<>();
				temptFeatureAttr.put("X", temptValueList.get(pointIndex)[0]);
				temptFeatureAttr.put("Y", temptValueList.get(pointIndex)[1]);
				temptFeatureAttr.put("Z", temptValueList.get(pointIndex)[2]);
				temptFeatureAttr.put("ID", id + "_" + (pointIndex + 1));
				bankLineVerticeShp.addFeature(GdalGlobal.CreatePoint(temptValueList.get(pointIndex)[0],
						temptValueList.get(pointIndex)[1], temptValueList.get(pointIndex)[2]), temptFeatureAttr);
			}

			// translate point to , length-Z plot, which length from 0% - 100%
			double currentLength = 0;
			double maxSlope = Double.NEGATIVE_INFINITY;
			Double[] currentPoint = temptValueList.get(0);
			outList.add(new Double[] { 0., currentPoint[2] });

			for (int pointIndex = 1; pointIndex < temptValueList.size(); pointIndex++) {
				Double[] nextPoint = temptValueList.get(pointIndex);
				double nextLength = AtCommonMath.getLength(currentPoint[0], currentPoint[1], nextPoint[0],
						nextPoint[1]);

				// check slope
				double slope = Math.abs((currentPoint[2] - nextPoint[2]) / nextLength) * 100;
				if (slope > maxSlope) {
					maxSlope = slope;
				}

				// summary length
				currentLength = currentLength + nextLength;

				// add to valueList
				outList.add(new Double[] { currentLength * 100 / length, temptValueList.get(pointIndex)[2] });
				currentPoint = temptValueList.get(pointIndex);
			}

			BankLineLevelingClass temptStoreClass = new BankLineLevelingClass();
			temptStoreClass.setID(id + "");
			temptStoreClass.setTotalLength(length);
			temptStoreClass.setValueList(outList);
			temptStoreClass.setSlope(maxSlope);
			temptStoreClass.setLinkedID((int) bankLineAttrList.get(index).get("LinkedID") + "");
			bankLineLeveling.put(id + "", temptStoreClass);
		}

		// output leveling
		List<String[]> outContent = new ArrayList<>();
		List<String> key = new ArrayList<>(bankLineLeveling.keySet());

		// outputTitle, ID
		List<String> titleID = new ArrayList<>();
		titleID.add("ID");

		// outputTitle, LinkedID
		List<String> titleLinkedID = new ArrayList<>();
		titleLinkedID.add("LinkedID");

		// outputTitle, maxSlope
		List<String> titleMaxSlope = new ArrayList<>();
		titleMaxSlope.add("MaxSlope(%)");

		// outputTitle, totalLength
		List<String> titleTotalLength = new ArrayList<>();
		titleTotalLength.add("TotalLength(m)");

		// get max size of xyValues
		int maxValuesIndex = 0;

		// output Title, x,y
		List<String> xyPlot = new ArrayList<>();
		xyPlot.add("");

		for (int index = 0; index < key.size(); index++) {
			xyPlot.add("Length(%)");
			xyPlot.add("Z-Leveling(m)");

			titleTotalLength.add(AtCommonMath.getDecimal_String(bankLineLeveling.get(key.get(index)).getLength(), 0));
			titleTotalLength.add("");

			titleMaxSlope.add(AtCommonMath.getDecimal_String(bankLineLeveling.get(key.get(index)).getSlope(), 0));
			titleMaxSlope.add("");

			titleLinkedID.add(bankLineLeveling.get(key.get(index)).getLinkedID());
			titleLinkedID.add("");

			titleID.add(bankLineLeveling.get(key.get(index)).getID());
			titleID.add("");

			if (bankLineLeveling.get(key.get(index)).getValueList().size() > maxValuesIndex) {
				maxValuesIndex = bankLineLeveling.get(key.get(index)).getValueList().size();
			}
		}

		outContent.add(titleID.parallelStream().toArray(String[]::new));
		outContent.add(titleLinkedID.parallelStream().toArray(String[]::new));
		outContent.add(titleTotalLength.parallelStream().toArray(String[]::new));
		outContent.add(titleMaxSlope.parallelStream().toArray(String[]::new));
		outContent.add(xyPlot.parallelStream().toArray(String[]::new));

		// output values
		for (int valueIndex = 0; valueIndex < maxValuesIndex; valueIndex++) {
			List<String> outputValues = new ArrayList<>();
			outputValues.add("");

			for (int index = 0; index < key.size(); index++) {
				BankLineLevelingClass temptBankLineLeveling = bankLineLeveling.get(key.get(index));
				try {
					outputValues.add(temptBankLineLeveling.getValueList().get(valueIndex)[0] + "");
					outputValues.add(temptBankLineLeveling.getValueList().get(valueIndex)[1] + "");
				} catch (Exception e) {
					outputValues.add("");
					outputValues.add("");
				}
			}
			outContent.add(outputValues.parallelStream().toArray(String[]::new));
		}

		new AtFileWriter(outContent.parallelStream().toArray(String[][]::new), bankLineHydem_Leveling).tabWriter();

		bankLineVerticeShp.saveAsShp(bankLineVerticeAdd);
	}

	static class BankLineLevelingClass {
		private List<Double[]> lengthZList = new ArrayList<>();
		private String id = "";
		private double totalLength = 0;
		private double maxSlope = Double.NEGATIVE_INFINITY;
		private String linkedID = "";

		public void setLinkedID(String linkedID) {
			this.linkedID = linkedID;
		}

		public String getLinkedID() {
			return this.linkedID;
		}

		public void setSlope(double slope) {
			this.maxSlope = slope;
		}

		public double getSlope() {
			return this.maxSlope;
		}

		public void setValueList(List<Double[]> valueList) {
			this.lengthZList = valueList;
		}

		public void setID(String id) {
			this.id = id;
		}

		public void setTotalLength(double length) {
			this.totalLength = length;
		}

		public List<Double[]> getValueList() {
			return this.lengthZList;
		}

		public String getID() {
			return this.id;
		}

		public double getLength() {
			return this.totalLength;
		}

	}

}
