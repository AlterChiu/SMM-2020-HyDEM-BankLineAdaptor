package Execution;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.gdal.ogr.Geometry;

import geo.common.Task.HyDEM.BankLine.WorkSpace;
import geo.gdal.GdalGlobal;
import geo.gdal.SpatialReader;
import geo.gdal.SpatialWriter;

public class HyDEM_SplitPolygonBySplitLine {
	public static int dataDecimal = 4;

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String testingWorkSpace = WorkSpace.testingWorkSpace;
		String hydemObjectWorkSpace = WorkSpace.hydemObjectWorkSpace;
		String mergedHydemPolygons = WorkSpace.mergedHydemPolygons;
		String userDefinSplitLine = WorkSpace.userDefineSplitLine;
		String splitHydemLines = WorkSpace.splitHydemLines;
		String splitHydemPolygons = WorkSpace.splitHydemPolygons;

		// <================================================>
		// <======== Split HyDEM polygon by SplitLine==================>
		// <================================================>
		/*
		 * @ split HyDEM polygon by split line
		 * 
		 * @ input : splitLinePairseBankPoints, hydemObjectWorkSpace(folder address)
		 * 
		 * @ output : splitHydemPolygons
		 */

		// merge all shp in hydemObjectWorkSpace folder
		List<Geometry> geoList = new ArrayList<>();
		for (String fileName : new File(hydemObjectWorkSpace).list()) {
			if (fileName.contains(".shp")) {
				new SpatialReader(hydemObjectWorkSpace + "\\" + fileName).getGeometryList()
						.forEach(geo -> geoList.add(geo));
			}
		}

		Geometry mergedBankLine = GdalGlobal.mergePolygons(geoList);
		new SpatialWriter().setGeoList(GdalGlobal.MultiPolyToSingle(mergedBankLine))
				.saveAsShp(testingWorkSpace + mergedHydemPolygons);
		geoList.clear();

		// get boundary of mergedBankLine
		Geometry mergedBankLineBoundary = mergedBankLine.GetBoundary();

		// get split line from splitLinePairseBankPoints
		// ignore which intersection nodes under than 1
		List<Geometry> splitLineHyDEM = new ArrayList<>();
		List<Geometry> splitLines = new SpatialReader(testingWorkSpace + userDefinSplitLine).getGeometryList();
		int completedPersantage = 0;
		for (int splitLineIndex = 0; splitLineIndex < splitLines.size(); splitLineIndex++) {
			double currentPersantage = (int) ((splitLineIndex + 0.) * 100 / splitLines.size());
			if (currentPersantage > completedPersantage) {
				System.out.print((int) currentPersantage + "....");
				completedPersantage = (int) currentPersantage;
			}

			// skip null splitLine
			try {
				Geometry splitLine = splitLines.get(splitLineIndex);
				Geometry intersection = splitLine.Intersection(mergedBankLineBoundary);

				if (intersection.GetGeometryCount() == 2) {
					Geometry point1 = intersection.GetGeometryRef(0);
					Geometry point2 = intersection.GetGeometryRef(1);
					splitLineHyDEM
							.add(GdalGlobal.CreateLine(point1.GetX(), point1.GetY(), point2.GetX(), point2.GetY()));
				}
			} catch (Exception e) {
			}
		}
		System.out.println("");

		// output splitLine in bankLine polygon
		new SpatialWriter().setGeoList(splitLineHyDEM).saveAsShp(testingWorkSpace + splitHydemLines);
		splitLines.clear();

		// buffer splitLine
		List<Geometry> dissoveSplitLine = new ArrayList<>();
		splitLineHyDEM.forEach(splitLine -> dissoveSplitLine.add(splitLine.Buffer(Math.pow(0.1, dataDecimal + 4))));

		// split mergedBankLine by dissoveSplitLine
		new SpatialWriter()
				.setGeoList(GdalGlobal
						.MultiPolyToSingle(mergedBankLine.Difference(GdalGlobal.mergePolygons(dissoveSplitLine))))
				.saveAsShp(testingWorkSpace + splitHydemPolygons);
		System.out.println("create split polygon complete, " + splitHydemPolygons);
	}

}