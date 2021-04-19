package Execution;

import java.util.List;
import java.util.Map;

import org.gdal.ogr.Geometry;

import geo.gdal.SpatialReader;
import geo.gdal.SpatialWriter;
import geo.gdal.application.IrregularReachBasicControl;

public class HyDEM_FixCenterLine {

	public static void main(String[] args) throws Exception {

		// get variables

		// @ input
		// -HyDEM_CenterLine (String , spatailFile path)

		// @Output
		// -HyDEM_CenterLine_Fix (String , spatailFile path)

		Map<String, String> parameter = WorkSpace.settingVariables(args);
		String centerLineHydemPolygons = parameter.get("-HyDEM_CenterLine");
		String centerLineFixed = parameter.get("-HyDEM_CenterLine_Fix");

		/*
		 * the main purpose of this function is to find out the longest length of
		 * reach-centerLine, which remove the small error in last-step
		 * "HyDEM_CreateCenterLine"
		 */

		// get the centerLine shpFile
		SpatialReader shpReader = new SpatialReader(centerLineHydemPolygons);
		List<Map<String, Object>> centerLineAttrList = shpReader.getAttributeTable();
		List<Geometry> centerLineGeoList = shpReader.getGeometryList();

		// output shpFile properties
		SpatialWriter shpWriter = new SpatialWriter();
		shpWriter.setFieldType(shpReader.getAttributeTitleType());

		// get the maxLength of each centerLine
		for (int index = 0; index < centerLineGeoList.size(); index++) {
			IrregularReachBasicControl temptCenterLine = new IrregularReachBasicControl(centerLineGeoList.get(index));
			shpWriter.addFeature(temptCenterLine.getMaxLengthReach(), centerLineAttrList.get(index));
		}

		// output shpFile
		shpWriter.saveAsShp(centerLineFixed);
		System.out.println("create centerLine-Fix complete, " + centerLineFixed);
	}

}
